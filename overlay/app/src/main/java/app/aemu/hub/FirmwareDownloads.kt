package app.aemu.hub

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException as CoroutineCancellation

/**
 * One-tap, resumable firmware downloads with published-checksum verification.
 *
 * Downloads are serial (one at a time) so a slow phone does not end up with three 1 GB
 * partial files. State is process-wide, so closing the Hub screen does not stop a transfer.
 * A completed file is only marked READY after its digest matches the catalog fingerprint;
 * a mismatch deletes the partial file and is reported as an error, never imported.
 */
object FirmwareDownloads {

    enum class Status { IDLE, QUEUED, DOWNLOADING, VERIFYING, READY, FAILED, CANCELLED }

    enum class Verdict { PENDING, VERIFIED, UNVERIFIED, MISMATCH }

    data class EntryState(
        val id: String,
        val status: Status = Status.IDLE,
        val received: Long = 0L,
        val total: Long = 0L,
        val bytesPerSec: Long = 0L,
        val message: String? = null,
        val verdict: Verdict = Verdict.PENDING,
    ) {
        val progress: Float get() = if (total > 0) (received.toDouble() / total).toFloat().coerceIn(0f, 1f) else -1f
    }

    private class ChecksumMismatch(message: String) : IOException(message)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<Map<String, EntryState>>(emptyMap())
    val entries: StateFlow<Map<String, EntryState>> = _entries
    private val _queue = MutableStateFlow<List<String>>(emptyList())
    val queue: StateFlow<List<String>> = _queue
    private var worker: Job? = null

    fun dir(ctx: Context): File = File(ctx.applicationContext.filesDir, "hub").apply { mkdirs() }

    private fun outFile(ctx: Context, entry: FirmwareCatalog.Entry) = File(dir(ctx), entry.fileName)
    private fun partFile(ctx: Context, entry: FirmwareCatalog.Entry) = File(dir(ctx), entry.fileName + ".part")
    private fun markerFile(ctx: Context, entry: FirmwareCatalog.Entry) = File(dir(ctx), entry.fileName + ".verified")

    fun state(id: String): EntryState = _entries.value[id] ?: EntryState(id)

    fun isDownloading(id: String): Boolean =
        state(id).status.let { it == Status.DOWNLOADING || it == Status.VERIFYING }

    fun queued(id: String): Boolean = id in _queue.value

    fun partialBytes(ctx: Context, entry: FirmwareCatalog.Entry): Long =
        partFile(ctx, entry).takeIf { it.isFile }?.length() ?: 0L

    /** A file is usable only when it has the advertised size and a verification marker. */
    fun finishedFile(ctx: Context, entry: FirmwareCatalog.Entry): File? {
        val f = outFile(ctx, entry)
        if (!f.isFile) return null
        if (entry.sizeBytes > 0 && f.length() != entry.sizeBytes) return null
        if (!markerFile(ctx, entry).isFile) return null
        return f
    }

    fun verdict(ctx: Context, entry: FirmwareCatalog.Entry): Verdict {
        val marker = markerFile(ctx, entry)
        if (!marker.isFile) return Verdict.PENDING
        return when (marker.readText().trim()) {
            "UNVERIFIED" -> Verdict.UNVERIFIED
            else -> Verdict.VERIFIED
        }
    }

    @Synchronized
    fun enqueue(ctx: Context, entry: FirmwareCatalog.Entry) {
        if (isDownloading(entry.id) || queued(entry.id)) return
        finishedFile(ctx, entry)?.let {
            set(entry.id) { copy(status = Status.READY, verdict = verdict(ctx, entry), message = null) }
            return
        }
        _queue.update { it + entry.id }
        set(entry.id) { copy(status = Status.QUEUED, received = partialBytes(ctx, entry), total = entry.sizeBytes, message = null) }
        if (worker?.isActive != true) {
            worker = scope.launch { drain(ctx.applicationContext) }
        }
    }

