package ua.osnova.text

/**
 * Search in one language's text. Word forms are found by their start ("свобода" also finds "свободи",
 * "свободу"; "citizen" finds "citizens"), every word of the query must be in the same paragraph, case and
 * the kind of apostrophe (or none at all) do not matter, nor Latin letters typed into a Ukrainian word. A number
 * on its own ("24", "ст. 24", "статтю 24", "24 стаття", "article 131-1") is also an article to go to, and
 * "розділ XV" or "chapter 4" a chapter.
 */
object Search {
    /** One paragraph found: where it is, a piece of it to show, and the words to mark in that piece. */
    class Hit(val page: Int, val paragraph: Int, val excerpt: String, val marks: List<IntRange>)

    /** At most this many are shown (a search for a common word): "200+". */
    const val MAX_HITS = 200
    /** A query longer than this (something pasted) is cut: searching stays quick on any phone. */
    private const val MAX_QUERY = 100
    private const val MAX_WORDS = 10
    /** How much text to show before the first word found, so it is never cut off the end of the line. */
    private const val BEFORE = 50
    private const val EXCERPT = 220

    private val WORD = Regex("[\\p{L}\\p{N}']+")
    /** "ст", "стаття", "статтю", "art", "article": any form of the word, and of "частина", "пункт" after the number */
    private const val ARTICLE = "(?:ст\\p{L}*|art\\p{L}*)\\.?"
    private const val PART = "(?:\\s*(?:ч|п|част\\p{L}*|пункт\\p{L}*|part|para\\p{L}*|clause)\\.?\\s*\\d+(?:-\\d+)?\\.?)*"
    private val NUMBER = Regex("^(?:$ARTICLE\\s*)?№?\\s*(\\d{1,3}(?:-\\d)?)\\.?(?:\\s*$ARTICLE)?$PART$")
    /** "розділ XV", "р. 15", "chapter iv", "п. 3 розділу XV" (the chapter is where it goes) */
    private val CHAPTER = Regex("^(?:(?:п|пункт\\p{L}*|clause)\\.?\\s*\\d+(?:-\\d+)?\\.?\\s*)?(?:розд\\p{L}*|р\\.|chapter|ch\\.)\\s*([ivxl]+|\\d{1,2})\\.?$")
    private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV")
    private val AMERICAN = Regex("iz(e|es|ed|ing|ation|ations)$")
    /** Latin letters that look like Ukrainian ones, typed by mistake into a Ukrainian word */
    private val LOOKALIKE = mapOf('a' to 'а', 'c' to 'с', 'e' to 'е', 'i' to 'і', 'o' to 'о', 'p' to 'р', 'x' to 'х', 'y' to 'у', 'k' to 'к', 'm' to 'м', 'h' to 'н', 't' to 'т', 'b' to 'в')

    /** Lower case and one kind of apostrophe, letter for letter: positions stay the same as in the text. */
    fun normalize(s: String): String = buildString(s.length) {
        for (ch in s) append(if (ch == '’' || ch == 'ʼ' || ch == '`' || ch == '‘') '\'' else ch.lowercaseChar())
    }

    /**
     * The query as typed, made like the text: lower case, one apostrophe (also for a quotation mark or nothing
     * between letters: "обов"язки"), Ukrainian letters for Latin or Russian ones inside a Ukrainian word.
     */
    fun clean(query: String): String = WORD.replace(normalize(query.take(MAX_QUERY).replace(Regex("(?<=\\p{L})[\"ʹ](?=\\p{L})"), "'"))) { m ->
        val w = m.value
        if (w.none { it in 'а'..'я' || it in "єіїґёыэъ" }) w
        else buildString { for (ch in w) append(when (ch) { 'ё' -> 'е'; 'э' -> 'е'; 'ы' -> 'и'; 'ъ' -> '\''; else -> LOOKALIKE[ch] ?: ch }) }
    }

    /** The beginnings of the query's words that a word in the text must start with, without apostrophes. */
    fun stems(query: String): List<String> = WORD.findAll(clean(query)).map { it.value.replace("'", "") }
        .filter { it.isNotEmpty() }
        .map { w ->
            when {
                w.any { it.isDigit() } -> w
                w.all { it in 'a'..'z' } -> english(w)
                // Long words lose their ending: what is left still says which word it is
                w.length >= 6 -> w.dropLast(2)
                w.length >= 4 -> w.dropLast(1)
                else -> w
            }
        }
        .distinct().take(MAX_WORDS).toList()

