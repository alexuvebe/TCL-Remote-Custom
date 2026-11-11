package dev.cavatz.keybindertv.ui.screen.appchooser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.AppIcon
import dev.cavatz.keybindertv.ui.component.TvCard
import dev.cavatz.keybindertv.util.InstalledAppsProvider
import kotlinx.coroutines.launch

/** Nombre de colonnes de la grille d'applications */
private const val GRID_COLUMNS: Int = 5

/** Taille de l'icône d'application dans la grille */
private val AppIconSize = 44.dp

/** Espacement entre les éléments de la grille */
private val GridSpacing = 8.dp

/** Padding interne des cartes d'application */
private val AppCardPadding = 8.dp

/** Padding global de l'écran */
private val ScreenPadding = 12.dp

/**
 * Écran de sélection d'application pour un mapping.
 * Affiche une grille d'apps installées avec une barre de recherche.
 *
 * @param navController Contrôleur de navigation
 * @param repository Repository des mappings
 * @param mappingId Identifiant du mapping à modifier
 * @param pressType Type d'appui — "short" ou "long"
 */
@Composable
fun AppChooserScreen(
    navController: NavHostController,
    repository: MappingRepository,
    mappingId: String,
    pressType: String
) {
    val context = LocalContext.current
    val appsProvider = InstalledAppsProvider(context)
    val viewModel: AppChooserViewModel = viewModel(
        factory = AppChooserViewModel.factory(repository, appsProvider, mappingId, pressType)
    )
    val uiState: AppChooserViewModel.AppChooserUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    AppChooserContent(
        uiState = uiState,
        onAppSelected = { app ->
            coroutineScope.launch {
                viewModel.selectApp(app)
                navController.popBackStack()
            }
        }
    )
}

/** Contenu stateless du sélecteur d'application */
@Composable
private fun AppChooserContent(
    uiState: AppChooserViewModel.AppChooserUiState,
    onAppSelected: (InstalledAppsProvider.AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding)
    ) {
        Text(
            text = stringResource(R.string.appchooser_title),
            style = MaterialTheme.typography.headlineMedium
        )
        when {
            uiState.isLoading -> LoadingGrid()
            else -> AppGrid(
                apps = uiState.apps,
                onAppSelected = onAppSelected
            )
        }
    }
}

/** Indicateur de chargement centré */
@Composable
private fun LoadingGrid() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/** Grille d'applications sélectionnables */
@Composable
private fun AppGrid(
    apps: List<InstalledAppsProvider.AppInfo>,
    onAppSelected: (InstalledAppsProvider.AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing)
    ) {
        items(
            items = apps,
            key = { app -> app.packageName }
        ) { app ->
            AppGridItem(app = app, onSelected = { onAppSelected(app) })
        }
    }
}

/** Carte d'une application dans la grille */
@Composable
private fun AppGridItem(
    app: InstalledAppsProvider.AppInfo,
    onSelected: () -> Unit
) {
    TvCard(onClick = onSelected) {
        Column(
            modifier = Modifier.padding(AppCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(
                packageName = app.packageName,
                contentDescription = app.appLabel,
                modifier = Modifier.size(AppIconSize)
            )
            Text(
                text = app.appLabel,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
