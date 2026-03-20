package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/**
 * Identifiant unique d'un bouton de télécommande.
 *
 * @param keyCode     Code Android du keyEvent (peut être null si inconnu)
 * @param sourcePackage Package source émettant l'événement (ex: com.tcl.launcher)
 * @param userLabel   Libellé lisible défini par l'utilisateur
 */
@Serializable
data class ButtonIdentifier(
    val keyCode: Int? = null,
    val sourcePackage: String? = null,
    val userLabel: String = ""
)
