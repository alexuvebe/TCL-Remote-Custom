package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/**
 * Application cible à lancer lors d'un appui remappé.
 *
 * @param packageName Identifiant de paquetage Android (ex: com.google.android.youtube.tv)
 * @param appLabel    Nom d'affichage de l'application
 */
@Serializable
data class ActionTarget(
    val packageName: String,
    val appLabel: String
)
