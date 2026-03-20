package dev.cavatz.keybindertv.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.KeyBinderTvApp
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.service.KeyInterceptorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Intervalle de vérification de l'état du service en millisecondes */
private const val SERVICE_POLL_INTERVAL_MS: Long = 2_000L

/**
 * ViewModel de l'écran d'accueil.
 * Expose l'état du service et le compteur de mappings actifs.
 *
 * @param repository Source de données des mappings
 * @param application Contexte applicatif pour AndroidViewModel
 */
class HomeViewModel(
    private val repository: MappingRepository,
    application: Application
) : AndroidViewModel(application) {

    /** État UI de l'écran d'accueil */
    data class HomeUiState(
        val isServiceActive: Boolean = false,
        val activeMappingsCount: Int = 0,
        val totalMappingsCount: Int = 0
    )

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeMappings()
        pollServiceStatus()
    }

    /** Observe le flux de configuration pour mettre à jour les compteurs */
    private fun observeMappings() {
        repository.configFlow
            .onEach { config: KeyBinderConfig ->
                _uiState.update { current ->
                    current.copy(
                        activeMappingsCount = config.mappings.count { it.isEnabled },
                        totalMappingsCount = config.mappings.size
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Vérifie périodiquement si le service d'accessibilité est actif */
    private fun pollServiceStatus() {
        viewModelScope.launch {
            while (true) {
                val isActive: Boolean = KeyInterceptorService.instance != null
                _uiState.update { current ->
                    current.copy(isServiceActive = isActive)
                }
                delay(SERVICE_POLL_INTERVAL_MS)
            }
        }
    }

    companion object {
        /** Factory pour injection du repository sans framework DI */
        fun factory(repository: MappingRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as KeyBinderTvApp
                    HomeViewModel(repository, app)
                }
            }
    }
}
