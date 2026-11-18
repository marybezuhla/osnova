package ua.osnova.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ua.osnova.R
import ua.osnova.speech.ReadAloud
import ua.osnova.speech.VoiceFiles

/**
 * Osnova's two voices (the same as Hramota's and Pysanka's) in the reading sheet: whether each is downloaded; download (in the browser, at the
 * pinned link), pick the downloaded file (only the exact one is accepted), delete.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VoiceDownloads() {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { VoiceFiles.cleanUp(context) }
    Text(stringResource(R.string.read_voices).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.read_voices_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    VoiceRow(VoiceFiles.Voice.TETIANA, R.string.read_voice_uk)
    VoiceRow(VoiceFiles.Voice.CORI, R.string.read_voice_en)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceRow(voice: VoiceFiles.Voice, title: Int) {
    val context = LocalContext.current
    val status by VoiceFiles.status(voice).collectAsState()
    // Read again after each copy or deletion
    val present = remember(status.done) { VoiceFiles.file(context, voice) != null }
    val deleted = remember(status.done) { VoiceFiles.deleted(context, voice) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) VoiceFiles.import(context, voice, uri) { ReadAloud.release() }
    }
    val name = stringResource(title)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        val progress = status.progress
        when {
            progress != null -> {
                Text(stringResource(R.string.read_voice_copying, (progress * 100).toInt()), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            present -> Text(stringResource(R.string.read_voice_ready), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Said by TalkBack too, with "Undo" right below
            deleted -> Text(stringResource(R.string.read_voice_deleted), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            else -> Text(stringResource(R.string.read_voice_missing, voice.downloadName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status.error?.let {
            Text(
                stringResource(
                    when (it) {
                        VoiceFiles.Error.WRONG_FILE -> R.string.read_voice_wrong
                        VoiceFiles.Error.NO_SPACE -> R.string.read_voice_no_space
                        VoiceFiles.Error.FAILED -> R.string.read_voice_failed
                        VoiceFiles.Error.INCOMPLETE -> R.string.read_voice_incomplete
                        VoiceFiles.Error.NOT_RESTORED -> R.string.read_voice_not_restored
                    },
                ),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
        }
        if (progress == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (present) {
                    val del = stringResource(R.string.label_for, stringResource(R.string.read_voice_delete), name)
                    // Reading stops (the voice's file is in use); "Undo" brings the voice back at once
                    TextButton(
                        onClick = { ReadAloud.stop(); ReadAloud.release(); VoiceFiles.delete(context, voice) },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = del },
                    ) { Text(stringResource(R.string.read_voice_delete)) }
                } else if (deleted) {
                    val undo = stringResource(R.string.label_for, stringResource(R.string.undo), name)
                    OutlinedButton(onClick = { VoiceFiles.restore(context, voice) }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = undo }) {
                        Text(stringResource(R.string.undo))
                    }
                } else {
                    val download = stringResource(R.string.read_voice_download)
                    val downloadSays = stringResource(R.string.label_for, download, name)
                    val pickSays = stringResource(R.string.label_for, stringResource(R.string.read_voice_pick), name)
                    OutlinedButton(onClick = {
                        // No browser on the phone: nothing happens rather than a crash
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(voice.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = downloadSays }) { Text(download) }
                    OutlinedButton(onClick = { runCatching { pick.launch(arrayOf("*/*")) } }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = pickSays }) {
                        Text(stringResource(R.string.read_voice_pick))
                    }
                }
            }
        }
    }
}
