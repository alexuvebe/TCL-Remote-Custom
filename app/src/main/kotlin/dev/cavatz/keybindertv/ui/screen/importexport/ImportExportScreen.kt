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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.component.QrCodeDisplay
import dev.cavatz.keybindertv.ui.theme.ActionBlue
import dev.cavatz.keybindertv.ui.theme.ActionGreen
import dev.cavatz.keybindertv.ui.theme.ActionYellow

/** Espacement vertical standard entre les éléments */
private val VerticalSpacing = 12.dp

/** Padding global autour du contenu */
private val ContentPadding = 24.dp

/** Taille du bloc QR code affiché */
private val QrCodeSize = 256.dp

/**
 * Écran Import/Export de la configuration KeyBinderTv.
 * Permet d'exporter la config en JSON (fichier ou QR code) et d'importer depuis un fichier ou JSON collé.
 *
 * @param repository Repository transmis pour alimenter le ViewModel
 */
@Composable
fun ImportExportScreen(repository: MappingRepository) {
    val viewModel: ImportExportViewModel = viewModel(
        factory = ImportExportViewModel.factory(repository)
    )
    val uiState: ImportExportViewModel.ImportExportUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher SAF — création d'un fichier JSON
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToFile(context, it) } }

    // Launcher SAF — ouverture d'un fichier JSON
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromFile(context, it) } }

    ImportExportContent(
        uiState = uiState,
        onExportFile = { exportLauncher.launch("keybinder_config.json") },
        onGenerateQr = { viewModel.generateQrCode() },
        onImportFile = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        onImportJson = { json -> viewModel.importFromJson(json) }
    )
}

/**
 * Contenu stateless de l'écran Import/Export.
 * Séparation screen/content pour faciliter les previews et les tests.
 */
@Composable
private fun ImportExportContent(
    uiState: ImportExportViewModel.ImportExportUiState,
    onExportFile: () -> Unit,
    onGenerateQr: () -> Unit,
    onImportFile: () -> Unit,
    onImportJson: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ContentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ScreenTitle()
        Spacer(modifier = Modifier.height(VerticalSpacing))
        StatusMessageRow(message = uiState.statusMessage)
        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        ExportSection(
            config = uiState.currentConfig,
            qrBitmap = uiState.qrBitmap,
            isQrTooLarge = uiState.isQrTooLarge,
            onExportFile = onExportFile,
            onGenerateQr = onGenerateQr
        )
        Spacer(modifier = Modifier.height(VerticalSpacing * 2))
        ImportSection(
            onImportFile = onImportFile,
            onImportJson = onImportJson
        )
    }
}

/** Titre de l'écran */
@Composable
private fun ScreenTitle() {
    Text(
        text = stringResource(R.string.importexport_title),
        style = MaterialTheme.typography.headlineMedium
    )
}

/**
 * Affiche le message de statut courant (succès ou erreur).
 *
 * @param message Message à afficher, vide si aucun message
 */
@Composable
private fun StatusMessageRow(message: String) {
    if (message.isNotBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Section Export : bouton fichier JSON + bloc QR code.
 *
 * @param config      Configuration courante
 * @param qrBitmap    Bitmap QR généré, null si non généré
 * @param isQrTooLarge Vrai si la config dépasse la limite du QR
 * @param onExportFile Callback export fichier
 * @param onGenerateQr Callback génération QR
 */
@Composable
private fun ExportSection(
    config: KeyBinderConfig,
    qrBitmap: Bitmap?,
    isQrTooLarge: Boolean,
    onExportFile: () -> Unit,
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
        Row(horizontalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
            Button(onClick = onExportFile, colors = ButtonDefaults.buttonColors(containerColor = ActionGreen, contentColor = Color.White)) {
                Text(text = stringResource(R.string.importexport_export_file))
            }
            Button(onClick = onGenerateQr, colors = ButtonDefaults.buttonColors(containerColor = ActionYellow, contentColor = Color(0xFF202020))) {
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

/**
 * Bloc d'affichage du QR code ou des messages associés.
 *
 * @param config       Configuration courante
 * @param qrBitmap     Bitmap QR généré
 * @param isQrTooLarge Vrai si la config est trop grande
 */
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

/**
 * Section Import : bouton fichier + zone de texte pour coller du JSON.
 *
 * @param onImportFile Callback import depuis fichier
 * @param onImportJson Callback import depuis JSON collé
 */
@Composable
private fun ImportSection(
    onImportFile: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var pastedJson: String by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        Text(
            text = stringResource(R.string.importexport_section_import),
            style = MaterialTheme.typography.titleMedium
        )
        Button(onClick = onImportFile, colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White)) {
            Text(text = stringResource(R.string.importexport_import_file))
        }
        OutlinedTextField(
            value = pastedJson,
            onValueChange = { pastedJson = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text(text = stringResource(R.string.importexport_import_paste_hint)) },
            maxLines = 8
        )
        Button(
            onClick = { onImportJson(pastedJson) },
            enabled = pastedJson.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen, contentColor = Color.White)
        ) {
            Text(text = stringResource(R.string.importexport_import_button))
        }
    }
}
