package ua.osnova.screens

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.osnova.speech.ReadAloud
import ua.osnova.text.Constitution
import ua.osnova.ui.AboutPage
import ua.osnova.ui.AppLanguage
import ua.osnova.ui.HomeScreen
import ua.osnova.ui.ReaderScreen
import ua.osnova.ui.ReadingText
import ua.osnova.ui.ShareContent
import ua.osnova.ui.theme.Look
import ua.osnova.ui.theme.LookSettings
import ua.osnova.ui.theme.Theme
import java.io.File

/**
 * The eight pictures for the store pages (and for Komora), in Ukrainian and English, at a phone's size
 * (1080 × 2340). Saved as store-<lang>-NN_name.png and copied into
 * fastlane/metadata/android/{uk,en-US}/images/phoneScreenshots.
 *
 * The order follows the account's standard: home, the main action, what is special, an overview, the second
 * key feature, the dark home, the themes, About.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "uk-w360dp-h780dp-xxhdpi")
class StoreScreens {
    @get:Rule val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    // The language of this run (the app's and the text's), as the app picks it
    private fun lang() = app.getString(ua.osnova.R.string.text_language_default)
    private val c by lazy { Constitution.parse(File("src/main/assets/constitution-${lang()}.json").readText()) }
    private val marks = listOf("a:24", "a:3", "a:34", "preamble")

    @Before fun setUp() {
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
    }

    private fun shot(name: String, theme: Theme, manualClock: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) =
        shoot(compose, "store-${lang()}-$name", theme, manualClock) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) { content() }
        }

    private fun home(name: String, theme: Theme, query: String = "") = shot(name, theme, manualClock = query.isNotEmpty()) {
        HomeScreen(c, lang(), marks, last = "a:24", query = query, onQuery = {}, showBookmarks = false, onShowBookmarks = {},
            onOpen = {}, onChapter = {}, onRemoveBookmark = {}, onSettings = {})
    }

    // 1. Home: the contents, the place to go on from, search
    private fun s1() = home("01_home", Theme.NEBO)

    // 2. The main action: reading an article
    private fun s2() = shot("02_article", Theme.NEBO) {
        ReaderScreen(c, lang(), "a:24", marks, onToggleBookmark = {}, onLanguage = {}, onBack = {}, onShown = {}, reading = ReadAloud.State())
    }

    // 3. What is special: reading aloud offline, the sentence read shaded
    private fun s3() = shot("03_read_aloud", Theme.NEBO) {
        val i = c.indexOf("a:3")!!
        val res = androidx.compose.ui.platform.LocalContext.current.resources
        val head = ReadingText(c, res, lang()).headLength(i)
        val first = c.paragraphs(c.pages[i]).first()
        val state = ReadAloud.State(document = "constitution-${lang()}", playing = true, unit = i, start = head, end = head + first.length)
        ReaderScreen(c, lang(), "a:3", marks, onToggleBookmark = {}, onLanguage = {}, onBack = {}, onShown = {}, reading = state)
    }

    // 4. An overview: search finds every form of a word
    private fun s4() = home("04_search", Theme.NEBO, query = if (lang() == "uk") "свобода" else "freedom")

    // 5. The second key feature: a quote to share, cited as lawyers do
    private fun s5() = shot("05_share", Theme.NEBO) { ShareContent(c, c.article("3")!!, lang()) }

    // 6. The dark home
    private fun s6() = home("06_starry_night", Theme.ZORIANA_NICH)

    // 7. The themes
    private fun s7() = shot("07_themes", Theme.KALYNA) {
        LookSettings(
            Look(day = Theme.KALYNA, night = Theme.MAK), {}, AppLanguage.Choice.AUTO, {},
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        )
    }

    // 8. About: the author's word, support for Ukraine, where the texts come from
    private fun s8() = shot("08_about", Theme.NEBO) { AboutPage(onBack = {}) }

    @Test fun uk1() = s1()
    @Test fun uk2() = s2()
    @Test fun uk3() = s3()
    @Test fun uk4() = s4()
    @Test fun uk5() = s5()
    @Config(qualifiers = "+night") @Test fun uk6() = s6()
    @Test fun uk7() = s7()
    @Test fun uk8() = s8()

    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en1() = s1()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en2() = s2()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en3() = s3()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en4() = s4()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en5() = s5()
    @Config(qualifiers = "en-w360dp-h780dp-night-xxhdpi") @Test fun en6() = s6()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en7() = s7()
    @Config(qualifiers = "en-w360dp-h780dp-xxhdpi") @Test fun en8() = s8()
}
