package ua.osnova.text

import org.json.JSONArray
import org.json.JSONObject

/** One article: its number as printed ("3", "131-1"), its paragraphs, and the official edition's notes. */
class Article(val num: String, val parts: List<String>, val notes: List<String>)

/**
 * One chapter ("розділ"): its number ("I", "XV"), its title as printed (in capitals), its articles, and for the
 * transitional provisions, which have no articles, its own paragraphs ([text]).
 */
class Chapter(val num: String, val title: String, val articles: List<Article>, val text: List<String>, val notes: List<String>)

/**
 * A place to read: the preamble, one article, or a chapter's own text. The [key] is the same in both
 * languages, so a bookmark or the place last read stays put when the language changes.
 */
sealed class Page(val key: String) {
    object Preamble : Page("preamble")
    class Of(val article: Article, val chapter: Chapter) : Page("a:" + article.num)
    class Text(val chapter: Chapter) : Page("c:" + chapter.num)
}

/** The whole Constitution in one language, word for word as in the app's assets. */
class Constitution(
    val edition: String,
    val preamble: List<String>,
    val preambleNotes: List<String>,
    val chapters: List<Chapter>,
    /** The closing lines after the last provisions ("adopted at the fifth session..."). */
    val closing: List<String>,
) {
    /** Every place to read, in the order of the text. */
    val pages: List<Page> = buildList {
        add(Page.Preamble)
        for (c in chapters) {
            c.articles.forEach { add(Page.Of(it, c)) }
            if (c.text.isNotEmpty()) add(Page.Text(c))
        }
    }

    private val byKey = pages.withIndex().associate { (i, p) -> p.key to i }

    /** The position of a page in [pages], or null for a key this text does not have. */
    fun indexOf(key: String): Int? = byKey[key]

    /** The page of the article with this number ("24", "131-1"), if there is one. */
    fun article(num: String): Page.Of? = indexOf("a:$num")?.let { pages[it] as Page.Of }

    /** The paragraphs of a page, in order. */
    fun paragraphs(page: Page): List<String> = when (page) {
        Page.Preamble -> preamble
        is Page.Of -> page.article.parts
        is Page.Text -> page.chapter.text
    }

    /** The edition's notes of a page. */
    fun notes(page: Page): List<String> = when (page) {
        Page.Preamble -> preambleNotes
        is Page.Of -> page.article.notes
        is Page.Text -> page.chapter.notes
    }

    companion object {
        /** Reads the JSON made by scripts/text/constitution_{uk,en}.py. */
        fun parse(json: String): Constitution {
            val o = JSONObject(json)
            val preamble = o.getJSONArray("preamble")
            val texts = ArrayList<String>()
            val notes = ArrayList<String>()
            for (i in 0 until preamble.length()) {
                val p = preamble.getJSONObject(i)
                if (p.has("text")) texts += p.getString("text") else notes += p.getString("note")
            }
            val sections = o.getJSONArray("sections")
            val chapters = (0 until sections.length()).map { i ->
                val s = sections.getJSONObject(i)
                val arts = s.getJSONArray("articles")
                Chapter(
                    num = s.getString("num"),
                    title = s.getString("title"),
                    articles = (0 until arts.length()).map { j ->
                        val a = arts.getJSONObject(j)
                        Article(a.getString("num"), strings(a.getJSONArray("parts")), strings(a.getJSONArray("notes")))
                    },
                    text = strings(s.getJSONArray("text")),
                    notes = strings(s.getJSONArray("notes")),
                )
            }
            return Constitution(o.getString("edition"), texts, notes, chapters, strings(o.getJSONArray("closing")))
        }

        private fun strings(a: JSONArray): List<String> = (0 until a.length()).map { a.getString(it) }
    }
}
