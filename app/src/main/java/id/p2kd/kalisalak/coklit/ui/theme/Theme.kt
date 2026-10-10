package id.p2kd.kalisalak.coklit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExecutiveLightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = PureWhite,
    secondary = Amber600,
    onSecondary = PureWhite,
    background = SoftWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary
)

@Composable
fun P2KDCoklitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExecutiveLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun P2kdTheme(
    content: @Composable () -> Unit
) {
    P2KDCoklitTheme(content)
}
