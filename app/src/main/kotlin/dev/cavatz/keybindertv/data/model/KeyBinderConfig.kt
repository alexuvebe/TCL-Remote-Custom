package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/**
 * Configuration globale de l'application, persistée via DataStore.
 *
 * @param version                Version du schéma de configuration (migration future)
 * @param mappings               Liste de tous les remappages définis par l'utilisateur
 * @param showOverlayNotification Afficher une notification lors d'un remapping actif
 * @param longPressThresholdMs   Durée minimale en ms pour déclencher un appui long
 */
@Serializable
data class KeyBinderConfig(
    val version: Int = 1,
    val mappings: List<KeyMapping> = emptyList(),
    val showOverlayNotification: Boolean = true,
    val longPressThresholdMs: Long = 500L,
    /** True when local storage was recovered from a damaged file; blocks automatic cloud overwrite. */
    val backupRecoveryPending: Boolean = false
)
