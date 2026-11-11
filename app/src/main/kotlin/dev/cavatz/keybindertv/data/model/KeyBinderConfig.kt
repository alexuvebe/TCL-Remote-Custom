package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.Serializable

/** Configuración global persistida. */
@Serializable
data class KeyBinderConfig(
    val version: Int = 1,
    val mappings: List<KeyMapping> = emptyList(),
    val showOverlayNotification: Boolean = true,
    val longPressThresholdMs: Long = 500L,
    val isGloballyEnabled: Boolean = true
)
