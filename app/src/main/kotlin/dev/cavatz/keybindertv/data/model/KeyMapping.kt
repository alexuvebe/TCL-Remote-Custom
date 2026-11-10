package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/**
 * Asociación entre un botón físico y sus acciones para pulsación corta y larga.
 * Los indicadores "UseDefault" distinguen la acción de fábrica de una acción
 * sin configurar. Sus valores por defecto mantienen compatibilidad con copias antiguas.
 */
@Serializable
data class KeyMapping(
    val id: String,
    val button: ButtonIdentifier,
    val shortPressAction: ActionTarget? = null,
    val longPressAction: ActionTarget? = null,
    val isEnabled: Boolean = true,
    val shortPressUseDefault: Boolean = false,
    val longPressUseDefault: Boolean = false
)
