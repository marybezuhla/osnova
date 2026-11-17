package ua.osnova.screens

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.osnova.text.Constitution
import ua.osnova.ui.AboutPage
import ua.osnova.ui.AppLanguage
import ua.osnova.ui.ChapterScreen
import ua.osnova.ui.HomeScreen
import ua.osnova.ui.ReaderScreen
import ua.osnova.ui.SettingsPage
import ua.osnova.ui.ShareContent
import androidx.compose.foundation.background
import ua.osnova.ui.theme.Look
import ua.osnova.ui.theme.Theme
import java.io.File

/**
 * Draws Osnova's screens on the computer, in several themes, sizes and font sizes. A screen that
 * crashes fails the build; the pictures go to build/screens for review.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "uk-w400dp-h1600dp-xhdpi")
class ScreensTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val texts = listOf("uk", "en").associateWith { Constitution.parse(File("src/main/assets/constitution-$it.json").readText()) }
    private val marks = listOf("a:24", "a:3", "preamble")

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // The empty activity that hosts each screen (only known to this test, never to the app)
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
    }

    private fun shot(name: String, theme: Theme, manualClock: Boolean = false, content: @Composable () -> Unit) =
        shoot(compose, name, theme, manualClock, content)

    private fun home(name: String, theme: Theme, lang: String = "uk", query: String = "", bookmarksTab: Boolean = false) =
        shot(name, theme, manualClock = query.isNotEmpty()) {
            HomeScreen(texts.getValue(lang), lang, marks, last = "a:24", query = query, onQuery = {}, showBookmarks = bookmarksTab,
                onShowBookmarks = {}, onOpen = {}, onChapter = {}, onRemoveBookmark = {}, onSettings = {})
        }

    private fun reader(name: String, theme: Theme, lang: String, page: String) = shot(name, theme) {
        ReaderScreen(texts.getValue(lang), lang, page, marks, onToggleBookmark = {}, onLanguage = {}, onBack = {}, onShown = {})
    }

    @Test fun home() = home("01-home", Theme.NEBO)
    @Config(qualifiers = "+night")
    @Test fun homeDark() = home("02-home-dark", Theme.ZORIANA_NICH)
    @Test fun chapter() = shot("03-chapter-ii", Theme.NEBO) { ChapterScreen(texts.getValue("uk"), "uk", "II", onBack = {}, onOpen = {}) }
    @Test fun article() = reader("04-article-3", Theme.NEBO, "uk", "a:3")
    @Config(qualifiers = "+night")
    @Test fun articleEnglishDark() = reader("05-article-3-en-dark", Theme.ZORIANA_NICH, "en", "a:3")
    @Test fun search() = home("06-search", Theme.NEBO, query = "свобода слова")
    @Test fun searchNumber() = home("07-search-number", Theme.NEBO, query = "ст. 24")
    @Test fun bookmarks() = home("08-bookmarks", Theme.KALYNA, bookmarksTab = true)
    @Test fun settings() = shot("09-settings", Theme.NEBO) {
        SettingsPage(Look(), onLook = {}, language = AppLanguage.Choice.AUTO, onLanguage = {}, onAbout = {}, onBack = {})
    }
    @Config(qualifiers = "uk-w400dp-h4200dp-xhdpi")
    @Test fun about() = shot("10-about", Theme.NEBO) { AboutPage(onBack = {}) }
    // The last page: the transitional provisions with the closing lines, in English
    @Config(qualifiers = "uk-w400dp-h6000dp-xhdpi")
    @Test fun lastPageEnglish() = reader("11-last-page-en", Theme.LINEN, "en", "c:XV")
    @Test fun preamble() = reader("12-preamble", Theme.GLYNA, "uk", "preamble")

    // The app in English: its labels and the chapter names
    @Config(qualifiers = "en-w400dp-h1600dp-xhdpi")
    @Test fun homeEnglish() = home("13-home-english-app", Theme.NEBO, lang = "en")

    // A narrow phone with the largest font: nothing may be cut off or break inside a word
    @Config(qualifiers = "uk-w320dp-h2400dp-xhdpi", fontScale = 2.0f)
    @Test fun homeNarrowLargeFont() = home("14-home-narrow-large-font", Theme.NEBO)
    @Config(qualifiers = "uk-w320dp-h2400dp-xhdpi", fontScale = 2.0f)
    @Test fun articleNarrowLargeFont() = reader("15-article-narrow-large-font", Theme.NEBO, "uk", "a:24")
    @Config(qualifiers = "en-w320dp-h3200dp-xhdpi", fontScale = 2.0f)
    @Test fun settingsNarrowLargeFontEnglish() = shot("16-settings-narrow-large-font-en", Theme.NEBO) {
        SettingsPage(Look(), onLook = {}, language = AppLanguage.Choice.EN, onLanguage = {}, onAbout = {}, onBack = {})
    }

    // "Share a quote", as it opens over the reader
    @Test fun share() = shot("19-share", Theme.NEBO) {
        val c = texts.getValue("uk")
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            ShareContent(c, c.article("3")!!, "uk")
        }
    }

    // The reading-aloud sheet: voice, the voices to download, speed, sleep timer
    @Config(qualifiers = "uk-w400dp-h2000dp-xhdpi")
    @Test fun readAloudSettings() = shot("20-read-aloud-settings", Theme.NEBO) {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            ua.osnova.ui.ReadAloudSettings(ua.osnova.speech.ReadAloud.State(), onVoice = {}, onSpeed = {})
        }
    }

    // A tablet held sideways, and an unfolded phone
    @Config(qualifiers = "uk-w1280dp-h800dp-mdpi")
    @Test fun tablet() = reader("17-tablet-article", Theme.NEBO, "uk", "a:24")
    @Config(qualifiers = "uk-w673dp-h841dp-xhdpi")
    @Test fun foldable() = home("18-foldable-home", Theme.MAK)
}
