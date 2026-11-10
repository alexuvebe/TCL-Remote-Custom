package dev.cavatz.keybindertv.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.cavatz.keybindertv.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val updateAvailable: Boolean = false,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloaded: Boolean = false,
    val message: String = "Comprueba si hay una actualización disponible."
)

/**
 * Comprueba las publicaciones oficiales de GitHub y prepara el APK para que
 * Android TV muestre su instalador del sistema. Nunca instala en silencio.
 */
class UpdateManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(UpdateUiState())
    val state = _state.asStateFlow()

    @Volatile private var downloadUrl: String? = null
    @Volatile private var downloadedApk: File? = null

    fun checkForUpdates() {
        if (_state.value.isChecking) return
        scope.launch {
            _state.value = _state.value.copy(isChecking = true, message = "Buscando actualizaciones…")
            try {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "MANDO-TCL-Android-TV")
                }
                val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                connection.disconnect()

                val tag = release.optString("tag_name")
                val remoteCode = tag.substringAfterLast('-').toIntOrNull()
                    ?: throw IllegalStateException("La versión publicada no tiene un número válido.")
                val assetList = release.optJSONArray("assets")
                    ?: throw IllegalStateException("La publicación no contiene el APK.")
                var apkUrl: String? = null
                for (index in 0 until assetList.length()) {
                    val asset = assetList.getJSONObject(index)
                    if (asset.optString("name") == APK_NAME) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isNullOrBlank()) throw IllegalStateException("No se encontró el archivo APK.")

                val available = remoteCode > BuildConfig.VERSION_CODE
                downloadUrl = apkUrl
                _state.value = UpdateUiState(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = release.optString("name", tag),
                    releaseNotes = release.optString("body"),
                    updateAvailable = available,
                    message = if (available) "Hay una nueva versión disponible." else "Ya tienes la última versión."
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isChecking = false,
                    message = "No se pudo comprobar la actualización. Revisa la conexión e inténtalo de nuevo."
                )
            }
        }
    }

    fun downloadAndInstall() {
        if (_state.value.isDownloading) return
        val url = downloadUrl ?: run {
            checkForUpdates()
            return
        }
        scope.launch {
            _state.value = _state.value.copy(isDownloading = true, message = "Descargando actualización…")
            try {
                val directory = File(context.cacheDir, "updates").apply { mkdirs() }
                val apk = File(directory, "mando-tcl-update.apk")
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                }
                connection.inputStream.use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }
                connection.disconnect()
                if (!apk.isFile || apk.length() < MIN_APK_SIZE) {
                    apk.delete()
                    throw IllegalStateException("La descarga está incompleta.")
                }
                downloadedApk = apk
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloaded = true,
                    message = "Descarga terminada. Pulsa «Instalar actualización»."
                )
                installDownloadedApk()
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    message = "No se pudo descargar el APK. Inténtalo de nuevo."
                )
            }
        }
    }

    fun installDownloadedApk() {
        val apk = downloadedApk ?: return
        if (!apk.exists()) {
            _state.value = _state.value.copy(downloaded = false, message = "El archivo descargado ya no está disponible.")
            return
        }
        try {
            if (!context.packageManager.canRequestPackageInstalls()) {
                _state.value = _state.value.copy(
                    message = "Permite a MANDO TCL instalar actualizaciones y vuelve a pulsar «Instalar actualización»."
                )
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            _state.value = _state.value.copy(message = "Confirma la instalación en la ventana de Android TV.")
        } catch (error: Exception) {
            _state.value = _state.value.copy(message = "Android TV no ha podido abrir el instalador.")
        }
    }

    fun dismissNotice() {
        _state.value = _state.value.copy(updateAvailable = false)
    }

    companion object {
        private const val RELEASE_API =
            "https://api.github.com/repos/alexuvebe/TCL-Remote-Custom/releases/latest"
        private const val APK_NAME = "app-debug.apk"
        private const val MIN_APK_SIZE = 1_000_000L
    }
}