    @Synchronized
    fun cancel(id: String) {
        _queue.update { it - id }
        if (state(id).status.let { it == Status.QUEUED || it == Status.IDLE }) {
            set(id) { copy(status = Status.CANCELLED, message = "Cancelled") }
            return
        }
        set(id) { copy(status = Status.CANCELLED, message = "Cancelled after current chunk") }
        worker?.cancel()
        worker = null
    }

    /** Deletes both the downloaded file and its partial, e.g. to free space or after import. */
    fun discard(ctx: Context, entry: FirmwareCatalog.Entry) {
        outFile(ctx, entry).delete()
        partFile(ctx, entry).delete()
        markerFile(ctx, entry).delete()
        set(entry.id) { EntryState(entry.id, Status.IDLE) }
    }

    /** Re-checks a finished file against the published fingerprint. Safe to call from a background thread. */
    fun verify(ctx: Context, entry: FirmwareCatalog.Entry) {
        val spec = entry.checksum
        val algorithm = Checksums.algorithm(spec)
        val file = outFile(ctx, entry)
        if (spec == null || algorithm == null || !file.isFile) {
            if (file.isFile) markerFile(ctx, entry).writeText("UNVERIFIED")
            set(entry.id) { copy(status = Status.READY, verdict = Verdict.UNVERIFIED,
                message = entry.verificationNote ?: "No published checksum available") }
            return
        }
        try {
            set(entry.id) { copy(status = Status.VERIFYING, message = null) }
            val actual = Checksums.digestOf(file, algorithm)
            if (!Checksums.matches(spec.hex, actual)) {
                markerFile(ctx, entry).delete()
                set(entry.id) { copy(status = Status.FAILED, verdict = Verdict.MISMATCH,
                    message = "Checksum mismatch: expected ${spec.hex}, got $actual") }
                return
            }
            markerFile(ctx, entry).writeText("$algorithm:${spec.hex.lowercase()}")
            set(entry.id) { copy(status = Status.READY, verdict = Verdict.VERIFIED, message = null) }
        } catch (e: Throwable) {
            set(entry.id) { copy(status = Status.FAILED, message = e.message ?: e.toString()) }
        }
    }

    private suspend fun drain(ctx: Context) {
        while (true) {
            val id = _queue.value.firstOrNull() ?: break
            val entry = FirmwareCatalog.find(id) ?: run { _queue.update { it - id }; continue }
            try {
                fetchAndVerify(ctx, entry)
            } catch (e: CancellationException) {
                break
            } catch (e: Throwable) {
                set(id) { copy(status = Status.FAILED, message = e.message ?: e.toString()) }
            }
            _queue.update { it - id }
        }
    }

