package ua.osnova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page

/** One chapter: its name, then every article with its first lines; a tap opens the article. */
@Composable
fun ChapterScreen(c: Constitution, lang: String, num: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val res = textResources(lang)
    val chapter = c.chapters.firstOrNull { it.num == num } ?: return
    Scaffold(containerColor = scheme.background, topBar = { Bar(res.getString(R.string.chapter, chapter.num), onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = READING_WIDTH).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        chapterName(res, chapter.num, chapter.title, lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).semantics { heading() },
                    )
                }
                items(chapter.articles, key = { it.num }) { article ->
                    Tile({ onOpen(Page.Of(article, chapter).key) }) {
                        Column {
                            Text(res.getString(R.string.article, article.num), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                article.parts.first(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
