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

class AppChooserViewModel(
    private val repository: MappingRepository,
    private val appsProvider: InstalledAppsProvider,
    private val mappingId: String,
    private val pressType: String
) : ViewModel() {

    data class AppChooserUiState(
        val apps: List<InstalledAppsProvider.AppInfo> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(AppChooserUiState())
    val uiState: StateFlow<AppChooserUiState> = _uiState.asStateFlow()
    private var allApps: List<InstalledAppsProvider.AppInfo> = emptyList()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { appsProvider.getInstalledApps() }
            allApps = apps
            _uiState.update { it.copy(apps = apps, isLoading = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = if (query.isBlank()) allApps else allApps.filter { app ->
            app.appLabel.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }
        _uiState.update { it.copy(searchQuery = query, apps = filtered) }
    }

    suspend fun selectApp(app: InstalledAppsProvider.AppInfo) {
        val config = repository.configFlow.first()
        val existing = config.mappings.firstOrNull { it.id == mappingId } ?: return
        val target = ActionTarget(app.packageName, app.appLabel)
        val updated = when (pressType) {
            "long" -> existing.copy(longPressAction = target, longPressUseDefault = false)
            else -> existing.copy(shortPressAction = target, shortPressUseDefault = false)
        }
        repository.updateMapping(updated)
    }

    /** Marca solo el tipo de pulsación actual como acción de fábrica. */
    suspend fun selectFactory() {
        val config = repository.configFlow.first()
        val existing = config.mappings.firstOrNull { it.id == mappingId } ?: return
        val updated = when (pressType) {
            "long" -> existing.copy(longPressAction = null, longPressUseDefault = true)
            else -> existing.copy(shortPressAction = null, shortPressUseDefault = true)
        }
        repository.updateMapping(updated)
    }

    companion object {
        fun factory(
            repository: MappingRepository,
            appsProvider: InstalledAppsProvider,
            mappingId: String,
            pressType: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppChooserViewModel(repository, appsProvider, mappingId, pressType)
            }
        }
    }
}
