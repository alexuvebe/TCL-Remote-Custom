package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.cavatz.keybindertv.ui.theme.SurfaceDark

/**
 * Contenedor oscuro para tarjetas de TV.
 * Por defecto no es clicable para que los controles interiores reciban el foco.
 */
@Composable
fun TvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(content = content)
    }
}
