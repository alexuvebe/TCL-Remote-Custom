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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.cavatz.keybindertv.ui.navigation.Screen
import dev.cavatz.keybindertv.ui.theme.ActionBlue
import dev.cavatz.keybindertv.ui.theme.ActionRed
import kotlinx.coroutines.launch

/** Espacement interne des cartes de mapping */
private val CardPadding = 12.dp

/** Espacement entre les éléments de la liste */
private val ItemSpacing = 8.dp

/** Padding global de l'écran */
private val ScreenPadding = 16.dp

/**
 * Écran de liste des mappings configurés.
 * Permet d'activer/désactiver, supprimer et créer de nouveaux mappings.
 *
 * @param navController Contrôleur de navigation
 * @param repository Repository des mappings
 */
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
        onAddMapping = {
            coroutineScope.launch {
                // Crée le mapping vide puis navigue vers LearningScreen avec son ID
                val newMappingId: String = viewModel.createNewMapping()
                navController.navigate(Screen.Learning.createRoute(newMappingId))
            }
        }
    )
}

/** Contenu stateless de la liste des mappings */
@Composable
private fun MappingListContent(
    uiState: MappingViewModel.MappingListUiState,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onAddMapping: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.mappings.isEmpty() -> EmptyMappingList(onAddMapping = onAddMapping)
            else -> MappingList(
                mappings = uiState.mappings,
                onToggle = onToggle,
                onRemove = onRemove
            )
        }

        // Bouton "+" toujours visible sauf pendant le chargement
        if (!uiState.isLoading) {
            Button(
                onClick = onAddMapping,
                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ScreenPadding)
            ) {
                Text(text = stringResource(R.string.mappings_add))
            }
        }
    }
}

/** Indicateur de chargement centré */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/** Message affiché quand la liste est vide */
@Composable
private fun EmptyMappingList(onAddMapping: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.mappings_empty),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(ItemSpacing))
            Button(onClick = onAddMapping) {
                Text(text = stringResource(R.string.mappings_add))
            }
        }
    }
}

/** Liste scrollable des mappings */
@Composable
private fun MappingList(
    mappings: List<KeyMapping>,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
    ) {
        items(
            items = mappings,
            key = { mapping -> mapping.id }
        ) { mapping ->
            MappingItem(
                mapping = mapping,
                onToggle = { enabled -> onToggle(mapping.id, enabled) },
                onRemove = { onRemove(mapping.id) }
            )
        }
    }
}

/** Tarjeta de una asignación individual. */
@Composable
private fun MappingItem(
    mapping: KeyMapping,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    TvCard(
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MappingInfo(
                mapping = mapping,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = mapping.isEnabled,
                    onCheckedChange = onToggle
                )
                TextButton(onClick = onRemove, colors = ButtonDefaults.textButtonColors(contentColor = ActionRed)) {
                    Text(text = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

/** Informations textuelles d'un mapping */
@Composable
private fun MappingInfo(
    mapping: KeyMapping,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = mapping.button.userLabel.ifEmpty { "Botón #${mapping.id.take(8)}" },
            style = MaterialTheme.typography.titleMedium
        )
        mapping.shortPressAction?.let { action ->
            Text(
                text = "${stringResource(R.string.mapping_short_press)} : ${action.appLabel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        mapping.longPressAction?.let { action ->
            Text(
                text = "${stringResource(R.string.mapping_long_press)} : ${action.appLabel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (mapping.shortPressAction == null && mapping.longPressAction == null) {
            Text(
                text = stringResource(R.string.mapping_no_action),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
