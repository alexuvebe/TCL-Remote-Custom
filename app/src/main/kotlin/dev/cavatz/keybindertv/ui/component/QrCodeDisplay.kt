package dev.cavatz.keybindertv.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.cavatz.keybindertv.data.export.QrCodeCodec
import dev.cavatz.keybindertv.data.model.KeyBinderConfig

/**
 * Affiche un QR code encodant la configuration KeyBinderTv.
 * Si la configuration est trop volumineuse pour un QR, affiche un message d'erreur.
 *
 * @param config   Configuration à encoder dans le QR code
 * @param codec    Codec QR utilisé pour l'encodage (injecté pour faciliter les tests)
 * @param modifier Modifier Compose optionnel
 */
@Composable
fun QrCodeDisplay(
    config: KeyBinderConfig,
    codec: QrCodeCodec = remember { QrCodeCodec() },
    modifier: Modifier = Modifier
) {
    // Calcul mémoïsé du bitmap — ne recalcule que si la config change
    val qrBitmap: Bitmap? by remember(config) {
        mutableStateOf(codec.encode(config))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            QrCodeImage(bitmap = qrBitmap!!)
        } else {
            QrCodeTooLargeMessage()
        }
    }
}

/**
 * Affiche le bitmap QR code converti en [ImageBitmap] pour Compose.
 *
 * @param bitmap Bitmap Android à afficher
 */
@Composable
private fun QrCodeImage(bitmap: Bitmap) {
    val imageBitmap: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = "QR code de la configuration",
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Message affiché quand la configuration dépasse la capacité du QR code.
 */
@Composable
private fun QrCodeTooLargeMessage() {
    Text(
        text = "Configuration trop grande pour un QR code — utilisez l'export fichier",
        style = MaterialTheme.typography.bodyMedium
    )
}
