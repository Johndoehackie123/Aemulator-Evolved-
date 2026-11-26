package app.aemu.ui

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Glass theme preference. Observable so toggling it in Settings updates the whole app. */
object GlassSettings {
    private const val PREFS = "glass_theme"
    private const val KEY_ENABLED = "enabled"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun load(ctx: Context): Boolean {
        val saved = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
        _enabled.value = saved
        return saved
    }

    fun set(ctx: Context, value: Boolean) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }
}

/** True when the glass palette is active; cards use it for translucent surfaces. */
val LocalGlassTheme = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun glassSurfaceColor(color: Color): Color =
    if (LocalGlassTheme.current) color.copy(alpha = 0.46f) else color

@Composable
fun glassCardColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color? = null,
): CardColors = if (contentColor == null) {
    CardDefaults.cardColors(containerColor = glassSurfaceColor(containerColor))
} else {
    CardDefaults.cardColors(containerColor = glassSurfaceColor(containerColor), contentColor = contentColor)
}

@Composable
fun glassElevatedCardColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainer): CardColors =
    CardDefaults.elevatedCardColors(containerColor = glassSurfaceColor(containerColor))

/** Thin luminous edge that sells the frosted-glass look without hiding content. */
@Composable
fun Modifier.glassBorder(shape: Shape): Modifier =
    if (LocalGlassTheme.current) {
        this.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), shape)
    } else this

@Composable
fun rememberGlassEnabled(): Boolean = GlassSettings.enabled.collectAsState(initial = GlassSettings.load(LocalContext.current)).value
