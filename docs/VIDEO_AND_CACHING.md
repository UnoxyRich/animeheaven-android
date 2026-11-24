# Video Playback, Caching & Preloading Engine

This document covers the networking, HTML parsing, multi-tier caching, Wi-Fi/cellular predictive preloading, and adaptive multi-mirror Media3 playback stack in `animeheaven-android`.

---

## 1. Networking & HTML Parsing

### OkHttp & Connection Reuse ([`AnimeHeavenClient.kt`](../app/src/main/java/me/animeheaven/android/data/network/AnimeHeavenClient.kt))
- Base URL: `https://animeheaven.me`
- Dispatcher limits: `maxRequests = 32`, `maxRequestsPerHost = 12`
- ConnectionPool: `12` idle connections, `5 min` keep-alive
- HTTP Disk Cache: `24 MB` under `cacheDir/http_cache` (used for HTML/API calls; stripped from Coil via `okHttpClient.newBuilder().cache(null).build()` so artwork is never double-cached on flash)
- Cancellable requests via `OkHttpClient.awaitResponse(request)` using `suspendCancellableCoroutine` + `cont.invokeOnCancellation { call.cancel() }`, cancelling underlying sockets immediately on rapid typing or back navigation.
- [`CachedDns`](../app/src/main/java/me/animeheaven/android/data/network/NetworkOptimizer.kt): 5-minute in-memory DNS TTL cache with stale IP fallback on `UnknownHostException`.

### Network Policy & Preconnection ([`NetworkOptimizer.kt`](../app/src/main/java/me/animeheaven/android/data/network/NetworkOptimizer.kt))
- `NetworkPolicy` tracks unmetered Wi-Fi vs. metered cellular connections reactively via `ConnectivityManager.NetworkCallback`, avoiding Binder IPC polling per preload check and automatically gating aggressive `8 MB` next-episode video preloads on metered mobile data.
- `NetworkPreconnector` runs async head/DNS warming for AnimeHeaven and learned top healthy video CDN hosts from `MirrorHealthTracker`.

### HTML Parsing ([`AnimeHeavenParser.kt`](../app/src/main/java/me/animeheaven/android/data/parser/AnimeHeavenParser.kt))
All CSS selectors are compiled **once** as Jsoup `Evaluator`s (`QueryParser.parse(...)`). Parsers handle:
- Home feed (`index.php` -> banners, schedule, popular strip, current season path/label)
- Catalog pages (`new.php`, `popular.php`, seasonal `*.php`, `tags.php?tag=...`)
- Fast search (`fastsearch.php?xhr=1&s=...`) & full search (`search.php?s=...`)
- Anime detail (`anime.php?...` -> metadata, tags, episodes, related/similar cards)
- Episode mirror gate extraction (`gate.php` -> episode stream & `<source>` mirror URLs)

---

## 2. Three-Tier Data Caching & Persistence

### Tier 1 — Bounded In-Memory LRU ([`LruCacheMap.kt`](../app/src/main/java/me/animeheaven/android/data/cache/LruCacheMap.kt), [`AnimeHeavenRepository.kt`](../app/src/main/java/me/animeheaven/android/data/repository/AnimeHeavenRepository.kt))
Synchronized access-ordered `LinkedHashMap` caches bound heap usage:
- `cachedCharts`: max `24`
- `cachedGrids`: max `24`
- `cachedDetails`: max `64`
- `cachedStreams`: max `48`
- `cachedFastSearch`: max `100`

Soft TTLs drive Stale-While-Revalidate (SWR) background refreshes:
- `TTL_HOME_MS` = `5 min`
- `TTL_CHART_MS` = `5 min`
- `TTL_SEASONAL_MS` = `30 min`
- `TTL_SEARCH_MS` = `30 min`
- `TTL_DETAIL_MS` = `15 min`
- `TTL_GATE_MS` = `15 min`

### Request Coalescing (`RequestCoalescer`)
Concurrent requests for the same key (e.g., foreground detail load + background preloader) share a single `Deferred<String>` running in `repositoryScope` (`SupervisorJob + Dispatchers.IO`), so cancelling a background caller never aborts the foreground fetch.

