package ua.osnova

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.text.Quote
import java.io.File

class QuoteTest {
    private val uk = Constitution.parse(File("src/main/assets/constitution-uk.json").readText())
    private val en = Constitution.parse(File("src/main/assets/constitution-en.json").readText())

    @Test fun partsKeepTheirPoints() {
        // Article 85: part 1 with its points, then part 2
        val p = Quote.pieces(uk, uk.article("85")!!)
        assertEquals(listOf("1", "2"), p.map { it.number })
        assertTrue(p[0].paragraphs.count() > 30)
        assertEquals(listOf("1", "2"), Quote.pieces(uk, uk.article("3")!!).map { it.number })
        // The lines of a point in small letters stay in it: article 150 has two parts, the second its last paragraph
        val p150 = Quote.pieces(uk, uk.article("150")!!)
        assertEquals(listOf("1", "2"), p150.map { it.number })
        assertEquals(8..8, p150[1].paragraphs)
        // Each line of an oath is a part, as the Constitutional Court cites them ("частина третя статті 79")
        assertEquals(6, Quote.pieces(uk, uk.article("79")!!).size)
        assertEquals(2..2, Quote.pieces(uk, uk.article("79")!!)[2].paragraphs)
        assertEquals(5, Quote.pieces(en, en.article("104")!!).size)
    }

    @Test fun piecesCoverThePageOnce() {
        for (c in listOf(uk, en)) for (page in c.pages) {
            val covered = Quote.pieces(c, page).flatMap { it.paragraphs }
            assertEquals(page.key, c.paragraphs(page).indices.toList(), covered)
        }
    }

    @Test fun languagesCiteAlike() {
        for ((a, b) in uk.pages.zip(en.pages)) {
            assertEquals(a.key, Quote.pieces(uk, a).map { it.number }, Quote.pieces(en, b).map { it.number })
        }
    }

    @Test fun transitionalPointsByTheirNumbers() {
        val numbers = Quote.pieces(uk, uk.pages.last()).map { it.number }
        assertEquals("1", numbers.first())
        assertTrue(numbers.contains("16-1"))
        assertEquals(9, Quote.pieces(uk, Page.Preamble).size)
    }

    @Test fun quoteText() {
        assertEquals("«a\nb»\n— Конституція України, стаття 3", Quote.text(listOf("a", "b"), "«", "»", "Конституція України, стаття 3"))
    }
}
