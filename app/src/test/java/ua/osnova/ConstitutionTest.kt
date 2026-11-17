package ua.osnova

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.text.Titles
import java.io.File

/** Both texts are whole, and they match each other article for article and paragraph for paragraph. */
class ConstitutionTest {
    private fun read(lang: String) = Constitution.parse(File("src/main/assets/constitution-$lang.json").readText())
    private val uk = read("uk")
    private val en = read("en")

    @Test fun ukrainianIsWhole() {
        assertEquals(listOf("I", "II", "III", "IV", "V", "VI", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"), uk.chapters.map { it.num })
        assertEquals(166, uk.chapters.sumOf { it.articles.size })
        assertEquals(9, uk.preamble.size)
        assertEquals(1, uk.preambleNotes.size)
        for (num in listOf("1", "3", "24", "64", "131-1", "148-1", "157", "161")) assertNotNull(num, uk.article(num))
        assertEquals(
            "Людина, її життя і здоров'я, честь і гідність, недоторканність і безпека визнаються в Україні найвищою соціальною цінністю.",
            uk.article("3")!!.article.parts.first(),
        )
        assertTrue(uk.edition.contains("01.01.2020"))
    }

    @Test fun englishIsWhole() {
        assertEquals(uk.chapters.map { it.num }, en.chapters.map { it.num })
        assertTrue(en.article("3")!!.article.parts.first().startsWith("The human being, his or her life and health"))
        assertTrue(en.closing.isNotEmpty())
    }

    @Test fun languagesMatch() {
        // The same places in the same order: a bookmark or the last page stays put when the language changes
        assertEquals(uk.pages.map { it.key }, en.pages.map { it.key })
        assertEquals(uk.preamble.size, en.preamble.size)
        for ((a, b) in uk.pages.zip(en.pages)) {
            assertEquals(a.key, uk.paragraphs(a).size, en.paragraphs(b).size)
            assertEquals(a.key, uk.notes(a).size, en.notes(b).size)
        }
    }

    @Test fun everyPageHasText() {
        for (c in listOf(uk, en)) for (p in c.pages) {
            val text = c.paragraphs(p)
            assertTrue(p.key, text.isNotEmpty() && text.all { it.isNotBlank() && it == it.trim() })
        }
    }

    @Test fun pagesInOrder() {
        assertEquals(Page.Preamble, uk.pages.first())
        assertEquals("a:1", uk.pages[1].key)
        // The transitional provisions have no articles: their own text is the last page
        assertEquals("c:XV", uk.pages.last().key)
        assertEquals(uk.pages.size - 1, uk.indexOf("c:XV"))
        assertNull(uk.indexOf("a:121"))
        assertNull(uk.article("999"))
    }

    @Test fun titlesOnlyChangeCase() {
        for ((c, lang) in listOf(uk to "uk", en to "en")) for (ch in c.chapters) {
            val shown = Titles.display(ch.title, lang)
            assertEquals(ch.title, shown.uppercase())
            assertTrue(shown, shown != ch.title)
        }
        assertEquals("Верховна Рада України", Titles.display("ВЕРХОВНА РАДА УКРАЇНИ", "uk"))
        assertEquals("Вибори. Референдум", Titles.display("ВИБОРИ. РЕФЕРЕНДУМ", "uk"))
        assertEquals("Права, свободи та обов'язки людини і громадянина", Titles.display("ПРАВА, СВОБОДИ ТА ОБОВ'ЯЗКИ ЛЮДИНИ І ГРОМАДЯНИНА", "uk"))
        assertEquals("Rights, Freedoms and Duties of the Human Being and the Citizen",
            Titles.display("RIGHTS, FREEDOMS AND DUTIES OF THE HUMAN BEING AND THE CITIZEN", "en"))
        assertEquals("Local Self-government", Titles.display("LOCAL SELF-GOVERNMENT", "en"))
    }
}
