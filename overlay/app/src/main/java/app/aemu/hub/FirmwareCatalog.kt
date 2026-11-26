package app.aemu.hub

/**
 * Small, hand-curated firmware catalog for the Firmware Hub.
 *
 * No firmware is hosted or redistributed by this app: every URL points at the device maker's
 * own server or at a public archive. Compatibility notes are community reports, NOT guarantees.
 *
 * Checksums come from the file metadata published by the host (Internet Archive's
 * /metadata/<item> API for the archived files). SHA-1 is what that API publishes; it is
 * weaker than SHA-256 but is still an authoritative host-side fingerprint for these files.
 * Where no published fingerprint could be found the checksum is null and the UI says so
 * loudly: those files are downloaded and imported only with an explicit user confirmation.
 */
object FirmwareCatalog {

    data class Checksum(
        val algorithm: String, // "SHA-1" or "SHA-256"
        val hex: String,
        val publishedAt: String,
    )

    data class Entry(
        val id: String,
        val device: String,
        val codename: String,
        val brand: String,
        val android: String,
        val skin: String,
        val format: String,
        /** Community compatibility report; shown as a report, never as a guarantee. */
        val compatReport: String,
        val sizeBytes: Long,
        val url: String,
        val fileName: String,
        val sourcePage: String,
        val source: String,
        val checksum: Checksum?,
        /** Shown next to the file when [checksum] is null. */
        val verificationNote: String? = null,
    ) {
        val verified: Boolean get() = checksum != null
    }

    private val archiveMeta = { item: String -> "https://archive.org/metadata/$item" }

    val entries: List<Entry> = listOf(
        Entry(
            id = "nexus4",
            device = "Nexus 4",
            codename = "occam",
            brand = "Google",
            android = "4.4.4",
            skin = "AOSP",
            format = "factory .tgz",
            compatReport = "Community report: boots, sound works",
            sizeBytes = 466_585_848L,
            url = "https://dl.google.com/dl/android/aosp/occam-ktu84p-factory-b6ac3ad6.tgz",
            fileName = "occam-ktu84p-factory-b6ac3ad6.tgz",
            sourcePage = "https://developers.google.com/android/images",
            source = "dl.google.com",
            checksum = null,
            verificationNote = "Google's archived factory-image page does not publish a machine-readable checksum for this old build. No fingerprint could be verified.",
        ),
        Entry(
            id = "galaxynexus",
            device = "Galaxy Nexus",
            codename = "takju",
            brand = "Google",
            android = "4.3",
            skin = "AOSP",
            format = "factory .tgz",
            compatReport = "Community report: boots, sound works",
            sizeBytes = 298_973_753L,
            url = "https://dl.google.com/dl/android/aosp/takju-jwr66y-factory-5104ab1d.tgz",
            fileName = "takju-jwr66y-factory-5104ab1d.tgz",
            sourcePage = "https://developers.google.com/android/images",
            source = "dl.google.com",
            checksum = null,
            verificationNote = "Google's archived factory-image page does not publish a machine-readable checksum for this old build. No fingerprint could be verified.",
        ),
        Entry(
            id = "galaxys2",
            device = "Galaxy S II",
            codename = "GT-I9100",
            brand = "Samsung",
            android = "2.3.3",
            skin = "TouchWiz",
            format = "Odin ZIP",
            compatReport = "Community report: boots; Bluetooth settings dialog",
            sizeBytes = 344_434_245L,
            url = "https://archive.org/download/i9100xexe/I9100XEKE1.zip",
            fileName = "I9100XEKE1.zip",
            sourcePage = archiveMeta("i9100xexe"),
            source = "archive.org",
            checksum = Checksum("SHA-1", "3244184dc2b27065b7351fe8282c0d2f2f76adcd", archiveMeta("i9100xexe")),
        ),
        Entry(
            id = "galaxys3",
            device = "Galaxy S III",
            codename = "GT-I9300",
            brand = "Samsung",
            android = "4.3",
            skin = "TouchWiz",
            format = "Odin ZIP",
            compatReport = "Community report: boots; sound not working yet",
            sizeBytes = 987_544_390L,
            url = "https://archive.org/download/i-9300-xxugnj-2-i-9300-oxxgnj-1-xef/I9300XXUGNJ2_I9300OXXGNJ1_XEF.zip",
            fileName = "I9300XXUGNJ2_I9300OXXGNJ1_XEF.zip",
            sourcePage = archiveMeta("i-9300-xxugnj-2-i-9300-oxxgnj-1-xef"),
            source = "archive.org",
            checksum = Checksum("SHA-1", "4e923786154c5b4efcd7be95dadcffbdd0c5d2d5",
                archiveMeta("i-9300-xxugnj-2-i-9300-oxxgnj-1-xef")),
        ),
        Entry(
            id = "redmi1s",
            device = "Redmi 1S",
            codename = "HM2014011",
            brand = "Xiaomi",
            android = "4.4.2",
            skin = "MIUI 8",
            format = "recovery ZIP",
            compatReport = "Community report: boots to setup",
            sizeBytes = 524_877_458L,
            url = "https://archive.org/download/HM2014011/multirom_HM1STD_V8.5.1.0.KHFCNED_v4.4.2_b10_3f73d79027.zip",
            fileName = "multirom_HM1STD_V8.5.1.0.KHFCNED_v4.4.2_b10_3f73d79027.zip",
            sourcePage = archiveMeta("HM2014011"),
            source = "archive.org",
            checksum = Checksum("SHA-1", "4e0e7dc467ab16739b509c347b368ecf8b05c52f", archiveMeta("HM2014011")),
        ),
        Entry(
            id = "galaxys2miui",
            device = "Galaxy S II (port)",
            codename = "GT-I9100",
            brand = "Samsung",
            android = "4.4",
            skin = "MIUI 7",
            format = "recovery ZIP",
            compatReport = "Community report: unofficial port crashes",
            sizeBytes = 444_909_635L,
            url = "https://archive.org/download/FullotaMiui7I9100Initial.release/Fullota_Miui7-I9100-Initial.release.zip",
            fileName = "Fullota_Miui7-I9100-Initial.release.zip",
            sourcePage = archiveMeta("FullotaMiui7I9100Initial.release"),
            source = "archive.org",
            checksum = Checksum("SHA-1", "2f96a0ae4142526b07032c4018e65c36f91d98f1",
                archiveMeta("FullotaMiui7I9100Initial.release")),
        ),
    )

    val brands: List<String> = listOf("All") + entries.map { it.brand }.distinct().sorted()

    fun find(id: String): Entry? = entries.firstOrNull { it.id == id }
}
