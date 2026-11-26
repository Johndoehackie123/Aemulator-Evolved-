package app.aemu.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val Color.isDark: Boolean
    get() = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Blurred brand-colour field behind the glass. Real RenderEffect blur on Android 12+. */
@Composable
fun GlassBackdrop(content: @Composable () -> Unit) {
    if (!LocalGlassTheme.current) {
        content()
    } else {
        val dark = MaterialTheme.colorScheme.background.isDark
        val base = MaterialTheme.colorScheme.background
        val wash = if (dark) listOf(base, Color(0xFF191B2C), Color(0xFF101C26))
                   else listOf(base, Color(0xFFF1EEFF), Color(0xFFEAF7FF))
        Box(Modifier.fillMaxSize().background(base)) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(wash)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(if (dark) Color(0x664A5CFF) else Color(0x664D80FF))
                        .blur(75.dp),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(320.dp)
                        .clip(CircleShape)
                        .background(if (dark) Color(0x4A1E6FA0) else Color(0x4D77D9FF))
                        .blur(90.dp),
                )
            }
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

/** One translucent layer only; glass palette colours already carry their own alpha. */
@Composable
fun glassSurfaceColor(color: Color): Color = if (!LocalGlassTheme.current) {
    color
} else if (color.alpha < 0.99f) {
    color
} else {
    color.copy(alpha = 0.64f)
}

/** Translucent well with a luminous brand edge for icon buttons, chips and nav items. */
@Composable
fun Modifier.glassWell(
    shape: Shape = CircleShape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): Modifier = if (LocalGlassTheme.current) {
    this
        .background(tint.copy(alpha = 0.32f), shape)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), shape)
} else this

/** Rounded rectangular variant for chips and navigation bars. */
@Composable
fun Modifier.glassPanel(): Modifier = glassWell(RoundedCornerShape(20.dp))

@Composable
fun glassButtonColors(): ButtonColors = if (LocalGlassTheme.current) {
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
} else ButtonDefaults.buttonColors()

@Composable
fun glassTonalButtonColors(): ButtonColors = if (LocalGlassTheme.current) {
    ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else ButtonDefaults.filledTonalButtonColors()
