package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Borde blanco de alto contraste para identificar el elemento enfocado con el mando. */
@Composable
fun Modifier.tvFocusBorder(shape: Shape = RoundedCornerShape(8.dp)): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.isFocused }
        .border(
            border = if (focused) BorderStroke(3.dp, Color.White) else BorderStroke(0.dp, Color.Transparent),
            shape = shape
        )
}
