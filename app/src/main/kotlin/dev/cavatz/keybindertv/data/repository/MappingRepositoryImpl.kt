package dev.cavatz.keybindertv.data.repository

import androidx.datastore.core.DataStore
import android.util.Log
import androidx.datastore.core.Serializer
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Sérialiseur DataStore utilisant kotlinx.serialization-json.
 * Utilisé pour lire/écrire [KeyBinderConfig] depuis/vers le fichier DataStore.
 */
object KeyBinderConfigSerializer : Serializer<KeyBinderConfig> {

    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val defaultValue: KeyBinderConfig = KeyBinderConfig()

    override suspend fun readFrom(input: InputStream): KeyBinderConfig {
        return try {
            json.decodeFromString(
                deserializer = KeyBinderConfig.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (exception: Exception) {
            // En cas de corruption, on repart avec la config par défaut
            defaultValue
        }
    }

    override suspend fun writeTo(t: KeyBinderConfig, output: OutputStream) {
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), t)
        output.write(encoded.encodeToByteArray())
    }
}

/**
 * Implémentation concrète de [MappingRepository].
 *
 * Stratégie de cache synchrone :
 * - Un [AtomicReference] est maintenu à jour par un collecteur Flow lancé dans [scope].
 * - [getConfigSync] lit ce cache sans suspension, adapté au service d'accessibilité.
 *
 * @param dataStore DataStore persistant la configuration JSON
 * @param scope     Scope de coroutine pour le collecteur de cache background
 */
class MappingRepositoryImpl(
    private val dataStore: DataStore<KeyBinderConfig>,
    scope: CoroutineScope
) : MappingRepository {

    /** Cache synchrone mis à jour en background via le Flow DataStore. */
    private val configCache: AtomicReference<KeyBinderConfig> =
        AtomicReference(KeyBinderConfig())

    override val configFlow: Flow<KeyBinderConfig> = dataStore.data
        .retryWhen { cause, attempt ->
            Log.e("MappingRepository", "Error leyendo configuración; se reintentará", cause)
            delay((1000L * (attempt + 1)).coerceAtMost(10000L))
            true
        }
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    init {
        // Lance le collecteur de cache en background dès la création du repository
        scope.launch {
            configFlow.collect { config ->
                configCache.set(config)
            }
        }
    }

    override suspend fun addMapping(mapping: KeyMapping) {
        dataStore.updateData { current ->
            require(current.mappings.none { it.id == mapping.id }) {
                "Un mapping avec l'id '${mapping.id}' existe déjà"
            }
            current.copy(mappings = current.mappings + mapping)
        }
    }

    override suspend fun updateMapping(mapping: KeyMapping) {
        dataStore.updateData { current ->
            val updatedMappings: List<KeyMapping> = current.mappings.map { existing ->
                if (existing.id == mapping.id) mapping else existing
            }
            current.copy(mappings = updatedMappings)
        }
    }

    override suspend fun removeMapping(id: String) {
        dataStore.updateData { current ->
            current.copy(mappings = current.mappings.filter { it.id != id })
        }
    }

    override suspend fun toggleMapping(id: String, enabled: Boolean) {
        dataStore.updateData { current ->
            val updatedMappings: List<KeyMapping> = current.mappings.map { mapping ->
                if (mapping.id == id) mapping.copy(isEnabled = enabled) else mapping
            }
            current.copy(mappings = updatedMappings)
        }
    }

    override suspend fun updateSettings(showOverlay: Boolean, longPressThresholdMs: Long) {
        dataStore.updateData { current ->
            current.copy(
                showOverlayNotification = showOverlay,
                longPressThresholdMs = longPressThresholdMs
            )
        }
    }

    override suspend fun replaceConfig(config: KeyBinderConfig) {
        dataStore.updateData { config }
    }

    override fun getConfigSync(): KeyBinderConfig = configCache.get()
}
