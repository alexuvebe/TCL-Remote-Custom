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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.TvCard
import dev.cavatz.keybindertv.ui.component.tvFocusOutline
import dev.cavatz.keybindertv.ui.navigation.Screen
import dev.cavatz.keybindertv.ui.theme.ActionBlue
import dev.cavatz.keybindertv.ui.theme.ActionGray
import dev.cavatz.keybindertv.ui.theme.ActionGreen
import dev.cavatz.keybindertv.ui.theme.ActionRed
import kotlinx.coroutines.launch

private val CardPadding = 16.dp
private val ItemSpacing = 12.dp
private val ScreenPadding = 16.dp

@Composable
fun MappingListScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: MappingViewModel = viewModel(
        factory = MappingViewModel.factory(repository)
    )
    val uiState: MappingViewModel.MappingListUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    MappingListContent(
        uiState = uiState,
        onToggle = { id, enabled -> viewModel.toggleMapping(id, enabled) },
        onRemove = { id -> viewModel.removeMapping(id) },
        onConfigureShort = { id ->
            navController.navigate(Screen.AppChooser.createRoute(id, "short"))
        },
        onConfigureLong = { id ->
            navController.navigate(Screen.AppChooser.createRoute(id, "long"))
        },
        onAddMapping = {
            coroutineScope.launch {
                val newMappingId: String = viewModel.createNewMapping()
                navController.navigate(Screen.Learning.createRoute(newMappingId))
            }
        }
    )
}

@Composable
private fun MappingListContent(
    uiState: MappingViewModel.MappingListUiState,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onConfigureShort: (String) -> Unit,
    onConfigureLong: (String) -> Unit,
    onAddMapping: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.mappings.isEmpty() -> EmptyMappingList(onAddMapping = onAddMapping)
                else -> MappingList(
                    mappings = uiState.mappings,
                    onToggle = onToggle,
                    onRemove = onRemove,
                    onConfigureShort = onConfigureShort,
                    onConfigureLong = onConfigureLong
                )
            }
        }

        if (!uiState.isLoading) {
            Button(
                onClick = onAddMapping,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ScreenPadding)
                    .tvFocusOutline()
            ) {
                Text(text = stringResource(R.string.mappings_add))
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ActionBlue)
    }
}

@Composable
private fun EmptyMappingList(onAddMapping: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.mappings_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(ItemSpacing))
            Button(
                onClick = onAddMapping,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionBlue,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(R.string.mappings_add))
            }
        }
    }
}

@Composable
private fun MappingList(
    mappings: List<KeyMapping>,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onConfigureShort: (String) -> Unit,
    onConfigureLong: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
    ) {
        items(items = mappings, key = { mapping -> mapping.id }) { mapping ->
            MappingItem(
                mapping = mapping,
                onToggle = { enabled -> onToggle(mapping.id, enabled) },
                onRemove = { onRemove(mapping.id) },
                onConfigureShort = { onConfigureShort(mapping.id) },
                onConfigureLong = { onConfigureLong(mapping.id) }
            )
        }
    }
}

@Composable
private fun MappingItem(
    mapping: KeyMapping,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onConfigureShort: () -> Unit,
    onConfigureLong: () -> Unit
) {
    TvCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MappingInfo(mapping = mapping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfigureShort,
                    modifier = Modifier.weight(1f).tvFocusOutline(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Pulsación corta", maxLines = 1)
                }
                Button(
                    onClick = onConfigureLong,
                    modifier = Modifier.weight(1f).tvFocusOutline(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Pulsación larga", maxLines = 1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onToggle(!mapping.isEnabled) },
                    modifier = Modifier.tvFocusOutline(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mapping.isEnabled) ActionGray else ActionRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = if (mapping.isEnabled) "Desactivar" else "Activar")
                }
                Button(
                    onClick = onRemove,
                    modifier = Modifier.tvFocusOutline(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun MappingInfo(mapping: KeyMapping) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = mapping.button.userLabel.ifEmpty { "Botón #${mapping.id.take(8)}" },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        val shortLabel = when {
            mapping.shortPressUseDefault -> "De fábrica"
            mapping.shortPressAction != null -> mapping.shortPressAction.appLabel
            else -> "Sin acción"
        }
        val longLabel = when {
            mapping.longPressUseDefault -> "De fábrica"
            mapping.longPressAction != null -> mapping.longPressAction.appLabel
            mapping.shortPressAction != null && !mapping.shortPressUseDefault -> "${mapping.shortPressAction.appLabel} (igual que corta)"
            else -> "Sin acción"
        }
        Text(
            text = "Pulsación corta: $shortLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Pulsación larga: $longLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (mapping.shortPressAction == null && mapping.longPressAction == null &&
            !mapping.shortPressUseDefault && !mapping.longPressUseDefault
        ) {
            Text(
                text = stringResource(R.string.mapping_no_action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (mapping.isEnabled) "Estado: Activado" else "Estado: Desactivado",
            style = MaterialTheme.typography.labelMedium,
            color = if (mapping.isEnabled) ActionGreen else ActionRed
        )
    }
}
