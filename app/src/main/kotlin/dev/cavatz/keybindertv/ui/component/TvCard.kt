package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import dev.cavatz.keybindertv.ui.component.tvFocusBorder

/**
 * Carte réutilisable adaptée aux interfaces TV.
 * Encapsule [androidx.tv.material3.Card] avec les paramètres par défaut.
 *
 * @param onClick Action déclenchée lors de la sélection
 * @param modifier Modificateur Compose optionnel
 * @param content Contenu de la carte
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.tvFocusBorder(),
        scale = CardDefaults.scale()
    ) {
        Column(content = content)
    }
}
