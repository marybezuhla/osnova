package ua.osnova.text

/**
 * Chapter titles are printed in capitals in the official text. On screen they read better in ordinary
 * letters: Ukrainian in sentence case with the names of state bodies kept capitalised, English in title
 * case. Only the letters' case changes (a test checks every title against the text).
 */
object Titles {
    // Capitalised inside a Ukrainian title (in lower case here, as they are found)
    private val UK_NAMES = listOf(
        "автономна республіка крим", "верховна рада", "кабінет міністрів", "конституційний суд", "конституції", "україни",
    )
    private val EN_SMALL = setOf("a", "an", "and", "of", "the", "to", "in", "for", "or")

    fun display(title: String, lang: String): String = if (lang == "uk") uk(title) else en(title)

    private fun uk(title: String): String {
        var s = title.lowercase()
        for (name in UK_NAMES) {
            s = s.replace(name, name.split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) })
        }
        // The first letter, and the first after a full stop ("Вибори. Референдум")
        return s.split(". ").joinToString(". ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    private fun en(title: String): String = title.lowercase().split(' ').mapIndexed { i, w ->
        // "Self-government" keeps its second half small, like the dictionaries
        if (i > 0 && w in EN_SMALL) w else w.replaceFirstChar(Char::uppercaseChar)
    }.joinToString(" ")
}
