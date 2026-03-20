package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cavatz.keybindertv.R

/** Couleur du cercle lorsque le service est actif */
private val ActiveColor: Color = Color(0xFF4CAF50)

/** Couleur du cercle lorsque le service est inactif */
private val InactiveColor: Color = Color(0xFFF44336)

/** Diamètre du cercle indicateur */
private val IndicatorSize = 12.dp

/** Espacement entre le cercle et le texte */
private val HorizontalSpacing = 8.dp

/**
 * Indicateur visuel d'état du service d'accessibilité.
 * Affiche un cercle vert (actif) ou rouge (inactif) avec un label textuel.
 *
 * @param isActive Vrai si le service est en cours d'exécution
 * @param modifier Modificateur Compose optionnel
 */
@Composable
fun StatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HorizontalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(isActive = isActive)
        StatusLabel(isActive = isActive)
    }
}

/** Cercle coloré indiquant l'état */
@Composable
private fun StatusDot(isActive: Boolean) {
    val color: Color = if (isActive) ActiveColor else InactiveColor
    Box(
        modifier = Modifier
            .size(IndicatorSize)
            .clip(CircleShape)
            .background(color)
    )
}

/** Texte "Actif" ou "Inactif" */
@Composable
private fun StatusLabel(isActive: Boolean) {
    val text: String = if (isActive) {
        stringResource(R.string.home_service_active)
    } else {
        stringResource(R.string.home_service_inactive)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
}
