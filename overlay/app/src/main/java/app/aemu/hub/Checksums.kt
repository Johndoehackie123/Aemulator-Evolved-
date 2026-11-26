package app.aemu.hub

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Streaming checksum helpers shared by the downloader, the verifier and the unit tests. */
object Checksums {

    class Digest(val algorithm: String) {
        private val md = MessageDigest.getInstance(algorithm)
        var bytes: Long = 0L
            private set

        fun update(buf: ByteArray, len: Int = buf.size) {
            if (len > 0) { md.update(buf, 0, len); bytes += len }
        }

        fun hex(): String = hex(md.digest())
    }

    fun digestOf(input: InputStream, algorithm: String, buffer: ByteArray = ByteArray(256 * 1024)): String {
        val d = Digest(algorithm)
        input.use { while (true) { val n = it.read(buffer); if (n < 0) break; d.update(buffer, n) } }
        return d.hex()
    }

    fun digestOf(file: File, algorithm: String): String =
        file.inputStream().use { digestOf(it, algorithm) }

    fun hex(bytes: ByteArray): String {
        val chars = "0123456789abcdef"
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            out.append(chars[v ushr 4]).append(chars[v and 0x0f])
        }
        return out.toString()
    }

    /** Constant-time-ish comparison; both values are hex strings of the same algorithm. */
    fun matches(expected: String, actual: String): Boolean {
        val e = expected.trim().lowercase()
        val a = actual.trim().lowercase()
        if (e.length != a.length || e.isEmpty()) return false
        var diff = 0
        for (i in e.indices) diff = diff or (e[i].code xor a[i].code)
        return diff == 0
    }

    fun algorithm(spec: FirmwareCatalog.Checksum?): String? = when (spec?.algorithm?.uppercase()) {
        "SHA-1", "SHA1" -> "SHA-1"
        "SHA-256", "SHA256" -> "SHA-256"
        else -> null
    }
}
