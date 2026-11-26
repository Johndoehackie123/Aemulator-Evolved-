package app.aemu.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.ByteArrayInputStream

/** Pure-JVM checks for the hub's verification logic (runs without an emulator). */
class ChecksumsTest {

    @Test
    fun knownSha1AndSha256Vectors() {
        val bytes = "abc".toByteArray(Charsets.UTF_8)
        assertTrue(Checksums.matches("a9993e364706816aba3e25717850c26c9cd0d89d",
            Checksums.digestOf(ByteArrayInputStream(bytes), "SHA-1")))
        assertTrue(Checksums.matches("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Checksums.digestOf(ByteArrayInputStream(bytes), "SHA-256")))
    }

    @Test
    fun fileDigestMatchesStreamDigest() {
        val f = File.createTempFile("hub-checksum", ".bin")
        f.writeBytes("firmware".toByteArray(Charsets.UTF_8))
        val expected = Checksums.digestOf(ByteArrayInputStream(f.readBytes()), "SHA-1")
        assertTrue(Checksums.matches(expected, Checksums.digestOf(f, "SHA-1")))
        f.delete()
    }

    @Test
    fun comparisonIsNormalisedAndStrict() {
        assertTrue(Checksums.matches("ABCDEF", "abcdef"))
        assertTrue(Checksums.matches(" abcdef ", "ABCDEF"))
        assertFalse(Checksums.matches("abcdef", "abcde"))
        assertFalse(Checksums.matches("abcdef", "000000"))
        assertFalse(Checksums.matches("", ""))
    }

    @Test
    fun catalogIsConsistent() {
        assertTrue(FirmwareCatalog.entries.isNotEmpty())
        assertTrue(FirmwareCatalog.entries.map { it.id }.toSet().size == FirmwareCatalog.entries.size)
        FirmwareCatalog.entries.forEach { e ->
            assertTrue(e.url.startsWith("https://"))
            assertTrue(e.sourcePage.startsWith("https://"))
            assertTrue(e.sizeBytes > 0)
            e.checksum?.let { c ->
                assertTrue(c.hex.length == 40 || c.hex.length == 64)
                assertTrue(c.hex.all { it in '0'..'9' || it in 'a'..'f' })
                assertTrue(Checksums.algorithm(c) != null)
            }
        }
    }

    @Test
    fun archiveFingerprintsArePresentForArchivedEntries() {
        val expected = mapOf(
            "galaxys2" to "3244184dc2b27065b7351fe8282c0d2f2f76adcd",
            "galaxys3" to "4e923786154c5b4efcd7be95dadcffbdd0c5d2d5",
            "redmi1s" to "4e0e7dc467ab16739b509c347b368ecf8b05c52f",
            "galaxys2miui" to "2f96a0ae4142526b07032c4018e65c36f91d98f1",
        )
        expected.forEach { (id, sha1) ->
            val entry = FirmwareCatalog.find(id)!!
            assertTrue(entry.verified)
            assertTrue(Checksums.matches(sha1, entry.checksum!!.hex))
        }
        // The two Google entries have no published fingerprint and must be labelled as such.
        assertTrue(FirmwareCatalog.find("nexus4")!!.checksum == null)
        assertTrue(FirmwareCatalog.find("galaxynexus")!!.verificationNote != null)
    }
}
