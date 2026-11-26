package app.aemu.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand blue (#3D5AFE) and its Material 3 tones, inherited from upstream.
private val Light = lightColorScheme(
    primary = Color(0xFF3D5AFE), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF), onPrimaryContainer = Color(0xFF00115A),
    secondary = Color(0xFF5B5D72), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0F9), onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD7F1), onTertiaryContainer = Color(0xFF2D1228),
    background = Color(0xFFFBF8FF), surface = Color(0xFFFBF8FF),
    surfaceContainer = Color(0xFFEFEDF7), surfaceContainerHigh = Color(0xFFE9E7F1),
    surfaceContainerHighest = Color(0xFFE3E1EC), surfaceContainerLow = Color(0xFFF5F2FC),
    error = Color(0xFFBA1A1A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF2E45D6), onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD), onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434659), onSecondaryContainer = Color(0xFFE0E0F9),
    tertiary = Color(0xFFE6BAD7), onTertiary = Color(0xFF44263D),
    tertiaryContainer = Color(0xFF5D3C55), onTertiaryContainer = Color(0xFFFFD7F1),
    background = Color(0xFF121318), surface = Color(0xFF121318),
    surfaceContainer = Color(0xFF1F1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A), surfaceContainerLow = Color(0xFF1B1B21),
)

/** Glass palette: translucent surfaces over a tinted background. All other tokens stay brand-true. */
private val GlassLight = Light.copy(
    background = Color(0xFFEDEAF8),
    surface = Color(0x66FFFFFF),
    surfaceContainerLowest = Color(0x99FFFFFF),
    surfaceContainerLow = Color(0x8AFFFFFF),
    surfaceContainer = Color(0x75FFFFFF),
    surfaceContainerHigh = Color(0x66FFFFFF),
    surfaceContainerHighest = Color(0x59FFFFFF),
    outline = Color(0xFF7A7690),
)

private val GlassDark = Dark.copy(
    background = Color(0xFF0D1018),
    surface = Color(0x14FFFFFF),
    surfaceContainerLowest = Color(0x14FFFFFF),
    surfaceContainerLow = Color(0x17FFFFFF),
    surfaceContainer = Color(0x1AFFFFFF),
    surfaceContainerHigh = Color(0x21FFFFFF),
    surfaceContainerHighest = Color(0x29FFFFFF),
    outline = Color(0xFFB6B9CC),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val base = Typography()
private val AppType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun AemuTheme(forceDark: Boolean? = null, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // Keep the main screen in sync with theme changes made in the settings activity.
    val rev = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(ctx) {
        val sp = app.aemu.AppPrefs.prefs(ctx)
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> rev.intValue++ }
        sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(l) }
    }
    rev.intValue
    val glass = rememberGlassEnabled()
    val dark = forceDark ?: when (app.aemu.AppPrefs.theme(ctx)) {
        app.aemu.AppPrefs.THEME_LIGHT -> false
        app.aemu.AppPrefs.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val dynamicActive = Build.VERSION.SDK_INT >= 31 && app.aemu.AppPrefs.dynamicColor(ctx)
    val scheme = when {
        dynamicActive -> {
            val dynamic = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (glass) dynamic.copy(
                surface = if (dark) Color(0x14FFFFFF) else Color(0x75FFFFFF),
                surfaceContainer = if (dark) Color(0x1AFFFFFF) else Color(0x75FFFFFF),
                surfaceContainerLow = if (dark) Color(0x17FFFFFF) else Color(0x8AFFFFFF),
                surfaceContainerHigh = if (dark) Color(0x21FFFFFF) else Color(0x66FFFFFF),
            ) else dynamic
        }
        glass && dark -> GlassDark
        glass -> GlassLight
        dark -> Dark
        else -> Light
    }
    CompositionLocalProvider(LocalGlassTheme provides glass) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppType,
        ) {
            GlassBackdrop(content)
        }
    }
}

val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
