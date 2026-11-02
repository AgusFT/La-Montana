package ar.com.lamontana.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF062D4B)
val Blue = Color(0xFF0D5FEA)
val Orange = Color(0xFFFF7A18)
val Green = Color(0xFF149447)
val Ink = Color(0xFF0B2342)
val Canvas = Color(0xFFF3F7FC)
val Line = Color(0xFFD8E2EE)

private val colors = lightColorScheme(
    primary = Blue,
    secondary = Orange,
    tertiary = Green,
    background = Canvas,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    outline = Line,
)

@Composable
fun LaMontanaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
