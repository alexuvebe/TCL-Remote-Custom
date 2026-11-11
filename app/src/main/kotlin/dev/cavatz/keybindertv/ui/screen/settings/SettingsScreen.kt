package dev.cavatz.keybindertv.ui.screen.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.KeyBinderTvApp
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.backup.GitHubBackupManager
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.StatusIndicator
import dev.cavatz.keybindertv.update.AppUpdateManager
import dev.cavatz.keybindertv.ui.theme.PrimaryBlueDark
import dev.cavatz.keybindertv.ui.theme.SuccessGreen
import dev.cavatz.keybindertv.ui.theme.SurfaceVariantDark
import dev.cavatz.keybindertv.ui.theme.WarningAmber
import kotlinx.coroutines.launch

/** Espacement vertical entre las secciones, conservando la distribución de la versión 155. */
private val SectionSpacing = 12.dp

/** Padding global de la pantalla, igual que en la versión 155. */
private val ScreenPadding = 16.dp

/** Número de pasos del control de pulsación larga. */
private val SLIDER_STEPS: Int = ((LONG_PRESS_MAX_MS - LONG_PRESS_MIN_MS) / LONG_PRESS_STEP_MS - 1).toInt()

@Composable
fun SettingsScreen(
    navController: NavHostController,
    repository: MappingRepository
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
    val uiState: SettingsViewModel.SettingsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val updateScope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf(context.getString(R.string.update_initial_status)) }
    var checkingUpdate by remember { mutableStateOf(false) }
    val backupManager = (context.applicationContext as KeyBinderTvApp).githubBackupManager
    val backupState by backupManager.state.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        backupState = backupState,
        backupManager = backupManager,
        updateStatus = updateStatus,
        checkingUpdate = checkingUpdate,
        onCheckUpdate = {
            val activity = context.findActivity()
            if (activity == null) {
                updateStatus = "No se pudo abrir el instalador desde esta pantalla."
            } else {
                updateScope.launch {
                    checkingUpdate = true
                    updateStatus = context.getString(R.string.update_checking)
                    try {
                        updateStatus = AppUpdateManager.checkAndInstall(activity)
                    } catch (error: Exception) {
                        updateStatus = "Error al buscar la actualización: " + (error.message ?: "desconocido")
                    } finally {
                        checkingUpdate = false
                    }
                }
            }
        },
        onSetShowOverlay = { enabled -> viewModel.setShowOverlay(enabled) },
        onSetLongPressThreshold = { threshold -> viewModel.setLongPressThreshold(threshold) }
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsViewModel.SettingsUiState,
    backupState: GitHubBackupManager.UiState,
    backupManager: GitHubBackupManager,
    updateStatus: String,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onSetShowOverlay: (Boolean) -> Unit,
    onSetLongPressThreshold: (Long) -> Unit
) {
    val context = LocalContext.current

    // Se conserva el orden visual de la versión 155: estado, preferencias y accesibilidad.
    // Las funciones nuevas quedan después, integradas con la misma tipografía y espaciado.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall
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

        // Actualizador incorporado en la versión 202, sin alterar los controles originales.
        Text(
            text = stringResource(R.string.update_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(updateStatus, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCheckUpdate, enabled = !checkingUpdate, colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)) {
            Text(
                if (checkingUpdate) stringResource(R.string.update_checking)
                else stringResource(R.string.update_check_button)
            )
        }
        Spacer(modifier = Modifier.height(SectionSpacing))

        // Copia de seguridad no pública en GitHub, integrada con el estilo existente.
        Text("Copia de seguridad no pública en GitHub", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(backupState.status, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "El Gist no aparece en listados públicos, pero cualquiera que tenga su enlace puede acceder.",
            style = MaterialTheme.typography.bodySmall
        )

        if (backupState.userCode != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Código de autorización", style = MaterialTheme.typography.bodyMedium)
            Text(backupState.userCode, style = MaterialTheme.typography.headlineLarge)
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(backupState.verificationUrl ?: "https://github.com/login/device")
                    )
                )
            }) {
                Text("Abrir GitHub para autorizar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (!backupState.connected) {
            Button(
                onClick = { backupManager.connect() },
                enabled = !backupState.busy
            ) {
                Text(if (backupState.userCode == null) "Conectar con GitHub" else "Solicitar otro código")
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { backupManager.backupNow() },
                    enabled = !backupState.busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Guardar copia")
                }
                Button(
                    onClick = { backupManager.restoreNow() },
                    enabled = !backupState.busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark)
                ) {
                    Text("Restaurar copia")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { backupManager.disconnect() },
                enabled = !backupState.busy,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark)
            ) {
                Text("Desconectar GitHub")
            }
        }
    }
}

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

@Composable
private fun AccessibilityButton() {
    val context = LocalContext.current
    Button(onClick = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }) {
        Text(text = stringResource(R.string.settings_accessibility))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
