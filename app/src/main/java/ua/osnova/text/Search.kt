package ua.osnova.text

/**
 * Search in one language's text. Word forms are found by their start ("свобода" also finds "свободи",
 * "свободу"; "citizen" finds "citizens"), every word of the query must be in the same paragraph, case and
 * the kind of apostrophe do not matter. A number on its own ("24", "ст. 24", "article 131-1") is also an
 * article to go to.
 */
object Search {
    /** One paragraph found: where it is, a piece of it to show, and the words to mark in that piece. */
    class Hit(val page: Int, val paragraph: Int, val excerpt: String, val marks: List<IntRange>)

    private const val MAX_HITS = 200
    /** A query longer than this (something pasted) is cut: searching stays quick on any phone. */
    private const val MAX_QUERY = 100
    private const val MAX_WORDS = 10
    /** How much text to show before the first word found, so it is never cut off the end of the line. */
    private const val BEFORE = 50
    private const val EXCERPT = 220

    private val WORD = Regex("[\\p{L}\\p{N}']+")
    private val NUMBER = Regex("^(?:ст(?:атт[яі])?|art(?:icle)?)?\\.?\\s*(\\d{1,3}(?:-\\d)?)$")

    /** Lower case and one kind of apostrophe, letter for letter: positions stay the same as in the text. */
    fun normalize(s: String): String = buildString(s.length) {
        for (ch in s) append(if (ch == '’' || ch == 'ʼ' || ch == '`' || ch == '‘') '\'' else ch.lowercaseChar())
    }

    /** The beginnings of the query's words that a word in the text must start with. */
    fun stems(query: String): List<String> = WORD.findAll(normalize(query.take(MAX_QUERY))).map { it.value.trim('\'') }
        .filter { it.isNotEmpty() }
        .map { w ->
            when {
                w.any { it.isDigit() } -> w
                // Long words lose their ending: what is left still says which word it is
                w.length >= 6 -> w.dropLast(2)
                w.length >= 4 -> w.dropLast(1)
                else -> w
            }
        }
        .distinct().take(MAX_WORDS).toList()

    /** The article number in a query like "24", "ст. 24", "стаття 131-1" or "article 24", or null. */
    fun articleNumber(query: String): String? = NUMBER.find(normalize(query.trim()))?.groupValues?.get(1)

    /**
     * Paragraphs that have every word of [query], in the order of the text; at most [MAX_HITS]. For an
     * article number ("ст. 24") only the number is looked for: "ст" is not a word to find.
     */
    fun find(c: Constitution, query: String): List<Hit> {
        val stems = stems(articleNumber(query) ?: query)
        if (stems.isEmpty()) return emptyList()
        val hits = ArrayList<Hit>()
        for ((pi, page) in c.pages.withIndex()) {
            for ((i, text) in c.paragraphs(page).withIndex()) {
                val marks = match(text, stems) ?: continue
                hits += excerpt(pi, i, text, marks)
                if (hits.size >= MAX_HITS) return hits
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
            val word = m.value.trim('\'')
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
