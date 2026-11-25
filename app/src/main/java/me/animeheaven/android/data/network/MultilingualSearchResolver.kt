package me.animeheaven.android.data.network

import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import me.animeheaven.android.data.cache.LruCacheMap
import me.animeheaven.android.data.local.WebCacheDao
import me.animeheaven.android.data.local.WebCacheEntity
import me.animeheaven.android.data.runCatchingCancellable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Resolves multi-language queries (Japanese, Chinese, Korean, Russian, Spanish, French, Arabic,
 * Thai, Vietnamese, Romaji pronunciation variants, etc.) into canonical English & Romaji titles
 * searchable on AnimeHeaven. Uses public keyless endpoints without AnimeHeaven cookies/headers.
 */
class MultilingualSearchResolver(
    baseClient: OkHttpClient,
    private val webCacheDao: WebCacheDao,
    private val scope: CoroutineScope
) {
    private val client = baseClient.newBuilder()
        .cache(null)
        .callTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private data class CachedResolution(val candidates: List<String>, val fetchedAt: Long)

    private val memoryCache = LruCacheMap<String, CachedResolution>(256)
    private val inFlight = ConcurrentHashMap<String, Deferred<List<String>>>()
    private val cacheMutex = Mutex()
    @Volatile private var generation = 0L
    @Volatile private var clearing = false

    fun trimMemory() {
        memoryCache.trimToSize(64)
    }

    fun onCacheCleared() {
        generation++
        clearing = true
        memoryCache.clear()
        clearing = false
    }

    suspend fun resolveCandidates(analyzed: AnalyzedQuery): List<String> {
        val rawKey = analyzed.original.lowercase()
        if (rawKey.isBlank()) return emptyList()
        val cacheKey = "$CACHE_PREFIX$rawKey"

        if (!clearing) {
            memoryCache[cacheKey]?.takeIf { isFresh(it.fetchedAt) }?.let { return it.candidates }
            val dbRow = runCatchingCancellable { webCacheDao.get(cacheKey) }.getOrNull()
            if (dbRow != null && isFresh(dbRow.fetchedAt)) {
                decodeCandidates(dbRow.payload)?.let { decoded ->
                    memoryCache[cacheKey] = CachedResolution(decoded, dbRow.fetchedAt)
                    return decoded
                }
            }
        }

        val epoch = generation
        val task = inFlight.computeIfAbsent("$epoch:$cacheKey") {
            scope.async(start = CoroutineStart.LAZY) {
                fetchAndRank(analyzed)
            }
        }
        task.invokeOnCompletion { inFlight.remove("$epoch:$cacheKey", task) }

        val resolved = try {
            task.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }

        if (resolved.isNotEmpty() && !clearing && epoch == generation) {
            val now = System.currentTimeMillis()
            cacheMutex.withLock {
                if (!clearing && epoch == generation) {
                    memoryCache[cacheKey] = CachedResolution(resolved, now)
                    runCatchingCancellable {
                        webCacheDao.upsert(WebCacheEntity(cacheKey, encodeCandidates(resolved), now))
                    }
                }
            }
        }
        return resolved
    }

    private suspend fun fetchAndRank(analyzed: AnalyzedQuery): List<String> = coroutineScope {
        val raw = analyzed.original
        val collected = mutableListOf<String>()

        when (analyzed.script) {
            QueryScript.HAN_CJK -> {
                val aniListJob = async { queryAniListSingle(raw) }
                val bangumiJob = async { queryBangumi(raw) }
                val wikidataJob = async { queryWikidata(raw, analyzed.wikidataLangHint) }

                val directAniList = aniListJob.await()
                val bangumiNative = bangumiJob.await()
                val wikidataHits = wikidataJob.await()

                collected.addAll(directAniList)
                if (bangumiNative.isNotEmpty()) {
                    val bangumiAniList = queryAniListBatch(bangumiNative.take(2))
                    collected.addAll(bangumiAniList)
                    // If Bangumi gave a kana/latin title directly, include searchable ones
                    collected.addAll(bangumiNative.filter(MultilingualQueryNormalizer::isSearchableOnSite))
                }
                collected.addAll(wikidataHits)
            }

            QueryScript.KANA_ONLY -> {
                val romaji = analyzed.kanaRomajiCandidates.firstOrNull().orEmpty()
                val aniListKanaJob = async { queryAniListSingle(raw) }
                val aniListRomajiJob = async {
                    if (romaji.isNotBlank()) queryAniListSingle(romaji) else emptyList()
                }
                val kitsuJob = async {
                    if (romaji.isNotBlank()) queryKitsu(romaji) else emptyList()
                }
                val wikidataJob = async { queryWikidata(raw, "ja") }

                collected.addAll(aniListRomajiJob.await())
                collected.addAll(aniListKanaJob.await())
                collected.addAll(kitsuJob.await())
                collected.addAll(wikidataJob.await())
            }

            QueryScript.OTHER_NON_LATIN -> {
                val aniListJob = async { queryAniListSingle(raw) }
                val wikidataJob = async { queryWikidata(raw, analyzed.wikidataLangHint) }

                collected.addAll(aniListJob.await())
                collected.addAll(wikidataJob.await())
            }

            QueryScript.LATIN -> {
                val q = analyzed.normalizedLatin
                val kitsuJob = async { queryKitsu(q) }
                val aniListJob = async { queryAniListSingle(q) }
                val wikidataJob = async { queryWikidata(q, "en") }

                collected.addAll(kitsuJob.await())
                collected.addAll(aniListJob.await())
                collected.addAll(wikidataJob.await())
            }
        }

        var ranked = rankAndDedupe(collected, excludeQuery = analyzed.normalizedLatin)

        // Jikan fallback only if primary providers returned nothing
        if (ranked.isEmpty()) {
            val jikanQuery = analyzed.kanaRomajiCandidates.firstOrNull() ?: analyzed.normalizedLatin
            val jikanHits = queryJikan(jikanQuery)
            ranked = rankAndDedupe(jikanHits, excludeQuery = analyzed.normalizedLatin)
        }

        // Append local Romaji variants at the end if still needed
        if (analyzed.romajiVariants.isNotEmpty()) {
            ranked = rankAndDedupe(ranked + analyzed.romajiVariants, excludeQuery = analyzed.normalizedLatin)
        }

        ranked.take(MAX_CANDIDATES)
    }

    private suspend fun queryAniListSingle(query: String): List<String> {
        val body = buildJsonObject {
            put("query", ANILIST_SINGLE_QUERY)
            put("variables", buildJsonObject { put("s", query) })
        }.toString()
        val json = postJson("https://graphql.anilist.co", body) ?: return emptyList()
        return parseAniListSingle(json)
    }

    private suspend fun queryAniListBatch(queries: List<String>): List<String> {
        val distinct = queries.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(3)
        if (distinct.isEmpty()) return emptyList()
        if (distinct.size == 1) return queryAniListSingle(distinct[0])

        val args = distinct.indices.joinToString(",") { "\$s$it:String!" }
        val aliases = distinct.indices.joinToString("") {
            "a$it:Page(perPage:4){media(search:\$s$it,type:ANIME,sort:[SEARCH_MATCH,POPULARITY_DESC]){$ANILIST_FIELDS}}"
        }
        val body = buildJsonObject {
            put("query", "query($args){$aliases}")
            put("variables", buildJsonObject { distinct.forEachIndexed { idx, value -> put("s$idx", value) } })
        }.toString()
        val json = postJson("https://graphql.anilist.co", body) ?: return emptyList()
        return parseAniListBatch(json, distinct.size)
    }

    private suspend fun queryBangumi(query: String): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = "https://api.bgm.tv/search/subject/$encoded?type=2&responseGroup=small&max_results=6"
        val json = getJson(url) ?: return emptyList()
        return parseBangumiNativeNames(json)
    }

    private suspend fun queryWikidata(query: String, langHint: String): List<String> {
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbsearchentities")
            .addQueryParameter("search", query)
            .addQueryParameter("language", langHint)
            .addQueryParameter("uselang", "en")
            .addQueryParameter("type", "item")
            .addQueryParameter("limit", "6")
            .addQueryParameter("format", "json")
            .build()
        val json = getJson(url.toString()) ?: return emptyList()
        return parseWikidataLabels(json)
    }

    private suspend fun queryKitsu(query: String): List<String> {
        val url = "https://kitsu.app/api/edge/anime".toHttpUrl().newBuilder()
            .addQueryParameter("filter[text]", query)
            .addQueryParameter("page[limit]", "6")
            .build()
        val json = getJson(url.toString()) ?: return emptyList()
        return parseKitsuTitles(json)
    }

    private suspend fun queryJikan(query: String): List<String> {
        val url = "https://api.jikan.moe/v4/anime".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("limit", "6")
            .build()
        val json = getJson(url.toString()) ?: return emptyList()
        return parseJikanTitles(json)
    }

    private suspend fun getJson(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", PUBLIC_METADATA_UA)
            .build()
        return runCatchingCancellable {
            client.readResponse(request) { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    private suspend fun postJson(url: String, jsonBody: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", PUBLIC_METADATA_UA)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return runCatchingCancellable {
            client.readResponse(request) { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    private fun isFresh(fetchedAt: Long): Boolean =
        System.currentTimeMillis() - fetchedAt < TTL_MS

    companion object {
        private const val CACHE_PREFIX = "mlsearch:"
        private const val MAX_CANDIDATES = 5
        private const val TTL_MS = 7 * 86_400_000L
        private const val PUBLIC_METADATA_UA = "AnimeHeavenAndroid/1.0 (MultilingualSearch)"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val ANILIST_FIELDS = "title{romaji english} format popularity"
        private const val ANILIST_SINGLE_QUERY =
            "query(\$s:String!){Page(perPage:6){media(search:\$s,type:ANIME,sort:[SEARCH_MATCH,POPULARITY_DESC]){$ANILIST_FIELDS}}}"

        private val WIKIDATA_ANIME_KEYWORDS = listOf(
            "anime", "manga", "animated", "animation", "television series",
            "tv series", "japanese film", "animated film", "light novel", "ova", "ona"
        )

        private fun parseObject(raw: String): JsonObject? =
            runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()

        private fun JsonElement?.str(): String? =
            (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
        private fun JsonObject.rows(key: String): List<JsonObject> =
            (get(key) as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

        internal fun parseAniListSingle(raw: String): List<String> {
            val media = parseObject(raw)?.obj("data")?.obj("Page")?.rows("media").orEmpty()
            return extractAniListMediaTitles(media)
        }

        internal fun parseAniListBatch(raw: String, count: Int): List<String> {
            val data = parseObject(raw)?.obj("data") ?: return emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until count) {
                val media = data.obj("a$i")?.rows("media").orEmpty()
                out.addAll(extractAniListMediaTitles(media))
            }
            return out
        }

        private fun extractAniListMediaTitles(media: List<JsonObject>): List<String> {
            val out = mutableListOf<String>()
            for (item in media) {
                val title = item.obj("title") ?: continue
                title["romaji"].str()?.let(out::add)
                title["english"].str()?.let(out::add)
            }
            return out
        }

        internal fun parseBangumiNativeNames(raw: String): List<String> {
            val rows = parseObject(raw)?.rows("list").orEmpty()
            return rows.mapNotNull { it["name"].str() }
        }

        internal fun parseWikidataLabels(raw: String): List<String> {
            val rows = parseObject(raw)?.rows("search").orEmpty()
            val preferred = mutableListOf<String>()
            val secondary = mutableListOf<String>()
            for (row in rows) {
                val label = row.obj("display")?.obj("label")?.get("value").str()
                    ?: row["label"].str()
                    ?: continue
                val desc = (row.obj("display")?.obj("description")?.get("value").str()
                    ?: row["description"].str().orEmpty()).lowercase()
                if (WIKIDATA_ANIME_KEYWORDS.any { it in desc }) {
                    preferred.add(label)
                } else if (desc.isNotBlank() && ("series" in desc || "film" in desc || "novel" in desc)) {
                    secondary.add(label)
                }
            }
            return preferred + secondary
        }

        internal fun parseKitsuTitles(raw: String): List<String> {
            val rows = parseObject(raw)?.rows("data").orEmpty()
            val out = mutableListOf<String>()
            for (row in rows) {
                val attrs = row.obj("attributes") ?: continue
                val titles = attrs.obj("titles")
                titles?.get("en_jp").str()?.let(out::add)
                titles?.get("en").str()?.let(out::add)
                attrs["canonicalTitle"].str()?.let(out::add)
            }
            return out
        }

        internal fun parseJikanTitles(raw: String): List<String> {
            val rows = parseObject(raw)?.rows("data").orEmpty()
            val out = mutableListOf<String>()
            for (row in rows) {
                row["title"].str()?.let(out::add)
                row["title_english"].str()?.let(out::add)
            }
            return out
        }

        internal fun rankAndDedupe(candidates: List<String>, excludeQuery: String = ""): List<String> =
            MultilingualQueryNormalizer.rankAndDedupe(candidates, excludeQuery)

        private fun encodeCandidates(candidates: List<String>): String =
            buildJsonArray { candidates.forEach { add(JsonPrimitive(it)) } }.toString()

        private fun decodeCandidates(raw: String): List<String>? = runCatching {
            val arr = Json.parseToJsonElement(raw) as? JsonArray ?: return@runCatching null
            arr.mapNotNull { it.str() }
        }.getOrNull()
    }
}
