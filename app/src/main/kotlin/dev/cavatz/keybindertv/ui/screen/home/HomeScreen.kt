package dev.cavatz.keybindertv.ui.screen.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.BuildConfig
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.StatusIndicator
import dev.cavatz.keybindertv.ui.navigation.Screen
import dev.cavatz.keybindertv.ui.theme.ErrorRed
import dev.cavatz.keybindertv.ui.theme.OnSurfaceDark
import dev.cavatz.keybindertv.ui.theme.PrimaryBlueDark
import dev.cavatz.keybindertv.ui.theme.SecondaryPurpleDark

private val VerticalSpacing = 10.dp
private val ContentPadding = 16.dp

@Composable
fun HomeScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val uiState: HomeViewModel.HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onGlobalToggle = viewModel::setGlobalEnabled,
        onNavigateMappings = { navController.navigate(Screen.MappingList.route) },
        onNavigateSettings = { navController.navigate(Screen.Settings.route) }
    )
}

@Composable
private fun HomeContent(
    uiState: HomeViewModel.HomeUiState,
    onGlobalToggle: (Boolean) -> Unit,
    onNavigateMappings: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(VerticalSpacing))
        StatusIndicator(isActive = uiState.isServiceActive)

        Spacer(modifier = Modifier.height(VerticalSpacing))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Remapeo global", style = MaterialTheme.typography.titleMedium)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (uiState.globalEnabled) "Activado" else "Desactivado",
                    style = MaterialTheme.typography.labelSmall
                )
                Switch(
                    checked = uiState.globalEnabled,
                    onCheckedChange = onGlobalToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ErrorRed,
                        checkedTrackColor = ErrorRed.copy(alpha = 0.45f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(VerticalSpacing))
        Text(
            text = stringResource(
                R.string.home_mappings_count,
                uiState.activeMappingsCount,
                uiState.totalMappingsCount
            ),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        Row(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateMappings,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark)
            ) {
                Text(stringResource(R.string.home_manage_mappings), maxLines = 1)
            }
            ServiceAccessButton(
                isServiceActive = uiState.isServiceActive,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onNavigateSettings,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryPurpleDark,
                    contentColor = OnSurfaceDark
                )
            ) {
                Text("⚙", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Versión ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ServiceAccessButton(
    isServiceActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
    ) {
        Text(
            if (isServiceActive) "Servicio activo" else stringResource(R.string.home_enable_service),
            maxLines = 1
        )
    }
}
