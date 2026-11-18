package ua.osnova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ua.osnova.R
import ua.osnova.speech.ReadAloud

/** Speeds offered: a little slower to a little faster than a calm narrator. */
internal val READ_SPEEDS = listOf(0.8f, 0.9f, 1f, 1.2f, 1.5f)

/** «1,0» in Ukrainian, «1.0» in English: the decimal mark of the app's language. */
internal fun speedLabel(s: Float) = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(s.toDouble())

/**
 * The player at the bottom, within the thumb's reach: the sentence back and forward, pause, speed and the sleep
 * timer at hand; the voice and closing below them (the bookmark is on top of the page).
 */
@Composable
internal fun ReadAloudBar(state: ReadAloud.State, onSettings: () -> Unit) {
    // A notice (no voice for a language) is said for a few seconds
    LaunchedEffect(state.notice) { if (state.notice != null) { delay(6000); ReadAloud.clearNotice() } }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val notice = when (state.notice) {
                ReadAloud.NOTICE_NO_VOICE -> R.string.read_no_voice
                ReadAloud.NOTICE_NO_LANGUAGE -> R.string.read_no_language
                ReadAloud.NOTICE_NO_SOUND -> R.string.read_no_sound
                ReadAloud.NOTICE_NOT_DOWNLOADED -> R.string.read_not_downloaded
                ReadAloud.NOTICE_NOT_DOWNLOADED_NO_PHONE -> R.string.read_not_downloaded_no_phone
                ReadAloud.NOTICE_OWN_FAILED -> R.string.read_own_failed
                else -> if (state.preparing) R.string.read_preparing else null
            }
            if (notice != null) Text(
                stringResource(notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val speedSays = stringResource(R.string.read_speed, speedLabel(state.speed))
                TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = speedSays }) { Text(speedLabel(state.speed) + "×", maxLines = 1) }
                IconButton(onClick = { ReadAloud.skip(-1) }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_previous), contentDescription = stringResource(R.string.read_previous))
                }
                FilledIconButton(onClick = { ReadAloud.toggle() }, modifier = Modifier.size(64.dp)) {
                    Icon(painterResource(if (state.playing) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = stringResource(if (state.playing) R.string.read_pause else R.string.read_play), modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { ReadAloud.skip(1) }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_next), contentDescription = stringResource(R.string.read_next))
                }
                val left = state.sleepAt?.takeIf { it != ReadAloud.SLEEP_CHAPTER }?.let { ((it - android.os.SystemClock.uptimeMillis()) / 60_000 + 1).coerceAtLeast(1).toInt() }
                val sleepSays = when {
                    state.sleepAt == null -> stringResource(R.string.read_sleep)
                    left == null -> stringResource(R.string.read_sleep_left, stringResource(R.string.read_sleep_chapter))
                    else -> stringResource(R.string.read_sleep_left, stringResource(R.string.read_sleep_minutes, left))
                }
                TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = sleepSays }) {
                    Text(when { state.sleepAt == null -> "☾"; left == null -> "☾ §"; else -> "☾ $left" }, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Short on the button (it fits with large type too); said in full to TalkBack
                val settingsSays = stringResource(R.string.read_settings)
                TextButton(onClick = onSettings, modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = settingsSays }) {
                    Text(stringResource(R.string.read_voice), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { ReadAloud.stop() }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.read_stop))
                }
            }
        }
    }
}

/** The voice, the speed and the sleep timer. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ReadAloudSheet(state: ReadAloud.State, onVoice: (String) -> Unit, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { ReadAloudSettings(state, onVoice, onSpeed) }
}

/** The sheet's content, apart from the sheet, so the screen tests can draw it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadAloudSettings(state: ReadAloud.State, onVoice: (String) -> Unit, onSpeed: (Float) -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.read_aloud), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.read_voice).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() })
        Column(Modifier.selectableGroup()) {
            for ((id, title, hint) in listOf(
                Triple(ReadAloud.OWN, R.string.read_voice_own, R.string.read_voice_own_hint),
                Triple(ReadAloud.PHONE, R.string.read_voice_phone, R.string.read_voice_phone_hint),
            )) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(state.voice == id, role = Role.RadioButton) { onVoice(id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.voice == id, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // The voices, downloaded once (not in the app)
        if (state.voice != ReadAloud.PHONE) VoiceDownloads()
        Text(stringResource(R.string.read_speed, speedLabel(state.speed)).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in READ_SPEEDS) {
                // "Швидкість 1,5", not "1,5 множення"
                val says = stringResource(R.string.read_speed, speedLabel(s))
                FilterChip(selected = state.speed == s, onClick = { onSpeed(s) }, label = { Text(speedLabel(s) + "×") },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = says })
            }
        }
        Text(stringResource(R.string.read_sleep).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val on = state.sleepAt
            FilterChip(selected = on == null, onClick = { ReadAloud.sleep(0) }, label = { Text(stringResource(R.string.read_sleep_off)) }, modifier = Modifier.heightIn(min = 48.dp))
            for (m in listOf(15L, 30L, 60L)) FilterChip(selected = state.sleepMinutes == m && on != null && on != ReadAloud.SLEEP_CHAPTER, onClick = { ReadAloud.sleep(m) }, label = { Text(stringResource(R.string.read_sleep_minutes, m.toInt())) }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(selected = on == ReadAloud.SLEEP_CHAPTER, onClick = { ReadAloud.sleep(ReadAloud.SLEEP_CHAPTER) }, label = { Text(stringResource(R.string.read_sleep_chapter)) }, modifier = Modifier.heightIn(min = 48.dp))
        }
        Spacer(Modifier.size(4.dp))
    }
}
