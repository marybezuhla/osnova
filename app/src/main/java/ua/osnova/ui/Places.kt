package ua.osnova.ui

import android.content.Context

/**
 * What the reader keeps on this phone only: bookmarks, the page last read, and the language of the
 * text. Never needed for anything to work: unreadable means none of them.
 */
class Places(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Page keys (see Page.key), newest first. */
    fun bookmarks(): List<String> = read { it.getString(BOOKMARKS, null)?.split(SEP)?.filter(String::isNotEmpty) } ?: emptyList()

    fun setBookmarks(keys: List<String>) = write { it.putString(BOOKMARKS, keys.joinToString(SEP)) }

    fun last(): String? = read { it.getString(LAST, null) }

    fun setLast(key: String) = write { it.putString(LAST, key) }

    /** "uk" or "en"; null until chosen, then the app's own language is used. */
    fun textLanguage(): String? = read { it.getString(TEXT_LANGUAGE, null)?.takeIf { l -> l == "uk" || l == "en" } }

    fun setTextLanguage(lang: String) = write { it.putString(TEXT_LANGUAGE, lang) }

    private fun <T> read(get: (android.content.SharedPreferences) -> T?): T? = try {
        get(prefs)
    } catch (_: Exception) {
        // Unreadable preferences: as if nothing was kept
        null
    }

    private fun write(put: (android.content.SharedPreferences.Editor) -> Unit) {
        try {
            prefs.edit().also(put).apply()
        } catch (_: Exception) {
            // Only a convenience: reading works the same without it
        }
    }

    private companion object {
        const val FILE = "places"
        const val BOOKMARKS = "bookmarks"
        const val LAST = "last"
        const val TEXT_LANGUAGE = "text_language"
        // Page keys are "preamble", "a:<number>" and "c:<roman>": they never contain a line break
        const val SEP = "\n"
    }
}
