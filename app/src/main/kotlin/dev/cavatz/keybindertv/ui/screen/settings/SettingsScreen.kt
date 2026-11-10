package dev.cavatz.keybindertv.ui.screen.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import dev.cavatz.keybindertv.ui.component.tvFocusOutline
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.KeyBinderTvApp
import dev.cavatz.keybindertv.ui.component.StatusIndicator
import dev.cavatz.keybindertv.ui.theme.ActionPurple

/** Espacement vertical entre les sections de paramètres */
private val SectionSpacing = 24.dp

/** Padding global de l'écran */
private val ScreenPadding = 32.dp

/** Nombre de pas sur le slider de long press */
private val SLIDER_STEPS: Int = ((LONG_PRESS_MAX_MS - LONG_PRESS_MIN_MS) / LONG_PRESS_STEP_MS - 1).toInt()

/**
 * Écran de paramètres globaux de l'application.
 * Permet de configurer l'overlay et le seuil de long press.
 *
 * @param navController Contrôleur de navigation
 * @param repository Repository des mappings
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(repository)
    )
    val uiState: SettingsViewModel.SettingsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onSetShowOverlay = { enabled -> viewModel.setShowOverlay(enabled) },
        onSetLongPressThreshold = { threshold -> viewModel.setLongPressThreshold(threshold) }
    )
}

/** Contenu stateless de l'écran des paramètres */
@Composable
private fun SettingsContent(
    uiState: SettingsViewModel.SettingsUiState,
    onSetShowOverlay: (Boolean) -> Unit,
    onSetLongPressThreshold: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(SectionSpacing))

        StatusIndicator(isActive = uiState.isServiceActive)
        Spacer(modifier = Modifier.height(SectionSpacing))

        OverlayToggle(
            enabled = uiState.showOverlayNotification,
            onToggle = onSetShowOverlay
        )
        Spacer(modifier = Modifier.height(SectionSpacing))

        LongPressSlider(
            thresholdMs = uiState.longPressThresholdMs,
            onThresholdChanged = onSetLongPressThreshold
        )
        Spacer(modifier = Modifier.height(SectionSpacing))

        AccessibilityButton()
        Spacer(modifier = Modifier.height(SectionSpacing))
        UpdateSection()
    }
}

/** Toggle pour la notification overlay */
@Composable
private fun OverlayToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.settings_overlay),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

/** Slider pour configurer le seuil de long press */
@Composable
private fun LongPressSlider(thresholdMs: Long, onThresholdChanged: (Long) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.settings_longpress_threshold, thresholdMs),
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = thresholdMs.toFloat(),
            onValueChange = { value -> onThresholdChanged(value.toLong()) },
            valueRange = LONG_PRESS_MIN_MS.toFloat()..LONG_PRESS_MAX_MS.toFloat(),
            steps = SLIDER_STEPS,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Bouton d'accès aux paramètres d'accessibilité Android */
@Composable
private fun AccessibilityButton() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    }, colors = ButtonDefaults.buttonColors(containerColor = ActionPurple, contentColor = Color.White)) {
        Text(text = stringResource(R.string.settings_accessibility))
    }
}


/** Comprobación manual y descarga de nuevas versiones desde GitHub. */
@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val manager = (context.applicationContext as KeyBinderTvApp).updateManager
    val updateState by manager.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Actualizaciones", style = MaterialTheme.typography.titleLarge)
        Text("Versión instalada: ${updateState.currentVersion}")
        if (updateState.latestVersion.isNotBlank()) {
            Text("Última versión: ${updateState.latestVersion}")
        }
        Text(updateState.message, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                modifier = Modifier.tvFocusOutline(),
                onClick = { manager.checkForUpdates() },
                enabled = !updateState.isChecking && !updateState.isDownloading
            ) {
                Text(if (updateState.isChecking) "Comprobando…" else "Buscar actualizaciones")
            }
            if (updateState.updateAvailable) {
                Button(
                    modifier = Modifier.tvFocusOutline(),
                    onClick = { manager.downloadAndInstall() },
                    enabled = !updateState.isDownloading
                ) {
                    Text(if (updateState.isDownloading) "Descargando…" else "Descargar e instalar")
                }
            } else if (updateState.downloaded) {
                Button(
                    modifier = Modifier.tvFocusOutline(),
                    onClick = { manager.installDownloadedApk() }
                ) {
                    Text("Instalar actualización")
                }
            }
        }
    }
}
