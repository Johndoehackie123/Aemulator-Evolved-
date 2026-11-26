package app.aemu.hub

/**
 * The built-in firmware catalog.
 *
 * Every entry points at a file hosted by the manufacturer's own server or a public archive —
 * TimeDroid itself ships no firmware and only downloads what the user asks for.
 * Sizes were verified against the live servers (HTTP Content-Length / archive.org range probe).
 */
object FirmwareCatalog {

    data class Entry(
        /** stable id, used as the download key */
        val id: String,
        /** human name, e.g. "Nexus 4" */
        val device: String,
        /** codename / model number shown next to the name */
        val codename: String,
        val brand: String,
        /** Android version, e.g. "4.4.4" */
        val android: String,
        val skin: String,
        /** how the archive is packaged: "factory tgz", "Odin package", "recovery ZIP" */
        val format: String,
        /** compatibility note from the community-tested list */
        val status: String,
        /** exact file size in bytes, 0 = unknown */
        val sizeBytes: Long,
        val url: String,
        /** file name the archive is saved under */
        val fileName: String,
        /** human-readable page where the file comes from */
        val sourcePage: String,
        val source: String,
    )

    val entries: List<Entry> = listOf(
        Entry(
            id = "nexus4",
            device = "Nexus 4",
            codename = "occam",
            brand = "Google",
            android = "4.4.4",
            skin = "AOSP",
            format = "factory tgz",
            status = "Works — sound works",
            sizeBytes = 466_585_848L,
            url = "https://dl.google.com/dl/android/aosp/occam-ktu84p-factory-b6ac3ad6.tgz",
            fileName = "occam-ktu84p-factory-b6ac3ad6.tgz",
            sourcePage = "https://developers.google.com/android/images",
            source = "dl.google.com",
        ),
        Entry(
            id = "galaxynexus",
            device = "Galaxy Nexus",
            codename = "takju",
            brand = "Google",
            android = "4.3",
            skin = "AOSP",
            format = "factory tgz",
            status = "Works — sound works",
            sizeBytes = 298_973_753L,
            url = "https://dl.google.com/dl/android/aosp/takju-jwr66y-factory-5104ab1d.tgz",
            fileName = "takju-jwr66y-factory-5104ab1d.tgz",
            sourcePage = "https://developers.google.com/android/images",
            source = "dl.google.com",
        ),
        Entry(
            id = "galaxys2",
            device = "Galaxy S II",
            codename = "GT-I9100",
            brand = "Samsung",
            android = "2.3.3",
            skin = "TouchWiz",
            format = "Odin package",
            status = "Works — Bluetooth app dialog",
            sizeBytes = 344_434_245L,
            url = "https://archive.org/download/i9100xexe/I9100XEKE1.zip",
            fileName = "I9100XEKE1.zip",
            sourcePage = "https://archive.org/details/i9100xexe",
            source = "archive.org",
        ),
        Entry(
            id = "galaxys3",
            device = "Galaxy S III",
            codename = "GT-I9300",
            brand = "Samsung",
            android = "4.3",
            skin = "TouchWiz",
            format = "Odin package",
            status = "Partly — no sound yet",
            sizeBytes = 987_544_390L,
            url = "https://archive.org/download/i-9300-xxugnj-2-i-9300-oxxgnj-1-xef/I9300XXUGNJ2_I9300OXXGNJ1_XEF.zip",
            fileName = "I9300XXUGNJ2_I9300OXXGNJ1_XEF.zip",
            sourcePage = "https://archive.org/details/i-9300-xxugnj-2-i-9300-oxgnj-1-xef",
            source = "archive.org",
        ),
        Entry(
            id = "redmi1s",
            device = "Redmi 1S",
            codename = "HM2014011",
            brand = "Xiaomi",
            android = "4.4.2",
            skin = "MIUI 8",
            format = "recovery ZIP",
            status = "In progress — boots to setup",
            sizeBytes = 524_877_458L,
            url = "https://archive.org/download/HM2014011/multirom_HM1STD_V8.5.1.0.KHFCNED_v4.4.2_b10_3f73d79027.zip",
            fileName = "multirom_HM1STD_V8.5.1.0.KHFCNED_v4.4.2_b10_3f73d79027.zip",
            sourcePage = "https://archive.org/details/HM2014011",
            source = "archive.org",
        ),
        Entry(
            id = "galaxys2miui",
            device = "Galaxy S II (port)",
            codename = "GT-I9100",
            brand = "Samsung",
            android = "4.4",
            skin = "MIUI 7",
            format = "recovery ZIP",
            status = "Not yet — unofficial port crashes",
            sizeBytes = 444_909_635L,
            url = "https://archive.org/download/FullotaMiui7I9100Initial.release/Fullota_Miui7-I9100-Initial.release.zip",
            fileName = "Fullota_Miui7-I9100-Initial.release.zip",
            sourcePage = "https://archive.org/details/FullotaMiui7I9100Initial.release",
            source = "archive.org",
        ),
    )

    val brands: List<String> = listOf("All") + entries.map { it.brand }.distinct().sorted()

    fun find(id: String): Entry? = entries.firstOrNull { it.id == id }

    fun totalBytes(): Long = entries.sumOf { it.sizeBytes }
}
