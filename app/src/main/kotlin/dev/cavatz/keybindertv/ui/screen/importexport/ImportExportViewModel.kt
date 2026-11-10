package dev.cavatz.keybindertv.ui.screen.importexport

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.data.export.ConfigExporter
import dev.cavatz.keybindertv.data.export.ConfigImporter
import dev.cavatz.keybindertv.data.export.QrCodeCodec
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran Import/Export.
 * Gère la génération de QR code, l'export fichier JSON et l'import depuis fichier ou JSON collé.
 *
 * @param repository Repository des mappings (source de vérité)
 * @param exporter   Composant d'export JSON
 * @param importer   Composant d'import et de validation JSON
 * @param qrCodec    Codec pour la génération et la lecture de QR codes
 */
class ImportExportViewModel(
    private val repository: MappingRepository,
    private val exporter: ConfigExporter = ConfigExporter(),
    private val importer: ConfigImporter = ConfigImporter(),
    private val qrCodec: QrCodeCodec = QrCodeCodec()
) : ViewModel() {

    /**
     * État de l'interface utilisateur de l'écran Import/Export.
     *
     * @param currentConfig       Configuration courante chargée depuis le repository
     * @param qrBitmap            Bitmap du QR code généré, null si non encore généré
     * @param isQrTooLarge        Vrai si la config dépasse la capacité du QR code
     * @param exportedJsonPreview Aperçu du JSON exporté (prévisualisation)
     * @param statusMessage       Message de statut affiché à l'utilisateur
     * @param isLoading           Vrai pendant les opérations asynchrones
     */
    data class ImportExportUiState(
        val currentConfig: KeyBinderConfig = KeyBinderConfig(),
        val qrBitmap: Bitmap? = null,
        val isQrTooLarge: Boolean = false,
        val exportedJsonPreview: String = "",
        val statusMessage: String = "",
        val isLoading: Boolean = false
    )

    private val _uiState: MutableStateFlow<ImportExportUiState> =
        MutableStateFlow(ImportExportUiState())

    /** Flux d'état observable par l'écran Compose */
    val uiState: StateFlow<ImportExportUiState> = _uiState.asStateFlow()

    init {
        // Synchronisation continue avec la configuration persistée
        viewModelScope.launch {
            repository.configFlow.collect { config ->
                _uiState.update { it.copy(currentConfig = config) }
            }
        }
    }

    /**
     * Génère le QR code de la configuration courante.
     * Met à jour [ImportExportUiState.qrBitmap] ou [ImportExportUiState.isQrTooLarge].
     */
    fun generateQrCode() {
        val config: KeyBinderConfig = _uiState.value.currentConfig
        val bitmap: Bitmap? = qrCodec.encode(config)
        _uiState.update { state ->
            state.copy(
                qrBitmap = bitmap,
                isQrTooLarge = bitmap == null,
                statusMessage = if (bitmap == null) {
                    "Configuration trop grande — utilisez l'export fichier"
                } else {
                    "QR code généré"
                }
            )
        }
    }

    /**
     * Exporte la configuration courante dans un fichier via SAF.
     * Met à jour le message de statut selon le résultat.
     *
     * @param context Contexte Android requis pour le ContentResolver
     * @param uri     URI de destination fourni par ACTION_CREATE_DOCUMENT
     */
    fun exportToJson(): String = exporter.exportToJson(_uiState.value.currentConfig)

    fun setStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun exportToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "") }
            try {
                val config: KeyBinderConfig = _uiState.value.currentConfig
                exporter.exportToFile(context, uri, config)
                val preview: String = exporter.exportToJson(config)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        exportedJsonPreview = preview,
                        statusMessage = "Configuration exportée avec succès"
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        statusMessage = "Erreur d'export : ${exception.message}"
                    )
                }
            }
        }
    }

    /**
     * Importe une configuration depuis un fichier via SAF.
     * En cas de succès, applique la configuration importée au repository.
     *
     * @param context Contexte Android requis pour le ContentResolver
     * @param uri     URI source fourni par ACTION_OPEN_DOCUMENT
     */
    fun importFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "") }
            val result: ConfigImporter.ImportResult = importer.importFromFile(context, uri)
            applyImportResult(result)
        }
    }

    /**
     * Importe une configuration depuis une chaîne JSON collée directement.
     *
     * @param jsonString Contenu JSON à parser et appliquer
     */
    fun importFromJson(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "") }
            val result: ConfigImporter.ImportResult = importer.importFromJson(jsonString)
            applyImportResult(result)
        }
    }

    /**
     * Applique le résultat d'un import au repository et met à jour l'état.
     *
     * @param result Résultat de l'import à traiter
     */
    private suspend fun applyImportResult(result: ConfigImporter.ImportResult) {
        when (result) {
            is ConfigImporter.ImportResult.Success -> {
                applyImportedConfig(result.config)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        statusMessage = "Configuration importée — ${result.config.mappings.size} mapping(s)"
                    )
                }
            }
            is ConfigImporter.ImportResult.Error -> {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        statusMessage = "Erreur d'import : ${result.message}"
                    )
                }
            }
        }
    }

    /**
     * Applique la configuration importée au repository.
     * Supprime les anciens mappings et ajoute les nouveaux.
     *
     * @param config Configuration importée à appliquer
     */
    private suspend fun applyImportedConfig(config: KeyBinderConfig) {
        val currentMappings = _uiState.value.currentConfig.mappings
        currentMappings.forEach { mapping -> repository.removeMapping(mapping.id) }
        config.mappings.forEach { mapping -> repository.addMapping(mapping) }
        repository.updateSettings(config.showOverlayNotification, config.longPressThresholdMs)
    }

    companion object {
        /**
         * Factory pour instancier le ViewModel avec le repository injecté.
         *
         * @param repository Repository des mappings
         */
        fun factory(repository: MappingRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ImportExportViewModel(repository) }
            }
    }
}
