package ua.osnova.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.text.Search
import ua.osnova.ui.theme.Segments

/**
 * The first screen: a search field, the place last read, and the contents or the bookmarks. While
 * something is typed, the search results take the place of the contents.
 */
@Composable
fun HomeScreen(
    c: Constitution,
    lang: String,
    bookmarks: List<String>,
    last: String?,
    query: String,
    onQuery: (String) -> Unit,
    showBookmarks: Boolean,
    onShowBookmarks: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onChapter: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val res = textResources(lang)
    val hits = remember(c, query) { if (query.isBlank()) emptyList() else Search.find(c, query) }
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            Bar(stringResource(R.string.app_name), onBack = null) {
                IconButton(onClick = onSettings) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = READING_WIDTH).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SearchField(query, onQuery) }
                if (query.isNotBlank()) {
                    results(c, res, lang, query, hits, onOpen)
                } else {
                    val lastPage = last?.let(c::indexOf)?.let { c.pages[it] }
                    if (lastPage != null) {
                        item { ContinueCard(pageTitle(res, lastPage, lang)) { onOpen(lastPage.key) } }
                    }
                    item {
                        Segments(
                            options = listOf(false to R.string.tab_contents, true to R.string.tab_bookmarks),
                            chosen = showBookmarks,
                            onChoose = onShowBookmarks,
                        )
                    }
                    if (showBookmarks) bookmarkList(c, res, lang, bookmarks, onOpen, onRemoveBookmark) else contents(c, res, lang, onOpen, onChapter)
                    item {
                        Text(
                            c.edition,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        // No corrections: the words of the law as typed
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surface,
            unfocusedContainerColor = scheme.surface,
            // The field's edge seen clearly (3:1 against the background)
            unfocusedBorderColor = scheme.outline,
        ),
    )
}

/** A card in the list: the theme's card colour, a thin frame, the whole card can be tapped. */
@Composable
internal fun Tile(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier.heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) { content() }
    }
}

@Composable
private fun ContinueCard(title: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = scheme.primary, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimary)
        }
    }
}

private fun LazyListScope.contents(c: Constitution, res: Resources, lang: String, onOpen: (String) -> Unit, onChapter: (String) -> Unit) {
    item {
        Tile({ onOpen(Page.Preamble.key) }) { Text(res.getString(R.string.preamble), style = MaterialTheme.typography.bodyLarge) }
    }
    for (chapter in c.chapters) {
        item(key = chapter.num) {
            Tile({ if (chapter.articles.isEmpty()) onOpen(Page.Text(chapter).key) else onChapter(chapter.num) }) {
                Column {
                    Text(chapterName(res, chapter.num, chapter.title, lang), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (chapter.articles.isNotEmpty()) {
                        Text(
                            res.getString(R.string.articles_range, chapter.articles.first().num, chapter.articles.last().num),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        // Chapter VII (the prosecutor's office) was taken out in 2016; the numbering keeps its gap
        if (chapter.num == "VI" && c.chapters.none { it.num == "VII" }) {
            item {
                Text(
                    res.getString(R.string.chapter_vii_excluded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private fun LazyListScope.bookmarkList(
    c: Constitution,
    res: Resources,
    lang: String,
    bookmarks: List<String>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val pages = bookmarks.mapNotNull { k -> c.indexOf(k)?.let { c.pages[it] } }
    if (pages.isEmpty()) {
        item {
            Text(
                stringResource(R.string.bookmarks_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }
    }
    items(pages, key = { it.key }) { page ->
        Tile({ onOpen(page.key) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pageTitle(res, page, lang), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        c.paragraphs(page).firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onRemove(page.key) }) {
                    Icon(
                        painterResource(R.drawable.ic_bookmark),
                        contentDescription = stringResource(R.string.bookmark_remove),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.results(
    c: Constitution,
    res: Resources,
    lang: String,
    query: String,
    hits: List<Search.Hit>,
    onOpen: (String) -> Unit,
) {
    // A number on its own is an article to go to, shown first
    val jump = Search.articleNumber(query)?.let(c::article)
    if (jump != null) {
        item {
            Tile({ onOpen(jump.key) }) {
                Column {
                    Text(stringResource(R.string.search_go_to), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(pageTitle(res, jump, lang), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    // How many were found (or that none were), said by TalkBack as the query changes
    item {
        Text(
            if (hits.isEmpty() && jump == null) stringResource(R.string.search_none) else stringResource(R.string.search_found, hits.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = if (hits.isEmpty() && jump == null) 24.dp else 0.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    items(hits) { hit ->
        val page = c.pages[hit.page]
        val mark = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer, fontWeight = FontWeight.SemiBold)
        Tile({ onOpen(page.key) }) {
            Column {
                Text(pageTitle(res, page, lang), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(marked(hit.excerpt, hit.marks, mark), style = MaterialTheme.typography.bodyMedium.copy(localeList = LocaleList(lang)))
            }
        }
    }
}

/** The excerpt with the words found shaded. */
private fun marked(text: String, marks: List<IntRange>, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    for (m in marks) addStyle(style, m.first, m.last + 1)
}
