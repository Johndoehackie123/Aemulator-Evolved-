package app.aemu.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButtonDefaults
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

/** Decorative blurred colour field behind the app. Real RenderEffect blur on Android 12+. */
@Composable
fun GlassBackdrop(content: @Composable () -> Unit) {
    if (!LocalGlassTheme.current) {
        content()
    } else {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.matchParentSize().background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFEDEBFF),
                        Color(0xFFF6F1FF),
                    ),
                ),
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(Color(0x554D80FF))
                    .blur(70.dp),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(Color(0x40B388FF))
                    .blur(85.dp),
            )
        }
        Box(Modifier.matchParentSize()) { content() }
    }
    }
}

/** Translucent well with a luminous edge for icon buttons, chips and nav items. */
@Composable
fun Modifier.glassWell(
    shape: Shape = CircleShape,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): Modifier = if (LocalGlassTheme.current) {
    this
        .background(tint.copy(alpha = 0.38f), shape)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
} else this

/** Rounded rectangular variant for chips and navigation bars. */
@Composable
fun Modifier.glassPanel(): Modifier = glassWell(RoundedCornerShape(20.dp))

@Composable
fun glassButtonColors(): ButtonColors = if (LocalGlassTheme.current) {
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
} else ButtonDefaults.buttonColors()

@Composable
fun glassTonalButtonColors(): ButtonColors = if (LocalGlassTheme.current) {
    FilledTonalButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else FilledTonalButtonDefaults.buttonColors()
