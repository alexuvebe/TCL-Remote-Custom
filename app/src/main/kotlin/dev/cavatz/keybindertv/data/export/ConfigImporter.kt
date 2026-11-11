package dev.cavatz.keybindertv.data.export

import android.content.Context
import android.net.Uri
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Importe et valide une configuration KeyBinderTv depuis JSON.
 *
 * @param json Instance Json configurée avec ignoreUnknownKeys = true pour la rétrocompatibilité
 */
class ConfigImporter(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    /** Résultat d'une tentative d'import */
    sealed class ImportResult {
        /** Import réussi avec la configuration désérialisée */
        data class Success(val config: KeyBinderConfig) : ImportResult()
        /** Import échoué avec le message d'erreur */
        data class Error(val message: String) : ImportResult()
    }

    /**
     * Désérialise et valide un JSON en [KeyBinderConfig].
     *
     * Règles de validation :
     * - version >= 1
     * - mappings non null (liste vide acceptée)
     * - chaque mapping doit avoir un id non vide
     *
     * @param jsonString Chaîne JSON à parser
     * @return [ImportResult.Success] ou [ImportResult.Error]
     */
    fun importFromJson(jsonString: String): ImportResult {
        if (jsonString.isBlank()) {
            return ImportResult.Error("Le JSON est vide")
        }
        return try {
            val config: KeyBinderConfig = json.decodeFromString(
                KeyBinderConfig.serializer(),
                jsonString
            )
            validateConfig(config)
        } catch (exception: SerializationException) {
            ImportResult.Error("JSON invalide : ${exception.message}")
        } catch (exception: IllegalArgumentException) {
            ImportResult.Error("Format inattendu : ${exception.message}")
        }
    }

    /**
     * Lit le contenu d'un fichier via SAF et tente l'import.
     * Opération exécutée sur le dispatcher IO.
     *
     * @param context Contexte Android
     * @param uri     URI du fichier source (obtenu via ACTION_OPEN_DOCUMENT)
     * @return [ImportResult.Success] ou [ImportResult.Error]
     */
    suspend fun importFromFile(context: Context, uri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val content: String = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: return@withContext ImportResult.Error("Impossible de lire le fichier")
                importFromJson(content)
            } catch (exception: Exception) {
                ImportResult.Error("Erreur de lecture : ${exception.message}")
            }
        }

    /**
     * Valide les règles métier de la configuration importée.
     *
     * @param config Configuration à valider
     * @return [ImportResult.Success] si valide, [ImportResult.Error] sinon
     */
    private fun validateConfig(config: KeyBinderConfig): ImportResult {
        if (config.version < 1) {
            return ImportResult.Error("Versión de configuración no válida: ${config.version}")
        }
        if (config.backupRecoveryPending) {
            return ImportResult.Error("La copia contiene un estado de recuperación local no válido")
        }
        if (config.mappings.any { it.id.isBlank() }) {
            return ImportResult.Error("La copia contiene una asignación sin identificador")
        }
        if (config.mappings.map { it.id }.distinct().size != config.mappings.size) {
            return ImportResult.Error("La copia contiene identificadores repetidos")
        }
        val invalidMapping = config.mappings.firstOrNull { mapping ->
            (mapping.button.keyCode == null && mapping.button.sourcePackage.isNullOrBlank()) ||
                listOfNotNull(mapping.shortPressAction, mapping.longPressAction)
                    .any { it.packageName.isBlank() } ||
                (mapping.button.sourcePackage != null &&
                    listOfNotNull(mapping.shortPressAction, mapping.longPressAction)
                        .any { it.packageName == mapping.button.sourcePackage })
        }
        if (invalidMapping != null) {
            return ImportResult.Error("La copia contiene una asignación o aplicación de destino no válida")
        }
        return ImportResult.Success(config)
    }
}
