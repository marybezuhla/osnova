package ua.osnova.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.speech.ReadAloud
import ua.osnova.ui.theme.Segments

/**
 * The text itself, one page (the preamble, an article, the transitional provisions) at a time. Swipe or
 * the buttons at the bottom go to the next or previous page; the language of the text can be switched
 * at the top without losing the place.
 */
@Composable
fun ReaderScreen(
    c: Constitution,
    lang: String,
    start: String,
    bookmarks: List<String>,
    onToggleBookmark: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onBack: () -> Unit,
    onShown: (String) -> Unit,
    readSpeed: Float = 1f,
    readVoice: String = ReadAloud.OWN,
    onReadSpeed: (Float) -> Unit = {},
    onReadVoice: (String) -> Unit = {},
    reading: ReadAloud.State = ReadAloud.state.collectAsState().value,
) {
    val scheme = MaterialTheme.colorScheme
    val res = textResources(lang)
    val context = LocalContext.current
    val pager = rememberPagerState(initialPage = c.indexOf(start) ?: 0) { c.pages.size }
    val scope = rememberCoroutineScope()
    // The page settled on is the place to come back to
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(pager, c) {
        snapshotFlow { pager.settledPage }.collect { shown(c.pages[it].key) }
    }
    val current = c.pages[pager.currentPage]
    val marked = current.key in bookmarks
    var sharing by rememberSaveable { mutableStateOf(false) }
    if (sharing) ShareSheet(c, current, lang) { sharing = false }

    // Reading aloud: this text in this language is one document; it stops when the reader closes or the
    // language changes. The pages turn by themselves to follow it.
    val doc = remember(c, res, lang) { ReadingText(c, res, lang) }
    val readingHere = reading.document == doc.id
    DisposableEffect(doc.id) { onDispose { ReadAloud.stop(doc.id) } }
    LaunchedEffect(readingHere, reading.unit) {
        if (readingHere && reading.playing && reading.unit in c.pages.indices && pager.currentPage != reading.unit) {
            pager.animateScrollToPage(reading.unit)
        }
    }
    var readSettings by rememberSaveable { mutableStateOf(false) }
    if (readSettings) {
        ReadAloudSheet(
            reading,
            chapters = true,
            onVoice = { ReadAloud.setVoice(it); onReadVoice(it) },
            onSpeed = { ReadAloud.setSpeed(it); onReadSpeed(it) },
            onDismiss = { readSettings = false },
        )
    }
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            Bar(pageShort(res, current), onBack) {
                if (!readingHere) {
                    IconButton(onClick = { ReadAloud.start(context, doc.document, pager.currentPage, speed = readSpeed, voice = readVoice) }) {
                        Icon(painterResource(R.drawable.ic_read_aloud), contentDescription = stringResource(R.string.read_aloud_start))
                    }
                }
                IconButton(onClick = { sharing = true }) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.share))
                }
                IconButton(onClick = { onToggleBookmark(current.key) }) {
                    Icon(
                        painterResource(if (marked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_add),
                        contentDescription = stringResource(if (marked) R.string.bookmark_remove else R.string.bookmark_add),
                        tint = if (marked) scheme.secondary else scheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.widthIn(max = READING_WIDTH).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Segments(
                    options = listOf("uk" to R.string.text_uk, "en" to R.string.text_en),
                    chosen = lang,
                    onChoose = onLanguage,
                )
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { c.pages[it].key }) { i ->
                val spoken = if (readingHere && reading.unit == i) doc.headLength(i).let { h -> (reading.start - h)..<(reading.end - h) } else null
                PageText(c, res, lang, c.pages[i], isLast = i == c.pages.lastIndex, spoken = spoken)
            }
            if (readingHere) {
                ReadAloudBar(reading, onSettings = { readSettings = true })
            } else Row(
                Modifier.widthIn(max = READING_WIDTH).fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val i = pager.currentPage
                if (i > 0) {
                    val name = pageShort(res, c.pages[i - 1])
                    val said = res.getString(R.string.previous, name)
                    TextButton(
                        onClick = { scope.launch { pager.animateScrollToPage(i - 1) } },
                        modifier = Modifier.semantics { contentDescription = said },
                    ) { Text(res.getString(R.string.previous_short, name)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
                if (i < c.pages.lastIndex) {
                    val name = pageShort(res, c.pages[i + 1])
                    val said = res.getString(R.string.next, name)
                    TextButton(
                        onClick = { scope.launch { pager.animateScrollToPage(i + 1) } },
                        modifier = Modifier.semantics { contentDescription = said },
                    ) { Text(res.getString(R.string.next_short, name)) }
                }
            }
        }
    }
}

/** A short name for the buttons: "Стаття 4", "Preamble", "Розділ XV". */
private fun pageShort(res: Resources, page: Page): String = when (page) {
    Page.Preamble -> res.getString(R.string.preamble)
    is Page.Of -> res.getString(R.string.article, page.article.num)
    is Page.Text -> res.getString(R.string.chapter, page.chapter.num)
}

@Composable
private fun PageText(c: Constitution, res: Resources, lang: String, page: Page, isLast: Boolean, spoken: IntRange?) {
    val scheme = MaterialTheme.colorScheme
    val paras = c.paragraphs(page)
    // Where each paragraph starts in the page's text as read aloud (paragraphs joined by line breaks)
    val starts = remember(paras) { paras.runningFold(0) { at, p -> at + p.length + 1 } }
    // The sentence being read, as a range in each paragraph it touches
    fun spokenIn(j: Int): IntRange? {
        val r = spoken ?: return null
        val a = (r.first - starts[j]).coerceIn(0, paras[j].length)
        val b = (r.last + 1 - starts[j]).coerceIn(0, paras[j].length)
        return if (a < b) a..<b else null
    }
    val chapter = when (page) {
        is Page.Of -> page.chapter
        is Page.Text -> page.chapter
        Page.Preamble -> null
    }
    val list = rememberLazyListState()
    val spokenParagraph = paras.indices.firstOrNull { spokenIn(it) != null }
    // The sentence read aloud stays in sight
    LaunchedEffect(spokenParagraph) {
        if (spokenParagraph != null) list.animateScrollToItem(spokenParagraph + if (chapter != null) 1 else 0)
    }
    val mark = SpanStyle(background = scheme.secondaryContainer)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // The text can be selected and copied
        SelectionContainer {
            LazyColumn(
                Modifier.widthIn(max = READING_WIDTH).fillMaxWidth(),
                state = list,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The bar on top names the page; above the text, the chapter it belongs to (or, for the
                // transitional provisions, the chapter's own full name)
                if (chapter != null) {
                    item {
                        Text(
                            chapterName(res, chapter.num, chapter.title, lang),
                            style = if (page is Page.Text) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                            fontWeight = if (page is Page.Text) FontWeight.SemiBold else null,
                            color = if (page is Page.Text) scheme.onBackground else scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().semantics { heading() },
                        )
                    }
                }
                items(paras.size) { j ->
                    val p = paras[j]
                    val on = spokenIn(j)
                    val text = if (on == null) AnnotatedString(p) else buildAnnotatedString { append(p); addStyle(mark, on.first, on.last + 1) }
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = scheme.onBackground)
                }
                if (isLast) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            for (line in c.closing) {
                                Text(line, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                    color = scheme.onBackground, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                val notes = c.notes(page)
                if (notes.isNotEmpty()) {
                    item {
                        Text(
                            res.getString(R.string.notes),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
                        )
                    }
                    items(notes) { n -> Text(n, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
                }
                if (lang == "en") {
                    item {
                        Text(
                            res.getString(R.string.translation_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Constitution in one language as one document for reading aloud: page by page, each read with its short
 * name first ("Стаття 3."), so the listener knows where the voice is. A new chapter is where "until the end
 * of the chapter" stops.
 */
class ReadingText(private val c: Constitution, private val res: Resources, lang: String) {
    val id = "constitution-$lang"

    private fun head(i: Int) = pageShort(res, c.pages[i]) + "."

    /** How many characters come before the page's own text (its name and a line break). */
    fun headLength(i: Int) = head(i).length + 1

    val document = ReadAloud.Document(
        id = id,
        title = res.getString(R.string.quote_constitution),
        count = { c.pages.size },
        text = { i -> head(i) + "\n" + c.paragraphs(c.pages[i]).joinToString("\n") },
        isHeading = { i ->
            when (val p = c.pages[i]) {
                Page.Preamble, is Page.Text -> true
                is Page.Of -> p.article === p.chapter.articles.first()
            }
        },
    )
}
