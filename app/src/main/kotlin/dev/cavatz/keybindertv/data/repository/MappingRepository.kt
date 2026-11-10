package dev.cavatz.keybindertv.data.repository

import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import kotlinx.coroutines.flow.Flow

/**
 * Contrat d'accès aux mappings clavier.
 * Toutes les opérations de mutation sont suspendantes sauf getConfigSync
 * qui est destinée au service d'accessibilité (appelé hors coroutine).
 */
interface MappingRepository {

    /** Flux réactif de la configuration complète. Émet à chaque modification. */
    val configFlow: Flow<KeyBinderConfig>

    /** Ajoute un nouveau mapping. Lève IllegalArgumentException si l'id existe déjà. */
    suspend fun addMapping(mapping: KeyMapping)

    /** Met à jour un mapping existant par son id. */
    suspend fun updateMapping(mapping: KeyMapping)

    /** Supprime le mapping identifié par [id]. Silencieux si l'id est inconnu. */
    suspend fun removeMapping(id: String)

    /** Active ou désactive le mapping identifié par [id]. */
    suspend fun toggleMapping(id: String, enabled: Boolean)

    /** Met à jour les paramètres globaux de la configuration. */
    suspend fun updateSettings(showOverlay: Boolean, longPressThresholdMs: Long)

    /** Reemplaza la configuración completa al restaurar una copia validada. */
    suspend fun replaceConfig(config: KeyBinderConfig)

    /**
     * Retourne la configuration courante de manière synchrone.
     * Utilise un cache AtomicReference mis à jour en background.
     * À utiliser uniquement depuis le service d'accessibilité.
     */
    fun getConfigSync(): KeyBinderConfig
}
