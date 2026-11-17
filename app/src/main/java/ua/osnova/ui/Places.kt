package ua.osnova.ui

import android.content.Context

/**
 * What the reader keeps on this phone only: bookmarks, the page last read, the language of the text,
 * and the voice and speed for reading aloud. Never needed for anything to work: unreadable means none of them.
 */
class Places(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Page keys (see Page.key), newest first. */
    fun bookmarks(): List<String> = read { it.getString(BOOKMARKS, null)?.split(SEP)?.filter(String::isNotEmpty)?.distinct() } ?: emptyList()

    fun setBookmarks(keys: List<String>) = write { it.putString(BOOKMARKS, keys.joinToString(SEP)) }

    fun last(): String? = read { it.getString(LAST, null) }

    fun setLast(key: String) = write { it.putString(LAST, key) }

    /** "uk" or "en"; null until chosen, then the app's own language is used. */
    fun textLanguage(): String? = read { it.getString(TEXT_LANGUAGE, null)?.takeIf { l -> l == "uk" || l == "en" } }

    fun setTextLanguage(lang: String) = write { it.putString(TEXT_LANGUAGE, lang) }

    /** Reading aloud: the speed (as offered in the player) and the voice ("own" or "phone"). */
    fun readSpeed(): Float = read { it.getFloat(READ_SPEED, 1f) }?.takeIf { s -> s in 0.5f..2f } ?: 1f

    fun setReadSpeed(speed: Float) = write { it.putFloat(READ_SPEED, speed) }

    fun readVoice(): String = read { it.getString(READ_VOICE, null) }
        ?.takeIf { it == ua.osnova.speech.ReadAloud.OWN || it == ua.osnova.speech.ReadAloud.PHONE } ?: ua.osnova.speech.ReadAloud.OWN

    fun setReadVoice(voice: String) = write { it.putString(READ_VOICE, voice) }

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
        const val READ_SPEED = "read_speed"
        const val READ_VOICE = "read_voice"
        // Page keys are "preamble", "a:<number>" and "c:<roman>": they never contain a line break
        const val SEP = "\n"
    }
}
