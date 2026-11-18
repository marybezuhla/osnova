package ua.osnova.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.osnova.R
import ua.osnova.text.Constitution
import ua.osnova.text.Page
import ua.osnova.text.Quote

/** "Share a quote" over the reader. Its content is drawn by [ShareContent], so the screen tests can show it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(c: Constitution, page: Page, lang: String, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        ShareContent(c, page, lang)
    }
}

/**
 * The whole page or chosen parts of it, the quote as it will be sent, and two ways to send it: copy, or
 * Android's own "Share" (messengers, mail, social networks). Nothing is sent by Osnova itself.
 */
@Composable
fun ShareContent(c: Constitution, page: Page, lang: String) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val res = textResources(lang)
    val pieces = remember(c, page) { Quote.pieces(c, page) }
    val paras = c.paragraphs(page)
    var choosing by rememberSaveable(page.key) { mutableStateOf(false) }
    // Chosen pieces by their position; all of them at first, so a choice is made by taking some away
    var chosen by rememberSaveable(page.key) { mutableStateOf(pieces.indices.toList()) }
    val picked = if (choosing) pieces.filterIndexed { i, _ -> i in chosen } else pieces
    val source = picked.takeIf { it.isNotEmpty() }?.let { Quote.source(page, if (it.size == pieces.size) null else it, lang) }
    val words = Quote.words(paras, picked)
    val pieceName = when (page) {
        Page.Preamble -> R.string.share_paragraph
        is Page.Of -> R.string.share_part
        is Page.Text -> R.string.share_point
    }
    val open = res.getString(R.string.quote_open)
    val close = res.getString(R.string.quote_close)
    val quote = source?.let { Quote.text(words, open, close, it) }
    var copied by remember(quote) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.share), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            color = scheme.onBackground, modifier = Modifier.semantics { heading() })
        if (pieces.size > 1) {
            ua.osnova.ui.theme.Segments(
                options = listOf(false to R.string.share_all, true to R.string.share_choose),
                chosen = choosing,
                onChoose = { choosing = it },
            )
        }
        if (choosing) {
            for ((i, piece) in pieces.withIndex()) {
                val on = i in chosen
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(on, role = Role.Checkbox) { chosen = if (it) (chosen + i).sorted() else chosen - i },
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.padding(top = 2.dp, end = 12.dp))
                    Column {
                        // Its number, as it will be cited: "Частина 2"
                        Text(res.getString(pieceName, piece.number), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                        Text(
                            inLanguage(paras[piece.paragraphs.first], lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        // The quote as it will be sent: a long one is cut short here, its source always shows
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (source == null) {
                    Text(stringResource(R.string.share_none), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                } else {
                    var cut by remember(words) { mutableStateOf(false) }
                    Text(inLanguage(Quote.body(words, open, close), lang), style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface, maxLines = 10, overflow = TextOverflow.Ellipsis,
                        onTextLayout = { cut = it.hasVisualOverflow })
                    // Said by TalkBack as parts are chosen
                    Text(inLanguage(Quote.SOURCE_MARK + source, lang), style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (cut) {
                        Text(stringResource(R.string.share_sent_whole), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    quote ?: return@OutlinedButton
                    // A plain quote from a public text: nothing private in the clipboard
                    runCatching {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText(Quote.CLIP_LABEL, quote))
                        copied = true
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (copied) R.string.share_copied else R.string.share_copy),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Button(
                onClick = {
                    quote ?: return@Button
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, quote)
                    // A phone without any app to share to: nothing happens rather than a crash
                    runCatching { context.startActivity(Intent.createChooser(send, null)) }
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.share_send)) }
        }
    }
}