    /** An English word without its ending: "elections", "presidential", "authorities", "taxes", "organization". */
    private fun english(word: String): String {
        // The text spells "-ise" ("organisation"), but "citizen" stays as it is
        val w = word.replace(AMERICAN, "is$1")
        return when {
            w.length >= 8 && w.endsWith("ial") -> w.dropLast(3)
            w.length >= 5 && w.endsWith("ies") -> w.dropLast(3)
            w.length >= 6 && w.endsWith("ing") -> w.dropLast(3)
            w.length >= 5 && (w.endsWith("xes") || w.endsWith("ses") || w.endsWith("ches") || w.endsWith("shes")) -> w.dropLast(2)
            w.length >= 6 && w.endsWith("ed") -> w.dropLast(2)
            w.length >= 7 && w.endsWith("y") -> w.dropLast(1)
            w.length >= 4 && w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us") && !w.endsWith("is") -> w.dropLast(1)
            else -> w
        }
    }

    /** The article number in a query like "24", "ст. 24", "статтю 131-1", "24 стаття", "ст 24 ч 2" or "article 24", or null. */
    fun articleNumber(query: String): String? = NUMBER.find(clean(query).trim())?.groupValues?.get(1)

    /** The chapter asked for: "розділ XV", "розділ 15", "chapter iv", or its title ("перехідні положення"); or null. */
    fun chapter(c: Constitution, query: String): Chapter? {
        val q = clean(query).trim()
        CHAPTER.find(q)?.groupValues?.get(1)?.let { n ->
            val roman = n.toIntOrNull()?.let { ROMAN.getOrNull(it - 1) } ?: n.uppercase()
            return c.chapters.firstOrNull { it.num == roman }
        }
        val stems = stems(query).takeIf { s -> s.isNotEmpty() && s.none { w -> w.any { it.isDigit() } } } ?: return null
        return c.chapters.firstOrNull { match(it.title, stems) != null && stems.sumOf { it.length } * 3 >= it.title.count(Char::isLetter) }
    }

    /**
     * Paragraphs that have every word of [query], in the order of the text; at most [MAX_HITS] + 1. For an
     * article number ("ст. 24") only the number is looked for: "ст" is not a word to find.
     */
    fun find(c: Constitution, query: String): List<Hit> {
        val stems = stems(articleNumber(query) ?: query)
        if (articleNumber(query) == null && CHAPTER.containsMatchIn(clean(query).trim())) return emptyList()
        if (stems.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        for ((pi, page) in c.pages.withIndex()) {
            for ((i, text) in c.paragraphs(page).withIndex()) {
                val marks = match(text, stems) ?: continue
                hits += excerpt(pi, i, text, marks)
                // One more than shown, so "200+" is said only when there are more
                if (hits.size > MAX_HITS) return hits
            }
        }
        return hits
    }

    /** The words of [text] that start with one of [stems], or null if any stem is missing. */
    fun match(text: String, stems: List<String>): List<IntRange>? {
        val norm = normalize(text)
        val found = HashSet<String>()
        val marks = ArrayList<IntRange>()
        for (m in WORD.findAll(norm)) {
            val word = m.value.replace("'", "")
            // A word counts for every stem it starts with ("суддя" for both "суд" and "судд")
            val its = stems.filter { s -> if (s.any { it.isDigit() }) word == s else word.startsWith(s) }
            if (its.isEmpty()) continue
            found += its
            marks += m.range
        }
        return marks.takeIf { found.size == stems.size }
    }

    private fun excerpt(page: Int, paragraph: Int, text: String, marks: List<IntRange>): Hit {
        val start = (marks.first().first - BEFORE).coerceAtLeast(0).let { s ->
            // Begin at a word, not in the middle of one
            if (s == 0) 0 else text.indexOf(' ', s).let { if (it in s until marks.first().first) it + 1 else s }
        }
        val end = (start + EXCERPT).coerceAtMost(text.length).let { e ->
            if (e == text.length) e else text.lastIndexOf(' ', e).takeIf { it > marks.first().last } ?: e
        }
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        val shift = prefix.length - start
        val inside = marks.filter { it.first >= start && it.last < end }.map { (it.first + shift)..(it.last + shift) }
        return Hit(page, paragraph, prefix + text.substring(start, end) + suffix, inside)
    }
}
