package ua.osnova.text

/**
 * A quote ready to send: the words in quotation marks and, under them, where they are from, cited the way
 * lawyers do ("Частина друга статті 85 Конституції України", "Article 85(2) of the Constitution of Ukraine"). What can be chosen on its own is a whole part of an article (with
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

    /** Where parts were left out between the parts chosen. */
    const val GAP = "[…]"

    /** The paragraphs of the [chosen] pieces, with [GAP] on its own line where pieces between them were left out. */
    fun words(paras: List<String>, chosen: List<Piece>): List<String> = buildList {
        for ((n, piece) in chosen.withIndex()) {
            if (n > 0 && chosen[n - 1].paragraphs.last + 1 != piece.paragraphs.first) add(GAP)
            for (i in piece.paragraphs) add(paras[i])
        }
    }

    /**
     * Where the quote is from, in the words a lawyer would write: "Частина друга статті 43 Конституції України",
     * "Пункти 9 та 16-1 розділу XV «Перехідні положення» Конституції України", "Article 43(2) and (5) of the
     * Constitution of Ukraine (official translation)". [chosen] is null when the whole page is quoted.
     */
    fun source(page: Page, chosen: List<Piece>?, lang: String): String {
        val uk = lang == "uk"
        val numbers = chosen?.map { it.number }
        val line = when (page) {
            is Page.Of -> {
                val n = page.article.num
                when {
                    numbers == null -> if (uk) "стаття $n" else "Article $n"
                    uk -> (if (numbers.size == 1) "частина " else "частини ") + list(numbers.map { ordinal(it, PART) }, uk) + " статті $n"
                    else -> "Article $n" + list(numbers.map { "($it)" }, uk)
                }
            }
            Page.Preamble -> when {
                numbers == null -> if (uk) "преамбула" else "the Preamble"
                uk -> (if (numbers.size == 1) "абзац " else "абзаци ") + list(numbers.map { ordinal(it, PARAGRAPH) }, uk) + " преамбули"
                else -> (if (numbers.size == 1) "paragraph " else "paragraphs ") + list(numbers, uk) + " of the Preamble"
            }
            is Page.Text -> {
                val chapter = page.chapter.num + " " + (if (uk) "«" else "“") + Titles.display(page.chapter.title, if (uk) "uk" else "en") + (if (uk) "»" else "”")
                when {
                    numbers == null -> (if (uk) "розділ " else "Chapter ") + chapter
                    uk -> (if (numbers.size == 1) "пункт " else "пункти ") + list(numbers, uk) + " розділу " + chapter
                    else -> (if (numbers.size == 1) "clause " else "clauses ") + list(numbers, uk) + " of Chapter " + chapter
                }
            }
        }
        val whole = if (uk) "$line Конституції України" else "$line of the Constitution of Ukraine (official translation)"
        return whole.replaceFirstChar { it.uppercase() }
    }

    /** "друга", "друга та п'ята", "перша, друга та п'ята"; "(2) and (5)" */
    private fun list(items: List<String>, uk: Boolean): String {
        val and = if (uk) " та " else " and "
        return if (items.size == 1) items[0] else items.dropLast(1).joinToString(", ") + and + items.last()
    }

    private val PART = listOf("перша", "друга", "третя", "четверта", "п'ята", "шоста", "сьома", "восьма", "дев'ята", "десята",
        "одинадцята", "дванадцята", "тринадцята", "чотирнадцята", "п'ятнадцята", "шістнадцята", "сімнадцята", "вісімнадцята",
        "дев'ятнадцята", "двадцята")
    private val PARAGRAPH = listOf("перший", "другий", "третій", "четвертий", "п'ятий", "шостий", "сьомий", "восьмий", "дев'ятий", "десятий",
        "одинадцятий", "дванадцятий", "тринадцятий", "чотирнадцятий", "п'ятнадцятий", "шістнадцятий", "сімнадцятий", "вісімнадцятий",
        "дев'ятнадцятий", "двадцятий")

    /** A part's number as a word ("друга"); a number there is no word for here stays a number. */
    private fun ordinal(number: String, words: List<String>): String = number.toIntOrNull()?.let { words.getOrNull(it - 1) } ?: number

    /** The chosen paragraphs in quotation marks. */
    fun body(paragraphs: List<String>, open: String, close: String): String = open + paragraphs.joinToString("\n") + close

    /**
     * The paragraphs that begin a part of an article: each paragraph in capitals that is not a point ("1)", "а)").
     * A paragraph in small letters is a line of the point before it (article 150). Each line of an oath is a part
     * of its own, as the Constitutional Court cites them (decision No. 11-рп/2010: "частина третя статті 79").
     */
    private fun partStarts(paras: List<String>): List<Int> = paras.indices.filter { i ->
        val p = paras[i]
        !POINT_OF_PART.containsMatchIn(p) && p.trimStart('"', '“', '«').first().isUpperCase()
    }

    /** The text to send: the quote, then a line with the source. */
    fun text(paragraphs: List<String>, open: String, close: String, source: String): String =
        body(paragraphs, open, close) + "\n" + SOURCE_MARK + source
}
