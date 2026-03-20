package dev.cavatz.keybindertv.ui.screen.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.service.KeyInterceptorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Durée du countdown en secondes avant timeout */
private const val COUNTDOWN_SECONDS: Int = 15

/** Intervalle d'une seconde en millisecondes */
private const val ONE_SECOND_MS: Long = 1_000L

/**
 * ViewModel du mode apprentissage.
 * Orchestre la détection du bouton via [KeyInterceptorService] avec un countdown.
 * Met à jour le bouton détecté dans le mapping via le repository.
 *
 * @param repository Repository des mappings
 * @param mappingId Identifiant du mapping en cours de création
 */
class LearningViewModel(
    private val repository: MappingRepository,
    private val mappingId: String
) : ViewModel() {

    /** États possibles du mode apprentissage */
    sealed class LearningState {
        /** En attente d'un appui sur un bouton de la télécommande */
        object WaitingForButton : LearningState()

        /** Bouton détecté — en attente de confirmation de l'utilisateur */
        data class ButtonDetected(val identifier: ButtonIdentifier) : LearningState()

        /** Aucun bouton détecté dans le délai imparti */
        object Timeout : LearningState()
    }

    private val _state: MutableStateFlow<LearningState> =
        MutableStateFlow(LearningState.WaitingForButton)
    val state: StateFlow<LearningState> = _state.asStateFlow()

    private val _remainingSeconds: MutableStateFlow<Int> =
        MutableStateFlow(COUNTDOWN_SECONDS)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    /** Job du countdown — annulable si un bouton est détecté */
    private var countdownJob: Job? = null

    init {
        startLearning()
    }

    /** Démarre le mode apprentissage — délègue au service si actif */
    fun startLearning() {
        val service: KeyInterceptorService? = KeyInterceptorService.instance
        if (service == null) {
            // Service inactif — l'UI affiche le message d'erreur via la vérification directe
            return
        }

        _state.value = LearningState.WaitingForButton
        _remainingSeconds.value = COUNTDOWN_SECONDS

        service.startLearningMode { identifier: ButtonIdentifier ->
            countdownJob?.cancel()
            _state.value = LearningState.ButtonDetected(identifier)
        }

        startCountdown()
    }

    /** Lance le countdown — décrémente chaque seconde jusqu'à timeout */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (remaining in COUNTDOWN_SECONDS downTo 1) {
                _remainingSeconds.value = remaining
                delay(ONE_SECOND_MS)
            }
            _remainingSeconds.value = 0
            KeyInterceptorService.instance?.stopLearningMode()
            _state.value = LearningState.Timeout
        }
    }

    /**
     * Confirme le bouton détecté et met à jour le mapping dans le repository.
     * La navigation vers AppChooserScreen est déclenchée par l'écran.
     */
    suspend fun confirmButton() {
        val currentState = _state.value as? LearningState.ButtonDetected ?: return
        val config = repository.configFlow.first()
        val existingMapping = config.mappings.firstOrNull { it.id == mappingId } ?: return
        val updatedMapping = existingMapping.copy(button = currentState.identifier)
        repository.updateMapping(updatedMapping)
    }

    /** Relance le mode apprentissage depuis l'état initial */
    fun retry() {
        countdownJob?.cancel()
        startLearning()
    }

    /** Annule le mode apprentissage et arrête le service */
    fun cancel() {
        countdownJob?.cancel()
        KeyInterceptorService.instance?.stopLearningMode()
    }

    override fun onCleared() {
        cancel()
        super.onCleared()
    }

    companion object {
        /** Factory pour injection des dépendances sans framework DI */
        fun factory(
            repository: MappingRepository,
            mappingId: String
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    LearningViewModel(repository, mappingId)
                }
            }
    }
}