### Tier 2 — Fast Home Snapshot ([`CacheSnapshotCodec.kt`](../app/src/main/java/me/animeheaven/android/data/cache/CacheSnapshotCodec.kt))
- Home feed persists atomically as `home_snapshot.v1.json` in `filesDir`.
- **Zero-AST Streaming Reader**: `StreamingJsonReader` reads JSON tokens directly into `HomeFeed` models without allocating intermediate `Map`/`List` AST objects.
- When `web_cache.parsedJson` is present, `AnimeHeavenRepository` skips redundant GZIP+Base64 HTML payload writes and persists off the caller path.

### Tier 3 — Room Database v3 ([`AppDatabase.kt`](../app/src/main/java/me/animeheaven/android/data/local/AppDatabase.kt))
Database file: `animeheaven_tv.db` (kept compatible in name/schema v3), opened with `JournalMode.WRITE_AHEAD_LOGGING` and `PRAGMA synchronous = NORMAL`, `temp_store = MEMORY`, `cache_size = -4000`.

| Table | Purpose | Key Indexes / Notes |
| :--- | :--- | :--- |
| `bookmarks` | User saved anime (`addedAt DESC`) | PrimaryKey `animeId` |
| `watch_progress` | Per-episode position, duration, `isWatched`, `nextEpisodeKey` | Indexed on `(animeId, updatedAt)` and `(updatedAt)`. `observeLatestPerAnime()` returns 1 row per anime (`LIMIT 30`). |
| `search_history` | Recent search queries (`LIMIT 10`) | PrimaryKey `query` |
| `web_cache` | Parsed JSON + optional compressed HTML (`MAX_DISK_RETENTION_MS = 14 days`) | Indexed on `fetchedAt`; stores page caches and AniSkip/AniList responses |

Migrations `MIGRATION_1_2` and `MIGRATION_2_3` preserve local progress and bookmarks cleanly.

### Watch Progress Thresholds (`AnimeHeavenRepository.kt`)
- `MIN_RESUME_THRESHOLD_MS` = `10,000 ms`
- `WATCHED_RATIO` = `0.85f` (episodes >= 85% watched promote Continue Watching to `nextEpisodeKey`)
- `MAX_RESUME_RATIO` = `0.92f` (re-opening past 92% restarts from `0 ms`)

---

## 3. Image Preloading & Memory Cache Parity

Coil 3 keys `MemoryCache` entries by `(url, widthPx, heightPx)`. Preloading and composables (`AhCoverImage`) therefore use identical canonical dimensions from [`ImageTargetSize`](../app/src/main/java/me/animeheaven/android/data/cache/ImageTargetSize.kt):
- `HERO_BANNER`: `1080 × 460`
- `DETAIL_POSTER`: `360 × 540`
- `CONTINUE_CARD`: `480 × 270`
- `POPULAR_CARD`: `320 × 320`
- `STANDARD_CARD`: `420 × 236`
- `SEARCH_THUMB`: `256 × 144`

Coil memory allocation in `AppContainer`: `18%` of memory class on low-RAM devices (`isLowRamDevice` or `memoryClass <= 192 MB`), `25%` on standard phones/tablets, plus a `250 MB` `image_cache` disk cache and `RGB_565` bitmap decoding.

---

## 4. Predictive Preloading ([`AnimePreloader.kt`](../app/src/main/java/me/animeheaven/android/data/cache/AnimePreloader.kt))

- **Band 0 (`band0Gate`, 3 permits)**: Hero images, top Continue Watching episode gate + mirror speed probe + resume-aware `2 MB` video head preload, visible scroll-window artwork, and active episode detail/stream warming.
- **Band 1 (`band1Gate`, 2 permits)**: Background catalog tabs (`New`, `Popular`, seasonal feed), top Home details, and bookmarks, gated behind a `450 ms` startup grace delay (`BAND1_STARTUP_GRACE_MS`) and `yieldIfUserActive()` (`MutableStateFlow` wait up to `6s` with zero polling).

