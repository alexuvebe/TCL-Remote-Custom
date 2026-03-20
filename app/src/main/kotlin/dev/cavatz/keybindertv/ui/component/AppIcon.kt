package dev.cavatz.keybindertv.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Affiche l'icône d'une application Android via son packageName.
 * Utilise Coil pour le chargement asynchrone depuis le PackageManager.
 *
 * @param packageName Nom du package de l'application cible
 * @param contentDescription Description pour l'accessibilité
 * @param modifier Modificateur Compose optionnel
 */
@Composable
fun AppIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Chargement de l'icône via le PackageManager — mémoïsé pour éviter les recompositions
    val iconDrawable = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(iconDrawable)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
