package dev.cavatz.keybindertv.ui.screen.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.StatusIndicator
import dev.cavatz.keybindertv.ui.navigation.Screen

/** Espacement vertical entre les éléments principaux */
private val VerticalSpacing = 16.dp

/** Padding autour du contenu */
private val ContentPadding = 32.dp

/**
 * Écran d'accueil de KeyBinderTv.
 * Affiche le statut du service, les compteurs de mappings et les boutons de navigation.
 *
 * @param navController Contrôleur de navigation
 * @param repository Repository des mappings
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(repository)
    )
    val uiState: HomeViewModel.HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    HomeContent(
        uiState = uiState,
        onNavigateMappings = { navController.navigate(Screen.MappingList.route) },
        onNavigateSettings = { navController.navigate(Screen.Settings.route) },
        onNavigateImportExport = { navController.navigate(Screen.ImportExport.route) },
        onSupportDeveloper = {
            val uri = Uri.parse(context.getString(R.string.support_url))
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    )
}

/** Contenu stateless de l'écran d'accueil */
@Composable
private fun HomeContent(
    uiState: HomeViewModel.HomeUiState,
    onNavigateMappings: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateImportExport: () -> Unit,
    onSupportDeveloper: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppTitle()
        Spacer(modifier = Modifier.height(VerticalSpacing))
        StatusIndicator(isActive = uiState.isServiceActive)
        Spacer(modifier = Modifier.height(VerticalSpacing))

        if (!uiState.isServiceActive) {
            EnableServiceButton()
            Spacer(modifier = Modifier.height(VerticalSpacing))
        }

        MappingsCountText(
            active = uiState.activeMappingsCount,
            total = uiState.totalMappingsCount
        )
        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        NavigationButtons(
            onNavigateMappings = onNavigateMappings,
            onNavigateSettings = onNavigateSettings,
            onNavigateImportExport = onNavigateImportExport
        )
        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        SupportButton(onClick = onSupportDeveloper)
    }
}

/** Titre principal de l'application */
@Composable
private fun AppTitle() {
    Text(
        text = stringResource(R.string.home_title),
        style = MaterialTheme.typography.displayMedium
    )
}

/** Bouton d'ouverture des paramètres d'accessibilité Android */
@Composable
private fun EnableServiceButton() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }) {
        Text(text = stringResource(R.string.home_enable_service))
    }
}

/** Texte indiquant le nombre de mappings actifs */
@Composable
private fun MappingsCountText(active: Int, total: Int) {
    Text(
        text = stringResource(R.string.home_mappings_count, active, total),
        style = MaterialTheme.typography.bodyLarge
    )
}

/** Bouton Buy Me a Coffee — lien vers la page de soutien */
@Composable
private fun SupportButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = stringResource(R.string.support_button))
    }
}

/** Boutons de navigation vers les autres écrans */
@Composable
private fun NavigationButtons(
    onNavigateMappings: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateImportExport: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        Button(onClick = onNavigateMappings) {
            Text(text = stringResource(R.string.home_manage_mappings))
        }
        Button(onClick = onNavigateSettings) {
            Text(text = stringResource(R.string.home_settings))
        }
        Button(onClick = onNavigateImportExport) {
            Text(text = stringResource(R.string.home_import_export))
        }
    }
}
