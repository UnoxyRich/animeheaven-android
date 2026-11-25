package me.animeheaven.android.data.network

import java.text.Normalizer

enum class QueryScript {
    LATIN,
    KANA_ONLY,
    HAN_CJK,
    OTHER_NON_LATIN
}

data class AnalyzedQuery(
    val original: String,
    val normalizedLatin: String,
    val script: QueryScript,
    val wikidataLangHint: String,
    val kanaRomajiCandidates: List<String>,
    val romajiVariants: List<String>
)

/**
 * Pure-Kotlin script detector, Latin macron/diacritic folder, Japanese Kana-to-Hepburn-Romaji
 * transliterator, and bounded Romaji spelling-variant generator.
 */
object MultilingualQueryNormalizer {

    private val combiningMarks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")
    private val matchPunctuation = Regex("[^\\p{L}\\p{N}\\s]+")
    private val latinLetters = Regex(".*[A-Za-z].*")

    private val kanaDigraphs: Map<String, String> = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu", "どぅ" to "du",
        "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
        "しぇ" to "she", "じぇ" to "je", "ちぇ" to "che",
        "つぁ" to "tsa", "つぃ" to "tsi", "つぇ" to "tse", "つぉ" to "tso",
        "ヴぁ" to "va", "ヴぃ" to "vi", "ヴぇ" to "ve", "ヴぉ" to "vo"
    )

    private val kanaSingles: Map<Char, String> = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'ゐ' to "wi", 'ゑ' to "we", 'を' to "wo", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa", 'ゔ' to "vu"
    )

    private val kunreiReplacements: List<Pair<Regex, String>> = listOf(
        Regex("\\bsi") to "shi",
        Regex("\\bti") to "chi",
        Regex("\\btu") to "tsu",
        Regex("\\bhu") to "fu",
        Regex("\\bzi") to "ji",
        Regex("sy([auo])") to "sh$1",
        Regex("ty([auo])") to "ch$1",
        Regex("zy([auo])") to "j$1"
    )

    fun analyze(rawQuery: String): AnalyzedQuery {
        val cleaned = rawQuery.trim().replace(whitespace, " ")
        val foldedLatin = stripDiacritics(cleaned)
        var hasLatin = false
        var hasKana = false
        var hasHan = false
        var hasHangul = false
        var hasCyrillic = false
        var hasArabic = false
        var hasThai = false
        var hasDevanagari = false
        var hasOtherNonLatin = false

        var index = 0
        while (index < cleaned.length) {
            val cp = cleaned.codePointAt(index)
            index += Character.charCount(cp)
            if (!Character.isLetter(cp)) continue
            when (Character.UnicodeScript.of(cp)) {
                Character.UnicodeScript.LATIN -> hasLatin = true
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> hasKana = true
                Character.UnicodeScript.HAN -> hasHan = true
                Character.UnicodeScript.HANGUL -> hasHangul = true
                Character.UnicodeScript.CYRILLIC -> hasCyrillic = true
                Character.UnicodeScript.ARABIC -> hasArabic = true
                Character.UnicodeScript.THAI -> hasThai = true
                Character.UnicodeScript.DEVANAGARI -> hasDevanagari = true
                Character.UnicodeScript.COMMON, Character.UnicodeScript.INHERITED -> Unit
                else -> hasOtherNonLatin = true
            }
        }

        val script = when {
            hasHan -> QueryScript.HAN_CJK
            hasHangul || hasCyrillic || hasArabic || hasThai || hasDevanagari || hasOtherNonLatin ->
                QueryScript.OTHER_NON_LATIN
            hasKana -> QueryScript.KANA_ONLY
            else -> QueryScript.LATIN
        }

        val langHint = when {
            hasHan && hasKana -> "ja"
            hasHan -> "zh"
            hasKana -> "ja"
            hasHangul -> "ko"
            hasCyrillic -> "ru"
            hasArabic -> "ar"
            hasThai -> "th"
            hasDevanagari -> "hi"
            else -> "en"
        }

        val kanaRomaji = if (script == QueryScript.KANA_ONLY) {
            kanaToRomajiCandidates(cleaned)
        } else {
            emptyList()
        }

        val romajiVariants = if (script == QueryScript.LATIN && hasLatin) {
            generateRomajiVariants(foldedLatin)
        } else {
            emptyList()
        }

        return AnalyzedQuery(
            original = cleaned,
            normalizedLatin = foldedLatin,
            script = script,
            wikidataLangHint = langHint,
            kanaRomajiCandidates = kanaRomaji,
            romajiVariants = romajiVariants
        )
    }

    fun stripDiacritics(input: String): String {
        val replaced = input
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('“', '"')
            .replace('”', '"')
            .replace('‐', '-')
            .replace('–', '-')
            .replace('—', '-')
        val nfkd = Normalizer.normalize(replaced, Normalizer.Form.NFKD)
        val stripped = combiningMarks.replace(nfkd, "")
        return Normalizer.normalize(stripped, Normalizer.Form.NFC)
            .replace(whitespace, " ")
            .trim()
    }

    fun normalizeForMatch(input: String): String =
        stripDiacritics(input)
            .lowercase()
            .replace("'", "")
            .replace(matchPunctuation, " ")
            .replace(whitespace, " ")
            .trim()

    fun isSearchableOnSite(candidate: String): Boolean {
        val folded = stripDiacritics(candidate)
        return folded.isNotBlank() && latinLetters.matches(folded)
    }

    fun rankAndDedupe(candidates: List<String>, excludeQuery: String = ""): List<String> {
        val excludedKey = normalizeForMatch(excludeQuery)
        val seen = HashSet<String>()
        val result = mutableListOf<String>()
        for (raw in candidates) {
            val folded = stripDiacritics(raw)
            if (!isSearchableOnSite(folded)) continue
            val key = normalizeForMatch(folded)
            if (key.isBlank() || key == excludedKey) continue
            if (seen.add(key)) {
                result.add(folded)
            }
        }
        return result
    }

    /**
     * Converts Hiragana and Katakana into Hepburn Romaji. Returns up to 2 variants:
     * standard lengthened romaji and vowel-collapsed form (useful for katakana loanwords).
     */
    fun kanaToRomajiCandidates(raw: String): List<String> {
        val hira = toHiragana(raw)
        val lengthened = StringBuilder(hira.length * 2)
        val compact = StringBuilder(hira.length * 2)

        var i = 0
        while (i < hira.length) {
            val c = hira[i]
            if (c == 'っ') {
                val next = nextKanaRomaji(hira, i + 1)
                if (next != null && next.isNotEmpty()) {
                    val doubled = if (next.startsWith("ch")) 't' else next[0]
                    if (doubled in 'a'..'z' && doubled !in "aeiou") {
                        lengthened.append(doubled)
                        compact.append(doubled)
                    }
                }
                i++
                continue
            }
            if (c == 'ー') {
                val lastVowel = lengthened.lastOrNull()?.takeIf { it in "aeiou" }
                if (lastVowel != null) {
                    lengthened.append(lastVowel)
                }
                i++
                continue
            }
            if (i + 1 < hira.length) {
                val pair = hira.substring(i, i + 2)
                val mapped = kanaDigraphs[pair]
                if (mapped != null) {
                    lengthened.append(mapped)
                    compact.append(mapped)
                    i += 2
                    continue
                }
            }
            val single = kanaSingles[c]
            if (single != null) {
                lengthened.append(single)
                compact.append(single)
            } else if (c.isWhitespace() || c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9') {
                val lower = c.lowercaseChar()
                lengthened.append(lower)
                compact.append(lower)
            } else if (c == '・' || c == '＝' || c == '-') {
                lengthened.append(' ')
                compact.append(' ')
            }
            i++
        }

        val first = lengthened.toString().replace(whitespace, " ").trim()
        val second = compact.toString().replace(whitespace, " ").trim()
        return listOf(first, second).filter { it.isNotBlank() }.distinct()
    }

    /**
     * Produces up to 3 bounded Romaji spelling variants (Kunrei->Hepburn, o<->ou, u<->uu)
     * for fallback when direct AnimeHeaven search returns zero hits.
     */
    fun generateRomajiVariants(foldedLatin: String): List<String> {
        val base = foldedLatin.lowercase()
        val results = linkedSetOf<String>()

        var hepburn = base
        for ((pattern, replacement) in kunreiReplacements) {
            hepburn = pattern.replace(hepburn, replacement)
        }
        if (hepburn != base) results.add(hepburn)

        // Collapse long vowels: ou -> o, uu -> u, oo -> o (e.g., Yuu Yuu -> Yu Yu, Sousou -> Soso)
        val collapsed = hepburn
            .replace("ou", "o")
            .replace("uu", "u")
            .replace("oo", "o")
        if (collapsed != base && collapsed != hepburn) results.add(collapsed)

        // Expand 'o' -> 'ou' in the final >=3-char word (e.g., Shokugeki no Soma -> Shokugeki no Souma)
        val tokens = hepburn.split(' ').toMutableList()
        val lastIdx = tokens.indexOfLast { it.length >= 3 && Regex("(?<=[bcdfghjklmnpqrstvwxyz])o").containsMatchIn(it) }
        if (lastIdx >= 0) {
            tokens[lastIdx] = tokens[lastIdx].replace(Regex("(?<=[bcdfghjklmnpqrstvwxyz])o"), "ou")
            val lastWordExpanded = tokens.joinToString(" ")
            if (lastWordExpanded != base && lastWordExpanded != hepburn && lastWordExpanded.length <= base.length + 4) {
                results.add(lastWordExpanded)
            }
        }

        // Expand short 'o' -> 'ou' in Japanese-like syllables (e.g., Soso -> Sousou)
        val expandedO = hepburn.replace(
            Regex("(?<=[bcdfghjklmnpqrstvwxyz])o(?=[bcdfghjklmnpqrstvwxyz])|(?<=\\S[bcdfghjklmnpqrstvwxyz])o(?=\\s|$)"),
            "ou"
        )
        if (expandedO != base && expandedO != hepburn && expandedO.length <= base.length + 6) {
            results.add(expandedO)
        }

        results.remove(base)
        return results.take(4)
    }

    private fun nextKanaRomaji(hira: String, index: Int): String? {
        if (index >= hira.length) return null
        if (index + 1 < hira.length) {
            kanaDigraphs[hira.substring(index, index + 2)]?.let { return it }
        }
        return kanaSingles[hira[index]]
    }

    private fun toHiragana(raw: String): String = buildString(raw.length) {
        for (c in raw) {
            when (c) {
                in '\u30A1'..'\u30F6' -> append((c.code - 0x60).toChar())
                '\u30FC' -> append('ー')
                else -> append(c)
            }
        }
    }
}
