package dev.cavatz.keybindertv.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api

/** Tarjeta de TV con un marco blanco grueso cuando recibe el foco del mando. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusModifier = Modifier
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = if (isFocused) 4.dp else 0.dp,
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        )

    Card(
        onClick = onClick,
        modifier = modifier.then(focusModifier),
        scale = CardDefaults.scale()
    ) {
        Column(content = content)
    }
}
