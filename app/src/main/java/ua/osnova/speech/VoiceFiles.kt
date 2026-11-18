package ua.osnova.speech

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Osnova's two voices (the same as Hramota's and Pysanka's), downloaded once by the person rather than built into the app (they were most of
 * its size): the file is fetched in the browser and picked here, and only the exact pinned file is
 * accepted (size and SHA-256, checked while it is copied). The same files as in Pysanka.
 */
object VoiceFiles {
    enum class Voice(
        val file: String,
        /** The name the file has when downloaded. */
        val downloadName: String,
        val url: String,
        val bytes: Long,
        val sha256: String,
    ) {
        /** Tetiana (robinhad/ukrainian-tts v5.0.0, as ONNX): lies in Pysanka's v1.0.0 release, never deleted. */
        TETIANA(
            "uk-tetiana.onnx", "uk-tetiana.onnx",
            "https://github.com/marybezuhla/pysanka/releases/download/v1.0.0/uk-tetiana.onnx",
            151_507_141L, "6d76437e89207e973266b7b3911de8f2bab8cef25bbaaeeb40d34f5a0bae8df9",
        ),

        /** Cori (Piper's en_GB-cori-medium), unchanged, from a pinned revision. */
        CORI(
            "en-cori.onnx", "en_GB-cori-medium.onnx",
            "https://huggingface.co/rhasspy/piper-voices/resolve/293cad0539066f86e6bce3b9780c472cc9157489/en/en_GB/cori/medium/en_GB-cori-medium.onnx?download=true",
            63_531_379L, "1899f98e5fb8310154f3c2973f4b8a929ba7245e722b3d3a85680b833d95f10d",
        ),
    }

    /** While a file is being copied in: 0..1; and how the last copy ended. */
    data class Status(val progress: Float? = null, val error: Error? = null, val done: Int = 0)
    enum class Error { WRONG_FILE, NO_SPACE, FAILED, NOT_RESTORED }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statuses = Voice.entries.associateWith { MutableStateFlow(Status()) }
    fun status(v: Voice): StateFlow<Status> = statuses.getValue(v).asStateFlow()

    private fun dir(context: Context) = File(context.filesDir, "voices").apply { mkdirs() }

    /** A copy cut off by a closed app (up to 151 MB) is not left behind; called when the reading sheet shows. */
    fun cleanUp(context: Context) {
        if (statuses.values.any { it.value.progress != null }) return
        dir(context).listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
    }

    /** A voice deleted and not brought back goes for good; called only when the app starts afresh. */
    fun forgetDeleted(context: Context) {
        for (v in Voice.entries) if (File(dir(context), v.file + DELETED).delete()) changed(v)
    }

    /** The downloaded file, when it is there whole. */
    fun file(context: Context, v: Voice): File? = File(dir(context), v.file).takeIf { it.isFile && it.length() == v.bytes }

    /** Whether the voice can be used: downloaded whole. */
    fun has(context: Context, v: Voice): Boolean = file(context, v) != null

    /** The voice's model, mapped (not read into memory). */
    fun map(context: Context, v: Voice): ByteBuffer {
        val f = file(context, v) ?: throw IllegalStateException("${v.name} not downloaded")
        return RandomAccessFile(f, "r").use { r -> r.channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) } }
    }

    /** Copies the picked file in, if it is exactly the pinned one; [onDone] on the main thread. */
    fun import(context: Context, v: Voice, uri: Uri, onDone: () -> Unit = {}) {
        val flow = statuses.getValue(v)
        if (flow.value.progress != null) return
        flow.value = flow.value.copy(progress = 0f, error = null)
        val app = context.applicationContext
        scope.launch {
            val error = runCatching { copyIn(app, v, uri) { p -> flow.value = flow.value.copy(progress = p) } }.getOrElse { Error.FAILED }
            flow.value = Status(progress = null, error = error, done = flow.value.done + 1)
            launch(Dispatchers.Main) { onDone() }
        }
    }

    private fun copyIn(context: Context, v: Voice, uri: Uri, progress: (Float) -> Unit): Error? {
        val dir = dir(context)
        val tmp = File(dir, v.file + ".part")
        tmp.delete()
        if (dir.usableSpace < v.bytes + 50L * 1024 * 1024) return Error.NO_SPACE
        val md = MessageDigest.getInstance("SHA-256")
        var total = 0L
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return Error.FAILED
            input.use { i ->
                FileOutputStream(tmp).use { o ->
                    val buf = ByteArray(1 shl 20)
                    while (true) {
                        val n = i.read(buf)
                        if (n < 0) break
                        total += n
                        // Bigger than the voice: not it (stopped before filling the phone)
                        if (total > v.bytes) return Error.WRONG_FILE
                        md.update(buf, 0, n)
                        o.write(buf, 0, n)
                        progress(total.toFloat() / v.bytes)
                    }
                    o.fd.sync()
                }
            }
            val sum = md.digest().joinToString("") { "%02x".format(it) }
            if (total != v.bytes || sum != v.sha256) return Error.WRONG_FILE
            val target = File(dir, v.file)
            target.delete()
            // A rename in the same folder is all or nothing: a copy cut off halfway never counts as the voice
            if (!tmp.renameTo(target)) return Error.FAILED
            return null
        } finally {
            tmp.delete()
        }
    }

    /** Deleted, but kept aside until the next start so it can be brought back at once ([restore]). */
    fun delete(context: Context, v: Voice) {
        val f = File(dir(context), v.file)
        val aside = File(dir(context), v.file + DELETED)
        aside.delete()
        // A rename keeps it whole for "Undo"; if even that fails, the voice simply goes
        if (!f.renameTo(aside)) f.delete()
        changed(v)
    }

    fun deleted(context: Context, v: Voice): Boolean = File(dir(context), v.file + DELETED).isFile

    fun restore(context: Context, v: Voice) {
        val back = File(dir(context), v.file + DELETED).renameTo(File(dir(context), v.file))
        statuses.getValue(v).value = Status(error = if (back) null else Error.NOT_RESTORED, done = statuses.getValue(v).value.done + 1)
    }

    private fun changed(v: Voice) {
        statuses.getValue(v).value = Status(done = statuses.getValue(v).value.done + 1)
    }

    private const val DELETED = ".deleted"
}
