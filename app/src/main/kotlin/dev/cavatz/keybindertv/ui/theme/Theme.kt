package dev.cavatz.keybindertv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Schéma de couleurs sombre — les TV utilisent toujours un thème sombre
 * pour réduire la fatigue oculaire et les reflets sur les grands écrans.
 */
private val KeyBinderColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryBlueDark,
    secondary = SecondaryPurple,
    onSecondary = OnPrimaryDark,
    secondaryContainer = SecondaryPurpleDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRed
)

/**
 * Thème principal de l'application KeyBinderTv.
 * Toujours en mode sombre, adapté aux interfaces TV.
 *
 * @param content Contenu Compose à encapsuler dans le thème
 */
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
