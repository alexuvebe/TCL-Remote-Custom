package dev.cavatz.keybindertv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tema oscuro con colores de acción claramente diferenciados. */
private val KeyBinderColorScheme = darkColorScheme(
    primary = ActionBlue,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryBlueDark,
    secondary = ActionGreen,
    onSecondary = OnPrimaryDark,
    secondaryContainer = ActionGreen,
    tertiary = ActionPurple,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = SecondaryPurpleDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun KeyBinderTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KeyBinderColorScheme,
        typography = KeyBinderTypography,
        content = content
    )
}
