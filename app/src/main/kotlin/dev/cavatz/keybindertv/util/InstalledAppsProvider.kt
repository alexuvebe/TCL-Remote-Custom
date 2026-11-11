package dev.cavatz.keybindertv.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

/**
 * Fournit la liste des applications installées sur l'appareil.
 * Nécessite la permission QUERY_ALL_PACKAGES déclarée dans le manifest.
 *
 * @param context Contexte Android
 */
class InstalledAppsProvider(private val context: Context) {

    /** Représente une application installée avec ses métadonnées d'affichage */
    data class AppInfo(
        val packageName: String,
        val appLabel: String,
        val icon: Drawable?
    )

    /**
     * Retourne la liste de toutes les apps installées ayant une activité principale.
     * Trié alphabétiquement par appLabel.
     * Exclut les apps système sans launcher et notre propre application.
     */
    fun getInstalledApps(): List<AppInfo> {
        val launcherIntents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        )
        val resolvedApps = launcherIntents
            .flatMap { intent ->
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            .distinctBy { it.activityInfo?.packageName }

        return resolvedApps
            .mapNotNull { resolveInfo -> buildAppInfo(resolveInfo) }
            .filter { app -> app.packageName != context.packageName }
            .sortedBy { app -> app.appLabel.lowercase() }
    }

    /** Construit un [AppInfo] depuis un [ResolveInfo], retourne null en cas d'erreur */
    private fun buildAppInfo(resolveInfo: ResolveInfo): AppInfo? {
        return try {
            val packageName: String = resolveInfo.activityInfo.packageName
            val appLabel: String = resolveInfo.loadLabel(context.packageManager).toString()
            val icon: Drawable? = runCatching {
                context.packageManager.getApplicationIcon(packageName)
            }.getOrNull()
            AppInfo(packageName = packageName, appLabel = appLabel, icon = icon)
        } catch (e: Exception) {
            null
        }
    }
}
