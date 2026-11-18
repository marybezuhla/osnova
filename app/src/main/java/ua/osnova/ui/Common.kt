package ua.osnova.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.osnova.R
import ua.osnova.text.Page
import ua.osnova.text.Titles
import ua.osnova.ui.theme.Ornament
import java.util.Locale

/** The widest a column of text gets on a tablet or an unfolded phone: lines stay easy to follow. */
val READING_WIDTH = 680.dp

/**
 * The app's strings in the language of the text, which can differ from the app's own language
 * (an English reader of the Ukrainian text sees "Стаття 3", not "Article 3").
 */
@Composable
fun textResources(lang: String): Resources {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(lang, config) {
        val c = Configuration(config).apply { setLocale(Locale.forLanguageTag(lang)) }
        context.createConfigurationContext(c).resources
    }
}

/**
 * Text marked with its language, so TalkBack reads it with that language's voice whatever the app's own
 * (the mark must be on the text itself: the style's language only shapes the letters).
 */
fun inLanguage(text: String, lang: String, marks: List<IntRange> = emptyList(), mark: SpanStyle? = null): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(localeList = LocaleList(lang))) { append(text) }
        if (mark != null) for (m in marks) addStyle(mark, m.first, m.last + 1)
    }

/** A chapter's name: "I. Загальні засади", "XV. Transitional Provisions". */
fun chapterName(res: Resources, num: String, title: String, lang: String): String =
    res.getString(R.string.chapter_titled, num, Titles.display(title, lang))

/** A page's name: "Преамбула", "Article 3", "XV. Перехідні положення". */
fun pageTitle(res: Resources, page: Page, lang: String): String = when (page) {
    Page.Preamble -> res.getString(R.string.preamble)
    is Page.Of -> res.getString(R.string.article, page.article.num)
    is Page.Text -> chapterName(res, page.chapter.num, page.chapter.title, lang)
}

/** The bar on top of every screen: the title in the middle, back on the left, the motif under it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.background(scheme.background)) {
        CenterAlignedTopAppBar(
            title = {
                Text(title, color = scheme.primary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = scheme.background),
        )
        Ornament(Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
    }
}
