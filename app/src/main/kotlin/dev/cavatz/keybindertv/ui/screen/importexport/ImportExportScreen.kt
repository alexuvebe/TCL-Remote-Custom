package dev.cavatz.keybindertv.ui.screen.importexport

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.ui.component.tvFocusOutline
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.QrCodeDisplay
import dev.cavatz.keybindertv.ui.theme.ActionGreen
import dev.cavatz.keybindertv.ui.theme.ActionYellow

private val VerticalSpacing = 12.dp
private val ContentPadding = 24.dp
private val QrCodeSize = 256.dp

/** Pantalla de copia de seguridad: permite exportar la configuración a JSON o QR. */
@Composable
fun ImportExportScreen(repository: MappingRepository) {
    val viewModel: ImportExportViewModel = viewModel(
        factory = ImportExportViewModel.factory(repository)
    )
    val uiState: ImportExportViewModel.ImportExportUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromFile(context, it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToFile(context, it) } }

    BackupContent(
        uiState = uiState,
        onExportFile = { exportLauncher.launch("keybinder_config.json") },
        onImportFile = { importLauncher.launch(arrayOf("application/json")) },
        onCopyBackup = {
            clipboardManager.setText(AnnotatedString(viewModel.exportToJson()))
            viewModel.setStatusMessage("Copia de seguridad copiada. No desinstales hasta restaurarla.")
        },
        onRestoreClipboard = {
            val backup = clipboardManager.getText()?.text
            if (backup.isNullOrBlank()) {
                viewModel.setStatusMessage("No hay ninguna copia JSON en el portapapeles.")
            } else {
                viewModel.importFromJson(backup)
            }
        },
        onGenerateQr = { viewModel.generateQrCode() }
    )
}

@Composable
private fun BackupContent(
    uiState: ImportExportViewModel.ImportExportUiState,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit,
    onCopyBackup: () -> Unit,
    onRestoreClipboard: () -> Unit,
    onGenerateQr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.importexport_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(VerticalSpacing))
        if (uiState.statusMessage.isNotBlank()) {
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        ExportSection(
            config = uiState.currentConfig,
            qrBitmap = uiState.qrBitmap,
            isQrTooLarge = uiState.isQrTooLarge,
            onExportFile = onExportFile,
            onImportFile = onImportFile,
            onCopyBackup = onCopyBackup,
            onRestoreClipboard = onRestoreClipboard,
            onGenerateQr = onGenerateQr
        )
    }
}

@Composable
private fun ExportSection(
    config: KeyBinderConfig,
    qrBitmap: Bitmap?,
    isQrTooLarge: Boolean,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit,
    onCopyBackup: () -> Unit,
    onRestoreClipboard: () -> Unit,
    onGenerateQr: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        Text(
            text = stringResource(R.string.importexport_section_export),
            style = MaterialTheme.typography.titleMedium
        )
        if (config.mappings.isEmpty()) {
            Text("¿Vienes de una instalación anterior? Restaura la copia desde el portapapeles.")
            Button(
                modifier = Modifier.tvFocusOutline(),
                onClick = onRestoreClipboard,
                colors = ButtonDefaults.buttonColors(containerColor = ActionYellow, contentColor = Color(0xFF202020))
            ) {
                Text("Restaurar desde portapapeles")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
            Button(
                modifier = Modifier.tvFocusOutline(),
                onClick = onCopyBackup,
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen, contentColor = Color.White)
            ) {
                Text("Copiar copia")
            }
            Button(
                modifier = Modifier.tvFocusOutline(),
                onClick = onExportFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionGreen,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(R.string.importexport_export_file))
            }
            Button(
                modifier = Modifier.tvFocusOutline(),
                onClick = onGenerateQr,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionYellow,
                    contentColor = Color(0xFF202020)
                )
            ) {
                Text(text = stringResource(R.string.importexport_generate_qr))
            }
        }
        QrCodeBlock(
            config = config,
            qrBitmap = qrBitmap,
            isQrTooLarge = isQrTooLarge
        )
    }
}

@Composable
private fun QrCodeBlock(
    config: KeyBinderConfig,
    qrBitmap: Bitmap?,
    isQrTooLarge: Boolean
) {
    when {
        isQrTooLarge -> {
            Text(
                text = stringResource(R.string.importexport_qr_too_large),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        qrBitmap != null -> {
            QrCodeDisplay(
                config = config,
                modifier = Modifier.size(QrCodeSize)
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.importexport_qr_prompt),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}