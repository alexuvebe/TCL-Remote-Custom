package dev.cavatz.keybindertv.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.cavatz.keybindertv.BuildConfig
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
        val currentCode = if (Build.VERSION.SDK_INT >= 28) {
            activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode.toLong()
        }
        if (remoteCode <= currentCode) return@withContext "MANDO TCL ya está actualizado (versión ${BuildConfig.VERSION_NAME})"

        val assets = release.optJSONArray("assets") ?: throw IllegalStateException("La publicación no contiene archivos")
        var apkUrl: String? = null
        var expectedDigest: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == APK_NAME) {
                apkUrl = asset.optString("browser_download_url")
                expectedDigest = asset.optString("digest").removePrefix("sha256:").takeIf { it.isNotBlank() }
                break
            }
        }
        val downloadUrl = apkUrl ?: throw IllegalStateException("No encuentro el APK en la última versión")

        val updateDir = File(activity.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDir, APK_NAME)
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
        if (!apkFile.isFile || apkFile.length() < 1_000_000L) {
            apkFile.delete()
            throw IllegalStateException("La descarga del APK está incompleta")
        }
        if (expectedDigest != null) {
            val actual = MessageDigest.getInstance("SHA-256").digest(apkFile.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedDigest, ignoreCase = true)) {
                apkFile.delete()
                throw IllegalStateException("La comprobación de seguridad del APK ha fallado")
            }
        }

        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
                val permissionIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(permissionIntent)
                "APK descargado. Autoriza la instalación de MANDO TCL y vuelve a pulsar «Buscar actualización»."
            } else {
                val apkUri = FileProvider.getUriForFile(
                    activity,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(installIntent)
                "Actualización ${tag} descargada. Confirma la instalación en el televisor."
            }
        }
    }
}
