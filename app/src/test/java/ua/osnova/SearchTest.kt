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
        // The order of the words does not matter, even when one starts the other
        assertEquals(Search.find(uk, "суддя суд").size, Search.find(uk, "суд суддя").size)
        assertTrue(Search.find(uk, "суд суддя").isNotEmpty())
        // Something long pasted in: still quick, still an answer
        Search.find(uk, "свобода ".repeat(5000))
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

    @Test fun typedAsPeopleType() {
        // No apostrophe, a quotation mark for it, Latin "i" in a Ukrainian word, "ё"
        val withIt = Search.find(uk, "обов'язки").size
        assertTrue(withIt > 0)
        assertEquals(withIt, Search.find(uk, "обовязки").size)
        assertEquals(withIt, Search.find(uk, "обов\"язки").size)
        assertEquals(Search.find(uk, "місцеве самоврядування").size, Search.find(uk, "мiсцеве самоврядування").size)
        assertTrue(Search.find(uk, "сiм'я").isNotEmpty())
        // Any form of "стаття", the number on either side, a part after it
        for (q in listOf("статтю 46", "46 стаття", "ст.106", "cт 106", "№ 106", "ст. 106 ч. 2", "стаття 106 п. 15", "106.")) {
            assertEquals(q, if (q.contains("46")) "46" else "106", Search.articleNumber(q))
        }
        assertNull(Search.articleNumber("2024 рік"))
    }

    @Test fun chaptersByNumberOrName() {
        assertEquals("XV", Search.chapter(uk, "розділ XV")?.num)
        assertEquals("XV", Search.chapter(uk, "розділ 15")?.num)
        assertEquals("XV", Search.chapter(uk, "п. 3 розділу XV")?.num)
        assertEquals("XV", Search.chapter(uk, "перехідні положення")?.num)
        assertEquals("IV", Search.chapter(en, "chapter iv")?.num)
        assertNull(Search.chapter(uk, "свобода слова"))
        assertTrue(Search.find(uk, "розділ XV").isEmpty())
    }

    @Test fun englishWordsAsJournalistsType() {
        for (q in listOf("citizens", "presidential election", "authorities", "liberties", "taxes", "organization", "martial law", "Crimea")) {
            assertTrue(q, Search.find(en, q).isNotEmpty())
        }
        // "Crimea" is not "criminal"
        for (h in Search.find(en, "Crimea")) for (m in h.marks) assertTrue(Search.normalize(h.excerpt.substring(m)).startsWith("crimea"))
    }
}
