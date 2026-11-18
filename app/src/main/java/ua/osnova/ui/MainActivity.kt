package ua.osnova.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.ui.theme.Look
import ua.osnova.ui.theme.LookStore
import ua.osnova.ui.theme.OsnovaTheme

class MainActivity : ComponentActivity() {
    /** Light or dark, and the day and night themes; remembered on this phone only. */
    private val look = mutableStateOf(Look())

    /** Both texts, read once in the background when the app opens. */
    private val texts = mutableStateOf<Map<String, Constitution>?>(null)

    private lateinit var places: Places
    private val bookmarks = mutableStateOf(emptyList<String>())
    private val last = mutableStateOf<String?>(null)
    private val textLang = mutableStateOf("uk")
    private val readSpeed = mutableStateOf(1f)
    private val readVoice = mutableStateOf(ua.osnova.speech.ReadAloud.OWN)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The list of recent apps shows no picture of what was read (Android 13 and newer)
        if (android.os.Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)
        look.value = LookStore.load(this)
        places = Places(this)
        bookmarks.value = places.bookmarks()
        last.value = places.last()
        readSpeed.value = places.readSpeed()
        readVoice.value = places.readVoice()
        // A voice deleted last time and not brought back goes now (not when the screen is only made anew:
        // "Undo" may still be on it)
        if (savedInstanceState == null) ua.osnova.speech.VoiceFiles.forgetDeleted(this)
        // Until chosen, the text is in the app's own language
        textLang.value = places.textLanguage() ?: getString(R.string.text_language_default)
        setContent {
            val theme = look.value.current(isSystemInDarkTheme())
            // Status and navigation bar icons follow the chosen theme, not only the phone
            LaunchedEffect(theme.dark) {
                val bars = if (theme.dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            LaunchedEffect(Unit) {
                if (texts.value == null) texts.value = withContext(Dispatchers.Default) { readTexts() }
            }
            OsnovaTheme(theme) {
                val t = texts.value
                if (t == null) Opening() else App(t)
            }
        }
    }

    private fun readTexts(): Map<String, Constitution> = listOf("uk", "en").associateWith { lang ->
        Constitution.parse(assets.open("constitution-$lang.json").bufferedReader().use { it.readText() })
    }

    /**
     * The screens, as a short path kept across restarts: "home", then "chapter:<num>", "read:<page key>",
     * "settings" and "about" on top of it. Back goes one step down.
     */
    @Composable
    private fun App(texts: Map<String, Constitution>) {
        var path by rememberSaveable { mutableStateOf(HOME) }
        val steps = path.split(SEP)
        // One step at a time: a double tap, or a tap on a screen still fading out, does not open (or close)
        // a second screen
        var moved by remember { mutableStateOf(0L) }
        fun settled(): Boolean = (SystemClock.uptimeMillis() - moved > STEP_MS).also { if (it) moved = SystemClock.uptimeMillis() }
        fun go(step: String) { if (path.split(SEP).last() != step && settled()) path = path + SEP + step }
        fun back() { path.split(SEP).let { if (it.size > 1 && settled()) path = it.dropLast(1).joinToString(SEP) } }
        BackHandler(enabled = steps.size > 1) { back() }

        val lang = textLang.value
        val c = texts.getValue(lang)
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val res = textResources(lang)

        fun setBookmarks(keys: List<String>) {
            bookmarks.value = keys
            places.setBookmarks(keys)
        }
        fun removeBookmark(key: String) {
            val before = bookmarks.value
            setBookmarks(before - key)
            val page = c.indexOf(key)?.let { c.pages[it] } ?: return
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val answer = snackbar.showSnackbar(
                    getString(R.string.bookmark_removed, pageTitle(res, page, lang)), getString(R.string.undo),
                    withDismissAction = true, duration = SnackbarDuration.Long,
                )
                val now = bookmarks.value
                // Back in its place; bookmarks added or removed meanwhile stay as they are now
                if (answer == SnackbarResult.ActionPerformed && key !in now) {
                    setBookmarks(now.filter { it !in before } + before.filter { it == key || it in now })
                }
            }
        }
        fun toggleBookmark(key: String) {
            if (key in bookmarks.value) {
                removeBookmark(key)
                return
            }
            setBookmarks(listOf(key) + bookmarks.value)
            // Said in words too: a colour change alone is easy to miss
            val page = c.indexOf(key)?.let { c.pages[it] } ?: return
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(getString(R.string.bookmark_added, pageTitle(res, page, lang)), withDismissAction = true)
            }
        }

