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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.TvCard
import dev.cavatz.keybindertv.ui.navigation.Screen
import kotlinx.coroutines.launch

private val CardPadding = 12.dp
private val ItemSpacing = 8.dp
private val ScreenPadding = 16.dp

private val ActionRed = Color(0xFFD32F2F)
private val SelectBlue = Color(0xFF1976D2)
private val SaveGreen = Color(0xFF388E3C)
private val WarningYellow = Color(0xFFF9A825)
private val SettingsPurple = Color(0xFF7B1FA2)
private val DisabledGray = Color(0xFF424242)
private val White = Color(0xFFFFFFFF)

@Composable
fun MappingListScreen(navController: NavHostController, repository: MappingRepository) {
    val viewModel: MappingViewModel = viewModel(factory = MappingViewModel.factory(repository))
    val uiState: MappingViewModel.MappingListUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    MappingListContent(
        uiState = uiState,
        onToggle = { id, enabled -> viewModel.toggleMapping(id, enabled) },
        onToggleGlobal = viewModel::toggleGlobal,
        onToggleAction = viewModel::toggleAction,
        onRemove = viewModel::removeMapping,
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
    onToggleGlobal: (Boolean) -> Unit,
    onToggleAction: (KeyMapping, Boolean, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onAddMapping: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.mappings.isEmpty() -> Column(
                Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.mappings_empty), color = White)
                Spacer(Modifier.height(ItemSpacing))
                Button(onClick = onAddMapping, colors = ButtonDefaults.buttonColors(containerColor = ActionRed, contentColor = White)) {
                    Text(stringResource(R.string.mappings_add))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(ItemSpacing)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Configuración general", color = White, style = MaterialTheme.typography.titleMedium)
                            Text(if (uiState.isGloballyEnabled) "Todos los remapeos activados" else "Remapeos desactivados", color = White)
                        }
                        Switch(checked = uiState.isGloballyEnabled, onCheckedChange = onToggleGlobal)
                    }
                }
                items(uiState.mappings, key = { it.id }) { mapping ->
                    MappingItem(
                        mapping = mapping,
                        onToggle = { onToggle(mapping.id, it) },
                        onToggleAction = { isLong, enabled -> onToggleAction(mapping, isLong, enabled) },
                        onRemove = { onRemove(mapping.id) }
                    )
                }
                item {
                    Button(
                        onClick = onAddMapping,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionRed, contentColor = White)
                    ) { Text(stringResource(R.string.mappings_add)) }
                }
            }
        }
    }
}

@Composable
private fun MappingItem(
    mapping: KeyMapping,
    onToggle: (Boolean) -> Unit,
    onToggleAction: (Boolean, Boolean) -> Unit,
    onRemove: () -> Unit
) {
    TvCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(CardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Código: ${mapping.button.keyCode ?: "—"} · De fábrica: ${factoryName(mapping.button.keyCode)}",
                        color = White, style = MaterialTheme.typography.titleMedium
                    )
                    Text(if (mapping.isEnabled) "Activado" else "Desactivado", color = if (mapping.isEnabled) SaveGreen else WarningYellow)
                }
                Switch(checked = mapping.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Eliminar configuración", tint = White)
                }
            }
            PressActionRow(
                label = "Pulsación corta",
                app = mapping.shortPressAction?.appLabel ?: "Sin asignar",
                enabled = mapping.shortPressAction?.isEnabled == true,
                exists = mapping.shortPressAction != null,
                onToggle = { onToggleAction(false, it) }
            )
            PressActionRow(
                label = "Pulsación larga",
                app = mapping.longPressAction?.appLabel ?: "Sin asignar",
                enabled = mapping.longPressAction?.isEnabled == true,
                exists = mapping.longPressAction != null,
                onToggle = { onToggleAction(true, it) }
            )
        }
    }
}

@Composable
private fun PressActionRow(label: String, app: String, enabled: Boolean, exists: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = White, style = MaterialTheme.typography.labelMedium)
            Text(app, color = White, style = MaterialTheme.typography.bodyMedium)
        }
        if (exists) {
            Switch(checked = enabled, onCheckedChange = onToggle)
        } else {
            Text("—", color = White)
        }
    }
}

private fun factoryName(keyCode: Int?): String = when (keyCode) {
    4062 -> "Netflix"
    4099 -> "Prime Video"
    4140 -> "Guard"
    4057 -> "Media"
    4065 -> "YouTube"
    4020 -> "TCL"
    null -> "Desconocido"
    else -> "Botón original"
}
