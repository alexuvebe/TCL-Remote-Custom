package dev.cavatz.keybindertv.data.repository

import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import kotlinx.coroutines.flow.Flow

interface MappingRepository {
    val configFlow: Flow<KeyBinderConfig>
    suspend fun addMapping(mapping: KeyMapping)
    suspend fun updateMapping(mapping: KeyMapping)
    suspend fun removeMapping(id: String)
    suspend fun toggleMapping(id: String, enabled: Boolean)
    suspend fun toggleGlobal(enabled: Boolean)
    suspend fun updateSettings(showOverlay: Boolean, longPressThresholdMs: Long)
    suspend fun replaceConfig(config: KeyBinderConfig)
    fun getConfigSync(): KeyBinderConfig
}
