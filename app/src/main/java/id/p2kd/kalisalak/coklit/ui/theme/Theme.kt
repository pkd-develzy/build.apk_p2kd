package id.p2kd.kalisalak.coklit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    onPrimary = White,
    secondary = Amber500,
    onSecondary = Navy950,
    background = Navy950,
    onBackground = White,
    surface = Navy900,
    onSurface = White,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate300
)

@Composable
fun P2KDCoklitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
