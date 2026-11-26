package app.aemu.hub

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** User preferences that only affect the Firmware Hub. Stored in the existing "hub" prefs. */
object HubSettings {

    enum class Layout { CARDS, COMPACT, GRID, MASONRY }

    data class Values(
        val autoImport: Boolean = true,
        val wifiOnly: Boolean = false,
        val deleteAfterImport: Boolean = false,
        val showUnverified: Boolean = true,
        val reverifyBeforeImport: Boolean = false,
        val layout: Layout = Layout.CARDS,
        /** 0 = unlimited; otherwise maximum total bytes of files kept in the Hub. */
        val storageQuotaGb: Int = 0,
    )

    private val _state = MutableStateFlow(Values())
    val state: StateFlow<Values> = _state.asStateFlow()

    fun load(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences("hub", Context.MODE_PRIVATE)
        _state.value = Values(
            autoImport = p.getBoolean("s_auto_import", true),
            wifiOnly = p.getBoolean("s_wifi_only", false),
            deleteAfterImport = p.getBoolean("s_delete_after_import", false),
            showUnverified = p.getBoolean("s_show_unverified", true),
            reverifyBeforeImport = p.getBoolean("s_reverify", false),
            layout = runCatching { Layout.valueOf(p.getString("s_layout", "CARDS") ?: "CARDS") }
                .getOrDefault(Layout.CARDS),
            storageQuotaGb = p.getInt("s_quota_gb", 0),
        )
    }

    fun setStorageQuota(ctx: Context, gb: Int) {
        ctx.applicationContext.getSharedPreferences("hub", Context.MODE_PRIVATE)
            .edit().putInt("s_quota_gb", gb).apply()
        _state.update { it.copy(storageQuotaGb = gb) }
    }

    fun setAutoImport(ctx: Context, value: Boolean) = put(ctx, "s_auto_import", value) { copy(autoImport = value) }
    fun setWifiOnly(ctx: Context, value: Boolean) = put(ctx, "s_wifi_only", value) { copy(wifiOnly = value) }
    fun setDeleteAfterImport(ctx: Context, value: Boolean) = put(ctx, "s_delete_after_import", value) { copy(deleteAfterImport = value) }
    fun setShowUnverified(ctx: Context, value: Boolean) = put(ctx, "s_show_unverified", value) { copy(showUnverified = value) }
    fun setReverify(ctx: Context, value: Boolean) = put(ctx, "s_reverify", value) { copy(reverifyBeforeImport = value) }

    fun setLayout(ctx: Context, value: Layout) {
        ctx.applicationContext.getSharedPreferences("hub", Context.MODE_PRIVATE)
            .edit().putString("s_layout", value.name).apply()
        _state.update { it.copy(layout = value) }
    }

    private fun put(ctx: Context, key: String, value: Boolean, transform: Values.() -> Values) {
        ctx.applicationContext.getSharedPreferences("hub", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
        _state.update(transform)
    }
}
