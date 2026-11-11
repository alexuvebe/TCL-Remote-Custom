package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/** Aplicación asignada a una pulsación. */
@Serializable
data class ActionTarget(
    val packageName: String,
    val appLabel: String,
    val isEnabled: Boolean = true
)
