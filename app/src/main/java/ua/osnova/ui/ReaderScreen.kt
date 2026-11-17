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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page
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
) {
    val scheme = MaterialTheme.colorScheme
    val res = textResources(lang)
    val pager = rememberPagerState(initialPage = c.indexOf(start) ?: 0) { c.pages.size }
    val scope = rememberCoroutineScope()
    // The page settled on is the place to come back to
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(pager, c) {
        snapshotFlow { pager.settledPage }.collect { shown(c.pages[it].key) }
    }
    val current = c.pages[pager.currentPage]
    val marked = current.key in bookmarks
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            Bar(pageShort(res, current), onBack) {
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
                PageText(c, res, lang, c.pages[i], isLast = i == c.pages.lastIndex)
            }
            Row(
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
private fun PageText(c: Constitution, res: Resources, lang: String, page: Page, isLast: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // The text can be selected and copied
        SelectionContainer {
            LazyColumn(
                Modifier.widthIn(max = READING_WIDTH).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The bar on top names the page; above the text, the chapter it belongs to (or, for the
                // transitional provisions, the chapter's own full name)
                val chapter = when (page) {
                    is Page.Of -> page.chapter
                    is Page.Text -> page.chapter
                    Page.Preamble -> null
                }
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
                items(c.paragraphs(page)) { p ->
                    Text(p, style = MaterialTheme.typography.bodyLarge, color = scheme.onBackground)
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
