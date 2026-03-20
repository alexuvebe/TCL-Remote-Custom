package dev.cavatz.keybindertv.ui.screen.appchooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.data.model.ActionTarget
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.util.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel du sélecteur d'application.
 * Charge la liste des apps installées et filtre selon la recherche.
 *
 * @param repository Repository des mappings
 * @param appsProvider Fournisseur de la liste des apps installées
 * @param mappingId Identifiant du mapping à modifier
 * @param pressType Type d'appui — "short" ou "long"
 */
class AppChooserViewModel(
    private val repository: MappingRepository,
    private val appsProvider: InstalledAppsProvider,
    private val mappingId: String,
    private val pressType: String
) : ViewModel() {

    /** État UI du sélecteur d'application */
    data class AppChooserUiState(
        val apps: List<InstalledAppsProvider.AppInfo> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = true
    )

    private val _uiState: MutableStateFlow<AppChooserUiState> =
        MutableStateFlow(AppChooserUiState())
    val uiState: StateFlow<AppChooserUiState> = _uiState.asStateFlow()

    /** Liste complète des apps — source de vérité pour le filtrage */
    private var allApps: List<InstalledAppsProvider.AppInfo> = emptyList()

    init {
        loadApps()
    }

    /** Charge les apps installées en arrière-plan */
    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                appsProvider.getInstalledApps()
            }
            allApps = apps
            _uiState.update { current ->
                current.copy(apps = apps, isLoading = false)
            }
        }
    }

    /** Met à jour la query et filtre la liste en temps réel */
    fun onSearchQueryChanged(query: String) {
        val filtered: List<InstalledAppsProvider.AppInfo> = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appLabel.contains(query, ignoreCase = true)
                        || app.packageName.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { current ->
            current.copy(searchQuery = query, apps = filtered)
        }
    }

    /**
     * Sélectionne une application et met à jour le mapping correspondant.
     * La navigation en arrière est gérée par l'écran après l'appel.
     */
    suspend fun selectApp(app: InstalledAppsProvider.AppInfo) {
        val config = repository.configFlow.first()
        val existingMapping: KeyMapping = config.mappings
            .firstOrNull { it.id == mappingId } ?: return

        val actionTarget = ActionTarget(
            packageName = app.packageName,
            appLabel = app.appLabel
        )

        val updatedMapping: KeyMapping = when (pressType) {
            "short" -> existingMapping.copy(shortPressAction = actionTarget)
            "long" -> existingMapping.copy(longPressAction = actionTarget)
            else -> existingMapping.copy(shortPressAction = actionTarget)
        }

        repository.updateMapping(updatedMapping)
    }

    companion object {
        /** Factory pour injection des dépendances sans framework DI */
        fun factory(
            repository: MappingRepository,
            appsProvider: InstalledAppsProvider,
            mappingId: String,
            pressType: String
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppChooserViewModel(repository, appsProvider, mappingId, pressType)
                }
            }
    }
}
