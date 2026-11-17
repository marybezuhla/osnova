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
        val starts = paras.indices.mapNotNull { i ->
            when (page) {
                is Page.Of -> i.takeIf { !POINT_OF_PART.containsMatchIn(paras[i]) }
                is Page.Text -> i.takeIf { POINT_OF_CHAPTER.containsMatchIn(paras[i]) }
                Page.Preamble -> null
            }
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

    /** The text to send: the chosen paragraphs in quotation marks, then a line with the source. */
    fun text(paragraphs: List<String>, open: String, close: String, source: String): String =
        open + paragraphs.joinToString("\n") + close + "\n— " + source
}
