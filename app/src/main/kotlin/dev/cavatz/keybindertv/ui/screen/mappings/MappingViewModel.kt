package dev.cavatz.keybindertv.ui.screen.mappings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel de la liste des mappings.
 * Gère les opérations CRUD sur les mappings via le repository.
 *
 * @param repository Source de données des mappings
 */
class MappingViewModel(
    private val repository: MappingRepository
) : ViewModel() {

    /** État UI de la liste des mappings */
    data class MappingListUiState(
        val mappings: List<KeyMapping> = emptyList(),
        val isLoading: Boolean = false
    )

    private val _uiState: MutableStateFlow<MappingListUiState> =
        MutableStateFlow(MappingListUiState(isLoading = true))
    val uiState: StateFlow<MappingListUiState> = _uiState.asStateFlow()

    init {
        observeMappings()
    }

    /** Observe le flux de configuration pour mettre à jour la liste */
    private fun observeMappings() {
        repository.configFlow
            .onEach { config: KeyBinderConfig ->
                _uiState.update { current ->
                    current.copy(
                        mappings = config.mappings,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Active ou désactive un mapping par son identifiant */
    fun toggleMapping(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMapping(id, enabled)
        }
    }

    /** Activa/desactiva de forma independiente cada tipo de pulsación. */
    fun toggleShortPress(id: String, enabled: Boolean) = updatePress(id) {
        copy(shortPressEnabled = enabled)
    }

    fun toggleLongPress(id: String, enabled: Boolean) = updatePress(id) {
        copy(longPressEnabled = enabled)
    }

    /** La X borra solo la asignación elegida y devuelve esa pulsación a fábrica. */
    fun clearShortPress(id: String) = updatePress(id) {
        copy(shortPressAction = null, shortPressUseDefault = true)
    }

    fun clearLongPress(id: String) = updatePress(id) {
        copy(longPressAction = null, longPressUseDefault = true)
    }

    private fun updatePress(id: String, change: KeyMapping.() -> KeyMapping) {
        viewModelScope.launch {
            val current = repository.configFlow.first().mappings.firstOrNull { it.id == id } ?: return@launch
            repository.updateMapping(current.change())
        }
    }

    /** Supprime un mapping par son identifiant */
    fun removeMapping(id: String) {
        viewModelScope.launch {
            repository.removeMapping(id)
        }
    }

    /**
     * Crée un nouveau mapping vide avec un UUID généré.
     * Retourne l'identifiant du mapping créé pour navigation vers LearningScreen.
     */
    suspend fun createNewMapping(): String {
        val newId: String = UUID.randomUUID().toString()
        val emptyMapping = KeyMapping(
            id = newId,
            button = ButtonIdentifier(userLabel = ""),
            shortPressAction = null,
            longPressAction = null,
            isEnabled = true,
            shortPressUseDefault = true,
            longPressUseDefault = true
        )
        repository.addMapping(emptyMapping)
        return newId
    }

    companion object {
        /** Factory pour injection du repository sans framework DI */
        fun factory(repository: MappingRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MappingViewModel(repository)
                }
            }
    }
}
