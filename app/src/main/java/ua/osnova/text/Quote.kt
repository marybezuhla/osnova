package ua.osnova.text

/**
 * A quote ready to send: the words in quotation marks and, under them, where they are from, cited the way
 * lawyers do ("стаття 85, частина 2"). What can be chosen on its own is a whole part of an article (with
 * its points "1)", "а)"), a point of the transitional provisions ("16-1."), or a paragraph of the preamble.
 */
object Quote {
    /** One piece that can be quoted on its own: its number as cited, and its paragraphs on the page. */
    class Piece(val number: String, val paragraphs: IntRange)

    private val POINT_OF_PART = Regex("^(\\d+(-\\d+)?|[а-яєіїґa-z])\\)")
    private val POINT_OF_CHAPTER = Regex("^(\\d+(-\\d+)?)\\.\\s")

    fun pieces(c: Constitution, page: Page): List<Piece> {
        val paras = c.paragraphs(page)
        if (page == Page.Preamble) return paras.indices.map { Piece((it + 1).toString(), it..it) }
        // Where a new piece starts, and its number
        val starts = when (page) {
            is Page.Of -> partStarts(paras)
            is Page.Text -> paras.indices.filter { POINT_OF_CHAPTER.containsMatchIn(paras[it]) }
            Page.Preamble -> emptyList()
        }
        return starts.mapIndexed { n, start ->
            val end = (starts.getOrNull(n + 1) ?: paras.size) - 1
            val number = when (page) {
                is Page.Text -> POINT_OF_CHAPTER.find(paras[start])!!.groupValues[1]
                else -> (n + 1).toString()
            }
            Piece(number, start..end)
        }
    }

    /** What comes before the source on its own line. */
    const val SOURCE_MARK = "— "

    /** The chosen paragraphs in quotation marks. */
    fun body(paragraphs: List<String>, open: String, close: String): String = open + paragraphs.joinToString("\n") + close

    /**
     * The paragraphs that begin a part of an article. Not a new part: a point ("1)", "а)"), a paragraph in small
     * letters (the lines of a point, as in article 150), or the words of an oath in quotation marks, until the
     * quotation closes (articles 79 and 104).
     */
    private fun partStarts(paras: List<String>): List<Int> {
        var quoted = false
        return paras.indices.filter { i ->
            val p = paras[i]
            val starts = !quoted && !POINT_OF_PART.containsMatchIn(p) && p.first().isUpperCase()
            quoted = quoted != (quotes(p) % 2 == 1)
            starts
        }
    }

    /** Quotation marks in a paragraph: the Ukrainian text uses " for both ends, the English “ and ”. */
    private fun quotes(p: String) = p.count { it == '"' || it == '“' || it == '”' }

    /** The text to send: the quote, then a line with the source. */
    fun text(paragraphs: List<String>, open: String, close: String, source: String): String =
        body(paragraphs, open, close) + "\n" + SOURCE_MARK + source
}