---

## 5. Multi-Mirror Video Streaming & Booster

### Shared Episode Cache Keys ([`VideoCacheManager.kt`](../app/src/main/java/me/animeheaven/android/data/cache/VideoCacheManager.kt))
- `buildEpisodeCacheKey(episodeKey, contentLength, fallbackUrl)` computes `"ep:$episodeKey:$contentLength"` once a probe confirms `contentLength >= MIN_VALID_VIDEO_BYTES`.
- Switching mirrors mid-playback or failover hits already-downloaded chunks in `SimpleCache` (`512 MB` budget on standard storage, `1 GB` when usable space >= `4 GB`; `2 MB` fragment size, `512 KB` chunk size).
- `videoCacheManager.cancelPreloadsExcept(episodeKey)` preserves in-flight preloads for the episode currently opening.

### First-Valid-Mirror Probe & `MirrorHealthTracker`
- `probeMirrorsForPlayback(urls, maxWaitMs = 900L)` races Range `bytes=0-0` probes and starts playback as soon as the **first** valid full-length (`>= 1 MB`) mirror responds, while remaining mirrors continue probing asynchronously for `StreamingChunkBooster`.
- `MirrorHealthTracker` records RTT, failure cooldowns, EWMA `avgBytesPerSec`, and `< 1 MB` stub-clip penalties, persisting host rankings in `SharedPreferences`.

### `StreamingChunkBooster` ([`StreamingChunkBooster.kt`](../app/src/main/java/me/animeheaven/android/data/cache/StreamingChunkBooster.kt))
- Fetches `512 KB` byte spans in parallel across throughput-ranked healthy mirrors into pooled `ByteArray` buffers without holding `SimpleCache` locks during network I/O.
- `Channel.CONFLATED` wakes the booster immediately on seeks (`onPlayheadUpdate`) with `+1 chunk` post-seek anchor tightening.
- Automatically queues MP4 tail chunks when chunk `0` lacks a `moov` box (`containsMoovBox`) for non-faststart files.
- `PlayerViewModel` escalates next-episode preloading once per episode (`PreloadMode.AGGRESSIVE` + unmetered Wi-Fi via `NetworkPolicy`).

### ExoPlayer Configuration
- `buildFastLoadControl()`: `250 ms` start playback buffer, `750 ms` after-rebuffer, RAM-scaled (`15s/35s/24 MB` low-RAM vs `25s/55s/48 MB` standard).
- `leanExtractorsFactory`: `Mp4Extractor`, `FragmentedMp4Extractor`, `MatroskaExtractor`.
- `PlayerScreen`: Async MediaCodec queueing (`forceEnableMediaCodecAsynchronousQueueing()`), decoder fallback, `AudioAttributes` (`USAGE_MEDIA` / `CONTENT_TYPE_MOVIE`), `setHandleAudioBecomingNoisy(true)`, and direct initial seek via `setMediaItem(mediaItem, resumePositionMs)`.

---

## 6. Skip OP / ED Pipeline ([`SkipTimesClient.kt`](../app/src/main/java/me/animeheaven/android/data/network/SkipTimesClient.kt), [`SkipPromptLogic.kt`](../app/src/main/java/me/animeheaven/android/ui/screens/player/SkipPromptLogic.kt))

1. Parses episode integer via `SkipTimesParser.parseEpisodeNumber`.
2. Resolves anime title -> MyAnimeList ID via AniList GraphQL (`https://graphql.anilist.co`), cached in memory + Room `web_cache` (`30 days`).
3. Fetches `https://api.aniskip.com/v2/skip-times/{malId}/{episodeNum}?types=op&types=ed&episodeLength=0`, cached in `web_cache` (`7 days`).
4. `SkipPromptLogic` drives contextual `Skip OP`, `Skip ED`, fallback OP (`skipOpSeconds`), and `Next Episode` prompts inside the 500 ms player poll loop.
