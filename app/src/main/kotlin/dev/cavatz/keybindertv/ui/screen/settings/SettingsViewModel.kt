package dev.cavatz.keybindertv.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.service.KeyInterceptorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Valeur minimale du seuil de long press en millisecondes */
const val LONG_PRESS_MIN_MS: Long = 200L

/** Valeur maximale du seuil de long press en millisecondes */
const val LONG_PRESS_MAX_MS: Long = 2_000L

/** Pas d'incrémentation du seuil de long press en millisecondes */
const val LONG_PRESS_STEP_MS: Long = 100L

/** Délai de debounce avant la persistance du seuil de long press */
private const val DEBOUNCE_MS: Long = 500L

/** Intervalle de vérification du service */
private const val SERVICE_POLL_INTERVAL_MS: Long = 2_000L

/**
 * ViewModel de l'écran des paramètres.
 * Gère l'état des préférences et leur persistance via le repository.
 *
 * @param repository Repository des mappings et de la configuration
 */
class SettingsViewModel(
    private val repository: MappingRepository
) : ViewModel() {

    /** État UI de l'écran des paramètres */
    data class SettingsUiState(
        val showOverlayNotification: Boolean = true,
        val longPressThresholdMs: Long = 500L,
        val isServiceActive: Boolean = false
    )

    private val _uiState: MutableStateFlow<SettingsUiState> =
        MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Job de debounce pour la persistance du seuil */
    private var thresholdDebounceJob: Job? = null

    init {
        observeConfig()
        pollServiceStatus()
    }

    /** Observe la configuration pour initialiser l'état UI */
    private fun observeConfig() {
        repository.configFlow
            .onEach { config: KeyBinderConfig ->
                _uiState.update { current ->
                    current.copy(
                        showOverlayNotification = config.showOverlayNotification,
                        longPressThresholdMs = config.longPressThresholdMs
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Vérifie périodiquement l'état du service */
    private fun pollServiceStatus() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { current ->
                    current.copy(isServiceActive = KeyInterceptorService.instance != null)
                }
                delay(SERVICE_POLL_INTERVAL_MS)
            }
        }
    }

    /** Active ou désactive la notification overlay lors du remapping */
    fun setShowOverlay(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(
                showOverlay = enabled,
                longPressThresholdMs = _uiState.value.longPressThresholdMs
            )
        }
    }

    /**
     * Met à jour le seuil de long press avec debounce.
     * La persistance est retardée pour éviter les écritures trop fréquentes.
     */
    fun setLongPressThreshold(thresholdMs: Long) {
        val clamped: Long = thresholdMs.coerceIn(LONG_PRESS_MIN_MS, LONG_PRESS_MAX_MS)
        _uiState.update { current -> current.copy(longPressThresholdMs = clamped) }

        thresholdDebounceJob?.cancel()
        thresholdDebounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            repository.updateSettings(
                showOverlay = _uiState.value.showOverlayNotification,
                longPressThresholdMs = clamped
            )
        }
    }

    companion object {
        /** Factory pour injection du repository sans framework DI */
        fun factory(repository: MappingRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(repository)
                }
            }
    }
}