        var query by rememberSaveable { mutableStateOf("") }
        var showBookmarks by rememberSaveable { mutableStateOf(false) }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // A short fade between screens, instead of a flash
            // Turning pages changes the path but stays on the same screen: no fade then. A screen fading
            // out keeps showing what it showed, so each kind of screen remembers its own last step.
            val lastOfKind = remember { HashMap<String, String>() }
            val kind = steps.last().substringBefore(':')
            lastOfKind[kind] = steps.last()
            // Each screen keeps where its list was scrolled to, for "back" (a reader is one, whichever page it shows)
            val saved = rememberSaveableStateHolder()
            // ...but a reader closed is opened anew next time, at the page asked for
            LaunchedEffect(kind) { if (kind != "read") saved.removeState("read") }
            Crossfade(targetState = kind, animationSpec = tween(120), label = "screen") { shown ->
                val step = lastOfKind[shown] ?: return@Crossfade
                saved.SaveableStateProvider(if (shown == "read") shown else step) {
                when {
                    step == HOME -> HomeScreen(
                        c, lang, bookmarks.value, last.value, query, { query = it }, showBookmarks, { showBookmarks = it },
                        onOpen = { go("read:$it") },
                        onChapter = { go("chapter:$it") },
                        onRemoveBookmark = ::removeBookmark,
                        onSettings = { go(SETTINGS) },
                    )
                    step.startsWith("chapter:") -> ChapterScreen(c, lang, step.removePrefix("chapter:"), ::back) { go("read:$it") }
                    step.startsWith("read:") -> ReaderScreen(
                        c, lang, step.removePrefix("read:"), bookmarks.value,
                        onToggleBookmark = ::toggleBookmark,
                        onLanguage = { textLang.value = it; places.setTextLanguage(it) },
                        onBack = ::back,
                        readSpeed = readSpeed.value,
                        readVoice = readVoice.value,
                        onReadSpeed = { readSpeed.value = it; places.setReadSpeed(it) },
                        onReadVoice = { readVoice.value = it; places.setReadVoice(it) },
                        onShown = { key ->
                            last.value = key
                            places.setLast(key)
                            // A reader still fading out after "back" must not write over the path
                            if (!path.split(SEP).last().startsWith("read:")) return@ReaderScreen
                            // The path keeps the page now shown, so the way back and a restart come back to it
                            path = (path.split(SEP).dropLast(1) + "read:$key").joinToString(SEP)
                        },
                    )
                    step == SETTINGS -> SettingsPage(
                        look.value,
                        onLook = { look.value = it; LookStore.save(this@MainActivity, it) },
                        language = AppLanguage.current(this@MainActivity),
                        onLanguage = { AppLanguage.set(this@MainActivity, it) },
                        onAbout = { go(ABOUT) },
                        onBack = ::back,
                    )
                    step == ABOUT -> AboutPage(::back)
                }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
        }
    }

    private companion object {
        const val HOME = "home"
        const val SETTINGS = "settings"
        const val ABOUT = "about"
        // Page keys and chapter numbers never contain it
        const val SEP = "|"
        // A little longer than the fade between screens (120 ms): a quick second "back" still works
        const val STEP_MS = 150L
    }
}

/** For the moment the texts are being read (usually too short to notice). */
@Composable
private fun Opening() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
