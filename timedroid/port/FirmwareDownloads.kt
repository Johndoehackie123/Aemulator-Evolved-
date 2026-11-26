package app.aemu.hub

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Download manager for catalog firmware.
 *
 * - resumable: partial files are kept as `<name>.part` and continued with a Range request
 * - survives the screen closing: runs on a process-wide scope, state lives in a StateFlow
 * - when the file is complete it is handed to [Listener.onReady] (the library screen imports it)
 */
object FirmwareDownloads {

    sealed interface State {
        data object Idle : State
        data class Running(val id: String, val received: Long, val total: Long, val bytesPerSec: Long) : State {
            val progress: Float get() = if (total > 0) (received.toDouble() / total).toFloat().coerceIn(0f, 1f) else -1f
        }
        data class Ready(val id: String, val file: File) : State
        data class Failed(val id: String, val message: String) : State
        data object Cancelled : State
    }

    fun interface Listener {
        /** Called on the main thread when a download has finished and the file is complete. */
        fun onReady(id: String, file: File)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private var job: kotlinx.coroutines.Job? = null
    private val listeners = linkedMapOf<String, Listener>()

    /** Where partial and finished firmware files live. Wiped by [clearFinished] / [cancel] policy. */
    fun dir(ctx: Context): File = File(ctx.filesDir, "hub").apply { mkdirs() }

    fun isDownloading(id: String): Boolean = (_state.value as? State.Running)?.id == id

    /** A finished file for this entry, if one is on disk. */
    fun finishedFile(ctx: Context, entry: FirmwareCatalog.Entry): File? =
        File(dir(ctx), entry.fileName).takeIf { it.isFile && it.length() == entry.sizeBytes && entry.sizeBytes > 0 }

    fun partialBytes(ctx: Context, entry: FirmwareCatalog.Entry): Long =
        File(dir(ctx), entry.fileName + ".part").takeIf { it.isFile }?.length() ?: 0L

    fun listen(key: String, l: Listener) { listeners[key] = l }
    fun unlisten(key: String) { listeners.remove(key) }

    @Synchronized
    fun start(ctx: Context, entry: FirmwareCatalog.Entry) {
        val current = _state.value
        // already running this entry: nothing to do
        if (current is State.Running && current.id == entry.id) return
        // one transfer at a time: cancel whatever is running
        if (current is State.Running) cancel()
        finishedFile(ctx, entry)?.let {
            _state.value = State.Ready(entry.id, it)
            return
        }
        val app = ctx.applicationContext
        _state.value = State.Running(entry.id, partialBytes(app, entry), entry.sizeBytes, 0)
        job = scope.launch {
            try {
                val file = download(app, entry)
                _state.value = State.Ready(entry.id, file)
                withContext(Dispatchers.Main) { listeners.values.forEach { it.onReady(entry.id, file) } }
            } catch (e: CancellationException) {
                _state.value = State.Cancelled
            } catch (e: Throwable) {
                _state.value = State.Failed(entry.id, e.message ?: e.toString())
            }
        }
    }

    @Synchronized
    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State.Cancelled
    }

    /** Removes a finished or partial file (e.g. after the user imported it, or to free space). */
    fun discard(ctx: Context, entry: FirmwareCatalog.Entry) {
        File(dir(ctx), entry.fileName).delete()
        File(dir(ctx), entry.fileName + ".part").delete()
        if ((_state.value as? State.Ready)?.id == entry.id) _state.value = State.Idle
    }

    private suspend fun download(ctx: Context, entry: FirmwareCatalog.Entry): File {
        val out = File(dir(ctx), entry.fileName)
        val part = File(dir(ctx), entry.fileName + ".part")
        var attempt = 0
        while (true) {
            attempt++
            try {
                fetch(entry, part)
                if (entry.sizeBytes > 0 && part.length() != entry.sizeBytes) {
                    // a server that ignored our Range restarted from zero: only an exact match counts
                    if (part.length() > entry.sizeBytes) { part.delete(); continue }
                    throw java.io.IOException("incomplete: ${part.length()} of ${entry.sizeBytes} bytes")
                }
                if (!part.renameTo(out)) throw java.io.IOException("cannot rename ${part.name}")
                return out
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (attempt >= 3) throw e
                kotlinx.coroutines.delay(2_000L * attempt)
            }
        }
    }

    private suspend fun fetch(entry: FirmwareCatalog.Entry, part: File) {
        val have = part.length()
        val conn = (URL(entry.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "TimeDroid/1.0 (firmware hub)")
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        val totalHeader = conn.contentLengthLong
        val total = if (conn.responseCode == HttpURLConnection.HTTP_PARTIAL && have > 0)
            have + totalHeader.coerceAtLeast(0)
        else if (entry.sizeBytes > 0) entry.sizeBytes else totalHeader
        if (conn.responseCode == HttpURLConnection.HTTP_OK && have > 0) {
            // server sent the whole file: start over
            part.delete()
        }
        val resume = part.length()
        var received = resume
        val started = System.currentTimeMillis()
        var lastReport = 0L
        var windowStart = started
        var windowBytes = resume
        var speed = 0L
        conn.inputStream.use { input ->
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(resume)
                val buf = ByteArray(256 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    raf.write(buf, 0, n)
                    received += n
                    val now = System.currentTimeMillis()
                    if (now - windowStart >= 1_000) {
                        speed = (received - windowBytes) * 1000 / (now - windowStart)
                        windowBytes = received
                        windowStart = now
                    }
                    if (now - lastReport >= 250) {
                        lastReport = now
                        _state.update { s ->
                            if (s is State.Running && s.id == entry.id)
                                s.copy(received = received, total = if (total > 0) total else entry.sizeBytes, bytesPerSec = speed)
                            else s
                        }
                    }
                }
            }
        }
        val finalLen = part.length()
        if (total > 0 && finalLen != total) throw java.io.IOException("incomplete: $finalLen of $total bytes")
    }

    // ---- small helpers shared with the UI ----

    fun fmtBytes(b: Long): String {
        if (b <= 0) return "?"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = b.toDouble(); var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return if (i == 0) "${b} B" else String.format(java.util.Locale.US, "%.1f %s", v, units[i])
    }

    fun fmtSpeed(bps: Long): String = if (bps <= 0) "" else fmtBytes(bps) + "/s"

    fun fmtEta(remaining: Long, bps: Long): String {
        if (bps <= 0 || remaining <= 0) return ""
        val s = remaining / bps
        return when {
            s < 60 -> "${s}s"
            s < 3600 -> "${s / 60} min ${s % 60}s"
            else -> "${s / 3600} h ${(s % 3600) / 60} min"
        }
    }

    fun persist(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }

    fun read(ctx: Context, key: String): String? = prefs(ctx).getString(key, null)

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("hub", Context.MODE_PRIVATE)

    /** Download history for the "downloaded" badge: id -> finished epoch millis. */
    fun markDone(ctx: Context, id: String) = persist(ctx, "done_$id", System.currentTimeMillis().toString())
    fun doneAt(ctx: Context, id: String): Long? = read(ctx, "done_$id")?.toLongOrNull()
}

private suspend fun <T> withContext(dispatcher: kotlinx.coroutines.CoroutineDispatcher, block: suspend () -> T): T =
    kotlinx.coroutines.withContext(dispatcher) { block() }
