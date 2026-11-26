package app.aemu.hub

/**
 * Detail fields shown on Firmware Hub cards/rows.
 *
 * These are pure values (no Android dependencies) so the ordering rules can be unit tested
 * on the JVM. HubSettings persists the visible fields, in order, per layout.
 */
enum class HubField {
    STATUS,
    SIZE,
    CHECKSUM,
    SOURCE,
    FORMAT,
    REPORT,
}

object HubFieldDefaults {
    /** Canonical order. Missing/disabled fields are appended here when re-enabled. */
    val ORDER: List<HubField> = listOf(
        HubField.STATUS,
        HubField.SIZE,
        HubField.CHECKSUM,
        HubField.SOURCE,
        HubField.FORMAT,
        HubField.REPORT,
    )

    fun encode(fields: List<HubField>): String = fields.joinToString(",") { it.name }

    /** Accepts stored comma-separated names, drops unknown names and restores missing fields. */
    fun decode(raw: String?): List<HubField> {
        val known = raw.orEmpty().split(',')
            .mapNotNull { runCatching { HubField.valueOf(it.trim()) }.getOrNull() }
            .distinct()
        return if (known.isEmpty() && raw.isNullOrBlank()) ORDER
        else known + ORDER.filter { it !in known }
    }

    fun toggle(fields: List<HubField>, field: HubField): List<HubField> =
        if (field in fields) fields.filter { it != field }
        else ORDER.filter { it == field || it in fields }

    /** Move a field one position up (-1) or down (+1), keeping all fields present. */
    fun move(fields: List<HubField>, field: HubField, delta: Int): List<HubField> {
        val list = fields.toMutableList()
        val from = list.indexOf(field)
        if (from < 0) return list
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return list
        list.add(to, list.removeAt(from))
        return list
    }
}
