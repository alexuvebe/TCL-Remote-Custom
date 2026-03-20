package dev.cavatz.keybindertv.ui.screen.importexport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.cavatz.keybindertv.R

/**
 * Écran Import/Export — placeholder Phase 4.
 * L'implémentation QR code et fichier sera ajoutée lors de la Phase 4.
 */
@Composable
fun ImportExportScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.importexport_title) + " — disponible en Phase 4")
    }
}
