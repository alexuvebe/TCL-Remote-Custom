package dev.cavatz.keybindertv.data.export

import android.content.Context
import android.net.Uri
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Exporte la configuration KeyBinderTv en JSON formaté.
 *
 * @param json Instance Json configurée avec prettyPrint = true et encodeDefaults = true
 */
class ConfigExporter(
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
) {

    /**
     * Sérialise la configuration en JSON lisible et indenté.
     *
     * @param config Configuration à exporter
     * @return JSON multi-lignes indenté
     */
    fun exportToJson(config: KeyBinderConfig): String =
        json.encodeToString(KeyBinderConfig.serializer(), config)

    /**
     * Écrit le JSON dans un fichier via un URI (SAF — Storage Access Framework).
     * Opération exécutée sur le dispatcher IO pour ne pas bloquer le thread principal.
     *
     * @param context Contexte Android
     * @param uri     URI du fichier de destination (obtenu via ACTION_CREATE_DOCUMENT)
     * @param config  Configuration à exporter
     */
    suspend fun exportToFile(context: Context, uri: Uri, config: KeyBinderConfig) {
        val jsonString: String = exportToJson(config)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
