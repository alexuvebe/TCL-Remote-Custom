package dev.cavatz.keybindertv.ui.screen.mappings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.navigation.Screen
import dev.cavatz.keybindertv.ui.component.tvFocusBorder
import dev.cavatz.keybindertv.ui.theme.BackgroundDark
import dev.cavatz.keybindertv.ui.theme.OnSurfaceDark
import dev.cavatz.keybindertv.ui.theme.OutlineDark
import dev.cavatz.keybindertv.ui.theme.SurfaceDark
import dev.cavatz.keybindertv.ui.theme.SurfaceVariantDark
import kotlinx.coroutines.launch

private val ScreenPadding = 14.dp
private val CardPadding = 7.dp
private val CardSpacing = 5.dp
private val CardShape = RoundedCornerShape(10.dp)

@Composable
private fun mappingButtonColors() = ButtonDefaults.colors(
    // Suave azul grisáceo: se distingue del fondo sin resultar demasiado claro.
    containerColor = Color(0xFF53697D),
    contentColor = Color(0xFFFFFFFF),
    focusedContainerColor = Color(0xFF7893AA),
    focusedContentColor = Color(0xFF101820),
    pressedContainerColor = Color(0xFF667F94),
    pressedContentColor = Color(0xFFFFFFFF),
    disabledContainerColor = Color(0xFF465765),
    disabledContentColor = Color(0xFFE2E8ED)
)

@Composable
fun MappingListScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: MappingViewModel = viewModel(factory = MappingViewModel.factory(repository))
    val uiState: MappingViewModel.MappingListUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    MappingListContent(
        uiState = uiState,
        onToggle = viewModel::toggleMapping,
        onToggleShort = viewModel::toggleShortPress,
        onToggleLong = viewModel::toggleLongPress,
        onRemove = viewModel::removeMapping,
        onEditShort = { id -> navController.navigate(Screen.AppChooser.createRoute(id, "short")) },
        onEditLong = { id -> navController.navigate(Screen.AppChooser.createRoute(id, "long")) },
        onAddMapping = {
            coroutineScope.launch {
                val newMappingId = viewModel.createNewMapping()
                navController.navigate(Screen.Learning.createRoute(newMappingId))
            }
        }
    )
}

@Composable
private fun MappingListContent(
    uiState: MappingViewModel.MappingListUiState,
    onToggle: (String, Boolean) -> Unit,
    onToggleShort: (String, Boolean) -> Unit,
    onToggleLong: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onEditShort: (String) -> Unit,
    onEditLong: (String) -> Unit,
    onAddMapping: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(ScreenPadding)
    ) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.mappings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.mappings_empty), color = OnSurfaceDark)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 62.dp),
                verticalArrangement = Arrangement.spacedBy(CardSpacing)
            ) {
                items(items = uiState.mappings, key = { it.id }) { mapping ->
                    MappingItem(
                        mapping = mapping,
                        onToggle = { onToggle(mapping.id, it) },
                        onToggleShort = { onToggleShort(mapping.id, it) },
                        onToggleLong = { onToggleLong(mapping.id, it) },
                        onRemove = { onRemove(mapping.id) },
                        onEditShort = { onEditShort(mapping.id) },
                        onEditLong = { onEditLong(mapping.id) }
                    )
                }
            }
        }

        Button(
            onClick = onAddMapping,
            modifier = Modifier.align(Alignment.BottomEnd).tvFocusBorder(),
            colors = mappingButtonColors()
        ) {
            Text(stringResource(R.string.mappings_add))
        }
    }
}

@Composable
private fun MappingItem(
    mapping: KeyMapping,
    onToggle: (Boolean) -> Unit,
    onToggleShort: (Boolean) -> Unit,
    onToggleLong: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onEditShort: () -> Unit,
    onEditLong: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, CardShape)
            .border(BorderStroke(1.dp, OutlineDark), CardShape)
            .padding(CardPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    mapping.button.userLabel.ifEmpty { "Botón configurado" },
                    color = OnSurfaceDark,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Código: ${mapping.button.keyCode ?: "—"} · De fábrica: ${factoryLabel(mapping.button.keyCode)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Button(onClick = { onToggle(!mapping.isEnabled) }, modifier = Modifier.tvFocusBorder(), contentPadding = PaddingValues(horizontal = 7.dp, vertical = 1.dp), colors = mappingButtonColors()) {
                Text(if (mapping.isEnabled) "ACTIVO" else "INACTIVO")
            }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onRemove, modifier = Modifier.tvFocusBorder(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), colors = mappingButtonColors()) {
                Text("×")
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionControl(
                title = "CORTA",
                destination = mapping.shortPressAction?.appLabel ?: "Elegir aplicación",
                enabled = mapping.shortPressEnabled,
                onToggle = { onToggleShort(!mapping.shortPressEnabled) },
                onEdit = onEditShort,
                modifier = Modifier.weight(1f)
            )
            ActionControl(
                title = "LARGA",
                destination = when {
                    mapping.longPressAction != null -> mapping.longPressAction.appLabel
                    mapping.shortPressAction != null -> "Usar acción corta"
                    else -> "Elegir aplicación"
                },
                enabled = mapping.longPressEnabled,
                onToggle = { onToggleLong(!mapping.longPressEnabled) },
                onEdit = onEditLong,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionControl(
    title: String,
    destination: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onEdit, modifier = Modifier.weight(1f).tvFocusBorder().height(46.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp), colors = mappingButtonColors()) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(
                    destination,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Button(onClick = onToggle, modifier = Modifier.tvFocusBorder().height(46.dp), contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp), colors = mappingButtonColors()) {
            Text(if (enabled) "✓" else "OFF")
        }
    }
}

private fun factoryLabel(keyCode: Int?): String = when (keyCode) {
    4062 -> "Netflix"
    4099 -> "Prime Video"
    4140 -> "Guard"
    4057 -> "Media"
    4065 -> "YouTube"
    4020 -> "TCL"
    else -> "Sin etiqueta"
}
