package dev.cavatz.keybindertv.ui.screen.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.cavatz.keybindertv.ui.theme.ActionBlue
import dev.cavatz.keybindertv.ui.theme.ActionPurple
import dev.cavatz.keybindertv.ui.theme.ActionRed
import dev.cavatz.keybindertv.ui.theme.ActionYellow

private val VerticalSpacing = 16.dp
private val ContentPadding = 32.dp

@Composable
fun HomeScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(repository)
    )
    val uiState: HomeViewModel.HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onNavigateMappings = { navController.navigate(Screen.MappingList.route) },
        onNavigateSettings = { navController.navigate(Screen.Settings.route) },
        onNavigateBackup = { navController.navigate(Screen.ImportExport.route) }
    )
}

@Composable
private fun HomeContent(
    uiState: HomeViewModel.HomeUiState,
    onNavigateMappings: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateBackup: () -> Unit
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
            onNavigateBackup = onNavigateBackup
        )
    }
}

@Composable
private fun AppTitle() {
    Text(
        text = stringResource(R.string.home_title),
        style = MaterialTheme.typography.displayMedium
    )
}

@Composable
private fun EnableServiceButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = ActionRed,
            contentColor = Color.White
        )
    ) {
        Text(text = stringResource(R.string.home_enable_service))
    }
}

@Composable
private fun MappingsCountText(active: Int, total: Int) {
    Text(
        text = stringResource(R.string.home_mappings_count, active, total),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun NavigationButtons(
    onNavigateMappings: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateBackup: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        Button(
            onClick = onNavigateMappings,
            colors = ButtonDefaults.buttonColors(
                containerColor = ActionBlue,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.home_manage_mappings))
        }
        Button(
            onClick = onNavigateSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = ActionPurple,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.home_settings))
        }
        Button(
            onClick = onNavigateBackup,
            colors = ButtonDefaults.buttonColors(
                containerColor = ActionYellow,
                contentColor = Color(0xFF202020)
            )
        ) {
            Text(text = stringResource(R.string.home_import_export))
        }
    }
}