package ua.osnova

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.osnova.text.Constitution
import ua.osnova.text.Search
import java.io.File

class SearchTest {
    private val uk = Constitution.parse(File("src/main/assets/constitution-uk.json").readText())
    private val en = Constitution.parse(File("src/main/assets/constitution-en.json").readText())

    @Test fun lettersStayInPlace() {
        assertEquals("обов'язки", Search.normalize("Обов’язки"))
        assertEquals("обов'язки", Search.normalize("ОБОВʼЯЗКИ"))
        assertEquals("Права і свободи".length, Search.normalize("Права і свободи").length)
    }

    @Test fun wordsLoseTheirEndings() {
        assertEquals(listOf("свобо"), Search.stems("Свобода"))
        assertEquals(listOf("прав"), Search.stems("права"))
        assertEquals(listOf("громадяни"), Search.stems("громадянина"))
        // "Рада" also finds "Ради", "Раді"
        assertEquals(listOf("верхов", "рад"), Search.stems("Верховна Рада"))
        assertEquals(listOf("суд", "24"), Search.stems("суд 24"))
        assertTrue(Search.stems("  , ").isEmpty())
    }

    @Test fun numbersAreArticles() {
        assertEquals("24", Search.articleNumber("24"))
        assertEquals("24", Search.articleNumber("ст. 24"))
        assertEquals("131-1", Search.articleNumber("Стаття 131-1"))
        assertEquals("3", Search.articleNumber("article 3"))
        assertNull(Search.articleNumber("свобода"))
        assertNull(Search.articleNumber("2024"))
    }

    @Test fun findsWordForms() {
        // "свободу думки і слова" is in article 34
        val hits = Search.find(uk, "свобода слова")
        assertTrue(hits.any { uk.pages[it.page].key == "a:34" })
        assertTrue(Search.find(uk, "обов’язки").isNotEmpty())
        assertTrue(Search.find(en, "citizens").isNotEmpty())
        assertTrue(Search.find(uk, "").isEmpty())
        assertTrue(Search.find(uk, "zzzqx").isEmpty())
        // "ст. 24" finds where article 24 is named, not every word starting with "ст"
        val byNumber = Search.find(uk, "ст. 24")
        assertTrue(byNumber.isNotEmpty())
        assertTrue(byNumber.all { h -> h.marks.all { h.excerpt.substring(it) == "24" } })
    }

    @Test fun marksAreTheWordsFound() {
        for ((c, q) in listOf(uk to "Верховна Рада", en to "court decision")) {
            val stems = Search.stems(q)
            val hits = Search.find(c, q)
            assertTrue(q, hits.isNotEmpty())
            for (h in hits) {
                assertTrue(h.marks.isNotEmpty())
                for (m in h.marks) {
                    assertTrue(m.first >= 0 && m.last < h.excerpt.length)
                    val word = Search.normalize(h.excerpt.substring(m))
                    assertTrue(word, stems.any { word.startsWith(it) })
                }
            }
        }
    }
}
