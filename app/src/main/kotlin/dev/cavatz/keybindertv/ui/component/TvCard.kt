package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import dev.cavatz.keybindertv.ui.theme.SurfaceDark

/**
 * Contenedor oscuro para tarjetas de TV. Las tarjetas seleccionables muestran
 * un contorno blanco grueso al recibir el foco del mando.
 */
@Composable
fun TvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val focusModifier = if (onClick != null) {
        Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    BorderStroke(3.dp, Color.White),
                    shape
                ) else Modifier
            )
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(focusModifier),
        shape = shape,
        color = SurfaceDark,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(content = content)
    }
}
