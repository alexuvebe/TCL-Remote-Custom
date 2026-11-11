package dev.cavatz.keybindertv.ui.screen.mappings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.TvCard
import dev.cavatz.keybindertv.ui.navigation.Screen
import kotlinx.coroutines.launch

private val CardPadding = 10.dp
private val ItemSpacing = 6.dp
private val ScreenPadding = 12.dp

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
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.mappings.isEmpty() -> EmptyMappingList(onAddMapping)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(ItemSpacing)
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

        if (!uiState.isLoading) {
            Button(
                onClick = onAddMapping,
                modifier = Modifier.align(Alignment.BottomEnd).padding(ScreenPadding)
            ) {
                Text(stringResource(R.string.mappings_add))
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMappingList(onAddMapping: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.mappings_empty), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(ItemSpacing))
            Button(onClick = onAddMapping) { Text(stringResource(R.string.mappings_add)) }
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
    TvCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        mapping.button.userLabel.ifEmpty { "Botón configurado" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Código: ${mapping.button.keyCode ?: "—"} · De fábrica: ${factoryLabel(mapping.button.keyCode)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = mapping.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar asignación y restaurar botón",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            ActionRow(
                title = "Pulsación corta",
                destination = mapping.shortPressAction?.appLabel ?: "Sin asignar",
                enabled = mapping.shortPressEnabled,
                onToggle = onToggleShort,
                onEdit = onEditShort
            )
            ActionRow(
                title = "Pulsación larga",
                destination = mapping.longPressAction?.appLabel
                    ?: mapping.shortPressAction?.let { "Usar acción corta: ${it.appLabel}" }
                    ?: "Sin asignar",
                enabled = mapping.longPressEnabled,
                onToggle = onToggleLong,
                onEdit = onEditLong
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    destination: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(destination, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
        TextButton(onClick = onEdit) {
            Text("Elegir")
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
