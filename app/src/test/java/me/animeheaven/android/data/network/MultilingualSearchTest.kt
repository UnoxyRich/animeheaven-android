package me.animeheaven.android.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualSearchTest {

    @Test
    fun `detects scripts and language hints accurately across world languages`() {
        assertEquals(QueryScript.LATIN, MultilingualQueryNormalizer.analyze("Sousou no Frieren").script)
        assertEquals(QueryScript.KANA_ONLY, MultilingualQueryNormalizer.analyze("しんげきのきょじん").script)
        assertEquals(QueryScript.HAN_CJK, MultilingualQueryNormalizer.analyze("葬送的芙莉莲").script)
        assertEquals("zh", MultilingualQueryNormalizer.analyze("葬送的芙莉莲").wikidataLangHint)
        assertEquals("ja", MultilingualQueryNormalizer.analyze("葬送のフリーレン").wikidataLangHint)

        val ko = MultilingualQueryNormalizer.analyze("진격의 거인")
        assertEquals(QueryScript.OTHER_NON_LATIN, ko.script)
        assertEquals("ko", ko.wikidataLangHint)

        val ru = MultilingualQueryNormalizer.analyze("Атака титанов")
        assertEquals(QueryScript.OTHER_NON_LATIN, ru.script)
        assertEquals("ru", ru.wikidataLangHint)

        val ar = MultilingualQueryNormalizer.analyze("هجوم العمالقة")
        assertEquals(QueryScript.OTHER_NON_LATIN, ar.script)
        assertEquals("ar", ar.wikidataLangHint)

        val th = MultilingualQueryNormalizer.analyze("ผ่าพิภพไททัน")
        assertEquals(QueryScript.OTHER_NON_LATIN, th.script)
        assertEquals("th", th.wikidataLangHint)
    }

    @Test
    fun `strips macrons and generates romaji phonetic variants`() {
        val analyzed = MultilingualQueryNormalizer.analyze("Sōsō no Furīren")
        assertEquals("Soso no Furiren", analyzed.normalizedLatin)
        assertTrue(analyzed.romajiVariants.contains("sousou no furiren"))

        val kunrei = MultilingualQueryNormalizer.generateRomajiVariants("Singeki no Kyozin")
        assertTrue(kunrei.any { it.contains("shingeki") })

        val soma = MultilingualQueryNormalizer.generateRomajiVariants("Shokugeki no Soma")
        assertTrue(soma.contains("shokugeki no souma"))
    }

    @Test
    fun `converts hiragana and katakana to hepburn romaji`() {
        val shingeki = MultilingualQueryNormalizer.kanaToRomajiCandidates("しんげき の きょじん")
        assertTrue(shingeki.contains("shingeki no kyojin"))

        val kimetsu = MultilingualQueryNormalizer.kanaToRomajiCandidates("きめつのやいば")
        assertTrue(kimetsu.contains("kimetsunoyaiba"))

        val katakana = MultilingualQueryNormalizer.kanaToRomajiCandidates("ワンピース")
        assertTrue(katakana.contains("wanpiisu"))
        assertTrue(katakana.contains("wanpisu"))

        val sokuon = MultilingualQueryNormalizer.kanaToRomajiCandidates("けっこん")
        assertTrue(sokuon.contains("kekkon"))
    }

    @Test
    fun `parses AniList, Bangumi, Wikidata, and Kitsu responses cleanly`() {
        val aniListJson = """
            {"data":{"Page":{"media":[{"title":{"romaji":"Sousou no Frieren","english":"Frieren: Beyond Journey's End"},"format":"TV","popularity":500000}]}}}
        """.trimIndent()
        assertEquals(
            listOf("Sousou no Frieren", "Frieren: Beyond Journey's End"),
            MultilingualSearchResolver.parseAniListSingle(aniListJson)
        )

        val bangumiJson = """
            {"results":2,"list":[{"id":420628,"name":"薬屋のひとりごと","name_cn":"药屋少女的呢喃"}]}
        """.trimIndent()
        assertEquals(listOf("薬屋のひとりごと"), MultilingualSearchResolver.parseBangumiNativeNames(bangumiJson))

        val wikidataJson = """
            {"search":[
              {"id":"Q999","label":"Random Company","description":"software company"},
              {"id":"Q586025","label":"Attack on Titan","description":"Japanese manga series"}
            ]}
        """.trimIndent()
        assertEquals(listOf("Attack on Titan"), MultilingualSearchResolver.parseWikidataLabels(wikidataJson))

        val kitsuJson = """
            {"data":[{"attributes":{"canonicalTitle":"Shokugeki no Souma","titles":{"en":"Food Wars! Shokugeki no Soma","en_jp":"Shokugeki no Souma"}}}]}
        """.trimIndent()
        assertEquals(
            listOf("Shokugeki no Souma", "Food Wars! Shokugeki no Soma", "Shokugeki no Souma"),
            MultilingualSearchResolver.parseKitsuTitles(kitsuJson)
        )
    }

    @Test
    fun `rankAndDedupe filters non-latin and deduplicates case and punctuation`() {
        val ranked = MultilingualSearchResolver.rankAndDedupe(
            candidates = listOf(
                "葬送のフリーレン",
                "Sousou no Frieren",
                "Sōusōu no Frieren",
                "sousou no frieren!",
                "Frieren: Beyond Journey’s End"
            ),
            excludeQuery = "Soso no Frieren"
        )
        assertEquals(listOf("Sousou no Frieren", "Frieren: Beyond Journey's End"), ranked)
        assertFalse(ranked.any { it.contains("葬送") })
    }
}
