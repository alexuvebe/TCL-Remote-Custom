package dev.cavatz.keybindertv.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.repository.MappingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Copia privada de la configuración en un Gist de GitHub.
 * El token OAuth se cifra con una clave no exportable de Android Keystore.
 */
class GitHubBackupManager(
    context: Context,
    private val repository: MappingRepository,
    private val scope: CoroutineScope
) {
    data class UiState(
        val status: String = "Copia de seguridad no conectada",
        val userCode: String? = null,
        val verificationUrl: String? = null,
        val busy: Boolean = false,
        val connected: Boolean = false
    )

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("github_backup", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val uploadMutex = Mutex()

    init {
        scope.launch(Dispatchers.IO) {
            val connected = readToken() != null
            _state.value = UiState(
                connected = connected,
                status = if (connected) "GitHub conectado" else "Copia de seguridad no conectada"
            )
            var firstEmission = true
            repository.configFlow.collectLatest { config ->
                if (firstEmission) {
                    firstEmission = false
                    return@collectLatest
                }
                delay(1800)
                // Nunca sustituir una copia remota por valores de fábrica tras una lectura dañada.
                if (config.backupRecoveryPending) return@collectLatest
                if (readToken() != null && prefs.getString(KEY_GIST_ID, null) != null) {
                    runCatching { uploadConfig(config) }
                        .onFailure {
                            _state.value = _state.value.copy(
                                status = "No se pudo actualizar la copia: ${it.message}"
                            )
                        }
                }
            }
        }
    }

    fun connect() {
        if (_state.value.busy) return
        scope.launch {
            _state.value = _state.value.copy(busy = true, status = "Solicitando código a GitHub…")
            try {
                val device = withContext(Dispatchers.IO) { requestDeviceCode() }
                _state.value = UiState(
                    status = "Introduce el código en GitHub desde tu móvil",
                    userCode = device.userCode,
                    verificationUrl = device.verificationUri,
                    busy = true,
                    connected = false
                )
                val token = pollForToken(device)
                withContext(Dispatchers.IO) { saveToken(token) }
                _state.value = UiState(status = "Autorizado. Preparando copia de seguridad…", busy = true, connected = true)
                val connectionStatus = withContext(Dispatchers.IO) { initializeGist() }
                _state.value = UiState(status = connectionStatus, connected = true)
            } catch (e: Exception) {
                _state.value = UiState(status = "No se pudo conectar: ${e.message ?: "error desconocido"}", connected = readToken() != null)
            }
        }
    }

    fun backupNow() {
        scope.launch {
            _state.value = _state.value.copy(busy = true, status = "Guardando copia de seguridad…")
            try {
                withContext(Dispatchers.IO) { uploadCurrentConfig() }
                _state.value = _state.value.copy(busy = false, status = "Copia de seguridad actualizada", connected = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error al guardar: ${e.message}")
            }
        }
    }

    fun restoreNow() {
        scope.launch {
            _state.value = _state.value.copy(busy = true, status = "Buscando copia de seguridad…")
            try {
                val config = withContext(Dispatchers.IO) { downloadConfig() }
                repository.replaceConfig(config)
                _state.value = _state.value.copy(busy = false, status = "Configuración restaurada correctamente", connected = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error al restaurar: ${e.message}")
            }
        }
    }

    fun disconnect() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_GIST_ID).apply()
        _state.value = UiState(
            status = "Desconectado en MANDO TCL. Para revocar el permiso concedido, elimina la autorización de la aplicación en GitHub > Settings > Applications."
        )
    }

    private data class DeviceCode(val deviceCode: String, val userCode: String, val verificationUri: String, val expiresIn: Int, val interval: Int)

    private fun requestDeviceCode(): DeviceCode {
        val body = "client_id=${enc(CLIENT_ID)}&scope=gist"
        val obj = requestJson("https://github.com/login/device/code", "POST", body, null).jsonObject
        return DeviceCode(
            obj.getValue("device_code").jsonPrimitive.content,
            obj.getValue("user_code").jsonPrimitive.content,
            obj.getValue("verification_uri").jsonPrimitive.content,
            obj.getValue("expires_in").jsonPrimitive.content.toInt(),
            obj.getValue("interval").jsonPrimitive.content.toInt()
        )
    }

    private suspend fun pollForToken(device: DeviceCode): String {
        val started = System.currentTimeMillis()
        var interval = device.interval.coerceAtLeast(5)
        while ((System.currentTimeMillis() - started) / 1000 < device.expiresIn) {
            delay(interval * 1000L)
            val body = "client_id=${enc(CLIENT_ID)}&device_code=${enc(device.deviceCode)}&grant_type=${enc("urn:ietf:params:oauth:grant-type:device_code")}"
            val obj = withContext(Dispatchers.IO) {
                requestJson("https://github.com/login/oauth/access_token", "POST", body, null).jsonObject
            }
            val token = obj["access_token"]?.jsonPrimitive?.content
            if (!token.isNullOrBlank()) return token
            when (obj["error"]?.jsonPrimitive?.content) {
                "authorization_pending" -> Unit
                "slow_down" -> interval += 5
                "expired_token" -> error("El código ha caducado. Vuelve a intentarlo.")
                "access_denied" -> error("Has cancelado la autorización.")
                else -> error(obj["error_description"]?.jsonPrimitive?.content ?: "Respuesta OAuth no válida")
            }
        }
        error("El código ha caducado. Vuelve a intentarlo.")
    }

    private suspend fun initializeGist(): String {
        var existingId = prefs.getString(KEY_GIST_ID, null)
        val token = readToken() ?: error("No se ha guardado la autorización de GitHub")
        if (existingId == null) {
            // Tras reinstalar se pierde el identificador local; localizamos la copia privada por su descripción.
            val gists = requestJson("https://api.github.com/gists?per_page=100", "GET", null, token)
            val match = gists.jsonArray.firstOrNull { gist ->
                gist.jsonObject["description"]?.jsonPrimitive?.content in
                    setOf(GIST_DESCRIPTION, "Copia privada de MANDO TCL") &&
                    gist.jsonObject["files"]?.jsonObject?.containsKey(BACKUP_FILE) == true
            }
            existingId = match?.jsonObject?.get("id")?.jsonPrimitive?.content
            if (existingId != null) prefs.edit().putString(KEY_GIST_ID, existingId).apply()
        }
        if (existingId != null) {
            val local = repository.configFlow.first()
            if (local == KeyBinderConfig() || local.backupRecoveryPending) {
                repository.replaceConfig(downloadConfig())
                return "GitHub conectado. Se ha recuperado la copia existente."
            }
            // No asumimos que la configuración local sea más reciente que la remota.
            return "GitHub conectado. Se ha detectado una copia existente y no se ha sobrescrito. Usa Restaurar si quieres recuperarla."
        }

        val local = repository.configFlow.first()
        if (local.backupRecoveryPending) {
            error("La configuración local se recuperó con valores de fábrica y no se ha encontrado una copia previa. No se creará una copia vacía para proteger tus datos.")
        }
        validateBackupConfig(local)
        val payload = backupPayload(local)
        val body = buildJsonObject {
            put("description", GIST_DESCRIPTION)
            put("public", false)
            put("files", buildJsonObject {
                put(BACKUP_FILE, buildJsonObject { put("content", payload) })
            })
        }.toString()
        val response = requestJson("https://api.github.com/gists", "POST", body, readToken()).jsonObject
        val id = response["id"]?.jsonPrimitive?.content ?: error("GitHub no devolvió el identificador de la copia")
        prefs.edit().putString(KEY_GIST_ID, id).apply()
        return "GitHub conectado. Copia de seguridad no pública creada."
    }

    private suspend fun uploadCurrentConfig() {
        val current = repository.configFlow.first()
        if (current.backupRecoveryPending) {
            error("La configuración local está marcada como recuperada tras un posible daño. Restaura primero la copia de GitHub para evitar sobrescribirla.")
        }
        uploadConfig(current)
    }

    private suspend fun uploadConfig(config: KeyBinderConfig) {
        validateBackupConfig(config)
        val token = readToken() ?: error("Conecta primero con GitHub")
        val id = prefs.getString(KEY_GIST_ID, null) ?: error("No existe una copia; vuelve a conectar")
        val body = buildJsonObject {
            put("files", buildJsonObject {
                put(BACKUP_FILE, buildJsonObject { put("content", backupPayload(config)) })
            })
        }.toString()
        uploadMutex.withLock {
            requestJson("https://api.github.com/gists/$id", "PATCH", body, token)
        }
    }

    private suspend fun downloadConfig(): KeyBinderConfig {
        val token = readToken() ?: error("Conecta primero con GitHub")
        val id = prefs.getString(KEY_GIST_ID, null) ?: error("No hay copia asociada")
        val gist = requestJson("https://api.github.com/gists/$id", "GET", null, token).jsonObject
        val content = gist.getValue("files").jsonObject.getValue(BACKUP_FILE).jsonObject.getValue("content").jsonPrimitive.content
        val root = json.parseToJsonElement(content).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.intOrNull == 1) {
            "La versión del formato de copia no es compatible"
        }
        require(root["app"]?.jsonPrimitive?.content == "MANDO TCL") {
            "El archivo no corresponde a una copia de MANDO TCL"
        }
        require((root["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L) > 0L) {
            "La copia no contiene una fecha válida"
        }
        val raw = root.getValue("config").toString()
        val config = json.decodeFromString(KeyBinderConfig.serializer(), raw)
        validateBackupConfig(config)
        return config
    }

    private fun validateBackupConfig(config: KeyBinderConfig) {
        require(config.version >= 1) { "La versión de la copia no es válida" }
        require(config.longPressThresholdMs in 200L..2000L) {
            "El umbral de pulsación de la copia no está entre 200 y 2000 ms"
        }
        require(!config.backupRecoveryPending) { "La copia no puede contener una recuperación local pendiente" }
        require(config.mappings.all { it.id.isNotBlank() }) {
            "La copia contiene una asignación sin identificador"
        }
        require(config.mappings.map { it.id }.distinct().size == config.mappings.size) {
            "La copia contiene identificadores repetidos"
        }
        config.mappings.forEach { mapping ->
            require(mapping.button.keyCode != null || !mapping.button.sourcePackage.isNullOrBlank()) {
                "La copia contiene un botón sin identificar"
            }
            val targets = listOfNotNull(mapping.shortPressAction, mapping.longPressAction)
            require(targets.all { it.packageName.isNotBlank() }) {
                "La copia contiene una aplicación de destino no válida"
            }
            require(mapping.button.sourcePackage == null || targets.none {
                it.packageName == mapping.button.sourcePackage
            }) { "La copia contiene una reasignación que se apunta a sí misma" }
        }
    }

    private fun backupPayload(config: KeyBinderConfig): String = buildJsonObject {
        put("schemaVersion", 1)
        put("updatedAt", System.currentTimeMillis())
        put("app", "MANDO TCL")
        put("config", json.parseToJsonElement(json.encodeToString(KeyBinderConfig.serializer(), config.copy(backupRecoveryPending = false))))
    }.toString()

    private fun requestJson(url: String, method: String, body: String?, token: String?): kotlinx.serialization.json.JsonElement {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (url.contains("github.com/login/")) {
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            } else {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "MANDO-TCL-Android")
                if (!token.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("GitHub HTTP $code: ${response.take(180)}")
            return json.parseToJsonElement(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun saveToken(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))
        val stored = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(KEY_TOKEN, stored).apply()
    }

    private fun readToken(): String? = runCatching {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        val parts = stored.split(":")
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val CLIENT_ID = "Ov23liFBLQq9S8Hk2Ggc"
        private const val BACKUP_FILE = "mando-tcl-backup.json"
        private const val GIST_DESCRIPTION = "Copia de seguridad no pública de MANDO TCL"
        private const val KEY_TOKEN = "encrypted_token"
        private const val KEY_GIST_ID = "gist_id"
        private const val KEY_ALIAS = "mando_tcl_github_backup_key"
    }
}
