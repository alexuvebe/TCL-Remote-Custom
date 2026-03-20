package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/**
 * Association entre un bouton physique et une ou deux actions cibles.
 * Un seul mapping peut couvrir l'appui court ET l'appui long.
 *
 * @param id               Identifiant UUID unique du mapping
 * @param button           Bouton source identifié
 * @param shortPressAction Application lancée sur appui court (null = pas de remapping)
 * @param longPressAction  Application lancée sur appui long (null = pas de remapping)
 * @param isEnabled        Permet de désactiver temporairement sans supprimer
 */
@Serializable
data class KeyMapping(
    val id: String,
    val button: ButtonIdentifier,
    val shortPressAction: ActionTarget? = null,
    val longPressAction: ActionTarget? = null,
    val isEnabled: Boolean = true
)