    private suspend fun fetchAndVerify(ctx: Context, entry: FirmwareCatalog.Entry) {
        val part = partFile(ctx, entry)
        val out = outFile(ctx, entry)
        val marker = markerFile(ctx, entry)
        val spec = entry.checksum
        val algorithm = Checksums.algorithm(spec)
        var attempt = 0
        while (true) {
            attempt++
            var digest: Checksums.Digest? = if (spec != null && algorithm != null) Checksums.Digest(algorithm) else null
            var keepPartial = false
            try {
                requireAllowedNetwork(ctx)
                var have = part.takeIf { it.isFile }?.length() ?: 0L
                val conn = (URL(entry.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "AEmulator-Evolved-FirmwareHub/1.0 (Android)")
                    if (have > 0) setRequestProperty("Range", "bytes=$have-")
                }
                val code = conn.responseCode
                if (code !in 200..299) throw IOException("HTTP $code")
                val partialResponse = code == HttpURLConnection.HTTP_PARTIAL
                val contentLength = conn.contentLengthLong
                val total = if (partialResponse && have > 0 && contentLength > 0) have + contentLength
                            else if (entry.sizeBytes > 0) entry.sizeBytes else contentLength
                if (!partialResponse && have > 0) {
                    // Server ignored Range: start again from zero.
                    part.delete()
                    have = 0
                    digest = if (spec != null && algorithm != null) Checksums.Digest(algorithm) else null
                }
                if (have > 0 && digest != null) {
                    // Include bytes from a previous run in the digest before appending new ones.
                    part.inputStream().use { ins ->
                        val buf = ByteArray(128 * 1024)
                        while (true) { val n = ins.read(buf); if (n < 0) break; digest!!.update(buf, n) }
                    }
                    keepPartial = true
                }

                set(entry.id) { copy(status = Status.DOWNLOADING, received = have, total = total, message = null) }
                val started = System.currentTimeMillis()
                var windowStart = started
                var windowBytes = have
                var lastReport = 0L
                var speed = 0L
                var received = have

                conn.inputStream.use { input ->
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(have)
                        val buf = ByteArray(256 * 1024)
                        while (true) {
                            if (!coroutineContextIsActive()) throw CancellationException()
                            val n = input.read(buf)
                            if (n < 0) break
                            raf.write(buf, 0, n)
                            digest?.update(buf, n)
                            received += n
                            val now = System.currentTimeMillis()
                            if (now - windowStart >= 1000) {
                                speed = (received - windowBytes) * 1000 / (now - windowStart)
                                windowBytes = received
                                windowStart = now
                            }
                            if (now - lastReport >= 250) {
                                lastReport = now
                                set(entry.id) {
                                    copy(received = received, total = total, bytesPerSec = speed,
                                        status = Status.DOWNLOADING)
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                keepPartial = false

                val len = part.length()
                if (entry.sizeBytes > 0 && len != entry.sizeBytes)
                    throw IOException("Incomplete download: $len of ${entry.sizeBytes} bytes")

                if (spec != null && algorithm != null && digest != null) {
                    val actual = digest.hex()
                    if (!Checksums.matches(spec.hex, actual)) {
                        part.delete()
                        throw ChecksumMismatch("Checksum mismatch — expected ${spec.hex}, got $actual. File discarded.")
                    }
                }

                if (!part.renameTo(out)) throw IOException("Cannot move completed download into place")
                marker.writeText(if (spec != null && algorithm != null) "$algorithm:${spec.hex.lowercase()}" else "UNVERIFIED")
                set(entry.id) {
                    copy(status = Status.READY, received = len, total = len, bytesPerSec = 0,
                        verdict = if (spec != null) Verdict.VERIFIED else Verdict.UNVERIFIED, message = null)
                }
                // User chose auto-import for verified files. Unverified files never auto-import:
                // the Hub still shows an explicit confirmation dialog for those.
                if (spec != null && algorithm != null && HubSettings.state.value.autoImport)
                    FirmwareImportQueue.enqueue(ctx, entry, out)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: ChecksumMismatch) {
                set(entry.id) { copy(status = Status.FAILED, verdict = Verdict.MISMATCH, message = e.message) }
                return
            } catch (e: Throwable) {
                // A checksum mismatch is never retried; a network error may be.
                if (!keepPartial && part.isFile && part.length() == 0L) part.delete()
                if (attempt >= 3) throw e
                set(entry.id) { copy(status = Status.DOWNLOADING, message = "Retry $attempt/3: ${e.message}") }
                delay(1500L * attempt)
            }
        }
    }

    private fun requireAllowedNetwork(ctx: Context) {
        if (!HubSettings.state.value.wifiOnly) return
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            throw IOException("Wi-Fi only is enabled; connect to Wi-Fi to download firmware")
    }

    private fun set(id: String, f: EntryState.() -> EntryState) {
        _entries.update { map -> map + (id to f(map[id] ?: EntryState(id))) }
    }

    private suspend fun coroutineContextIsActive(): Boolean =
        kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive ?: true

    fun fmtBytes(b: Long): String {
        if (b <= 0) return "?"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = b.toDouble(); var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return if (i == 0) "$b B" else String.format(Locale.US, "%.1f %s", v, units[i])
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
}
