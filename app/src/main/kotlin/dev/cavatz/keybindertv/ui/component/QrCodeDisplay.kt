package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Placeholder pour l'affichage QR code — implémenté en Phase 4.
 * Affiche un texte indicatif en attendant l'implémentation complète.
 *
 * @param modifier Modificateur Compose optionnel
 */
@Composable
fun QrCodeDisplay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(text = "QR code — Phase 4")
    }
}
