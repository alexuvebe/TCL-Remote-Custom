package dev.cavatz.keybindertv.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.cavatz.keybindertv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object AppUpdateManager {
    private const val RELEASE_API = "https://api.github.com/repos/alexuvebe/TCL-Remote-Custom/releases/latest"
    private const val APK_NAME = "mando-tcl.apk"

    suspend fun checkAndInstall(activity: Activity): String = withContext(Dispatchers.IO) {
        val releaseConnection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MANDO-TCL-Android-Updater")
        }
        val release = try {
            if (releaseConnection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("No se pudo consultar GitHub (HTTP ${releaseConnection.responseCode})")
            }
            JSONObject(releaseConnection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            releaseConnection.disconnect()
        }

        val tag = release.optString("tag_name")
        val remoteCode = tag.substringAfterLast("-").toLongOrNull()
            ?: throw IllegalStateException("La versión publicada no tiene un número válido")
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        val currentCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        if (remoteCode <= currentCode) {
            return@withContext "MANDO TCL ya está actualizado (versión ${BuildConfig.VERSION_NAME})"
        }

        val assets = release.optJSONArray("assets")
            ?: throw IllegalStateException("La publicación no contiene archivos")
        var apkUrl: String? = null
        var expectedDigest: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == APK_NAME) {
                apkUrl = asset.optString("browser_download_url")
                expectedDigest = asset.optString("digest").removePrefix("sha256:")
                break
            }
        }
        val downloadUrl = apkUrl ?: throw IllegalStateException("No encuentro el APK en la última versión")
        val digest = expectedDigest?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
            ?: throw IllegalStateException("GitHub no ha proporcionado una huella SHA-256 válida. Se cancela la instalación por seguridad.")

        val parsedUrl = URL(downloadUrl)
        if (parsedUrl.protocol != "https" || parsedUrl.host != "github.com") {
            throw IllegalStateException("La dirección de descarga no pertenece a GitHub o no usa HTTPS")
        }

        // Solicitar el permiso antes de descargar el APK para evitar descargas repetidas.
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            return@withContext withContext(Dispatchers.Main) {
                val permissionIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}")
                )
                try {
                    if (permissionIntent.resolveActivity(activity.packageManager) == null) {
                        "Activa «Instalar aplicaciones desconocidas» para MANDO TCL desde los ajustes del televisor y vuelve a buscar la actualización."
                    } else {
                        activity.startActivity(permissionIntent)
                        "Autoriza la instalación de MANDO TCL y vuelve a pulsar «Buscar actualización»."
                    }
                } catch (exception: ActivityNotFoundException) {
                    "No se han podido abrir los ajustes de instalación. Activa manualmente el permiso para MANDO TCL y vuelve a intentarlo."
                }
            }
        }

        val updateDir = File(activity.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDir, APK_NAME)
        val cachedIsValid = apkFile.isFile && apkFile.length() > 1_000_000L &&
            sha256(apkFile).equals(digest, ignoreCase = true)

        if (!cachedIsValid) {
            apkFile.delete()
            val apkConnection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MANDO-TCL-Android-Updater")
            }
            try {
                if (apkConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("No se pudo descargar el APK (HTTP ${apkConnection.responseCode})")
                }
                apkConnection.inputStream.use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                apkConnection.disconnect()
            }
        }

        if (!apkFile.isFile || apkFile.length() < 1_000_000L) {
            apkFile.delete()
            throw IllegalStateException("La descarga del APK está incompleta")
        }
        if (!sha256(apkFile).equals(digest, ignoreCase = true)) {
            apkFile.delete()
            throw IllegalStateException("La comprobación SHA-256 del APK ha fallado. No se instalará.")
        }

        withContext(Dispatchers.Main) {
            try {
                val apkUri = FileProvider.getUriForFile(
                    activity,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (installIntent.resolveActivity(activity.packageManager) == null) {
                    "El televisor no ofrece un instalador compatible. El APK verificado está descargado en la caché; actualiza desde el navegador del televisor."
                } else {
                    activity.startActivity(installIntent)
                    "Actualización ${tag} descargada y verificada. Confirma la instalación en el televisor."
                }
            } catch (exception: ActivityNotFoundException) {
                "No se ha encontrado el instalador del televisor. El APK se ha descargado y verificado; puedes actualizar desde el navegador."
            } catch (exception: SecurityException) {
                "El televisor ha bloqueado el instalador. Revisa el permiso de instalación de MANDO TCL."
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
