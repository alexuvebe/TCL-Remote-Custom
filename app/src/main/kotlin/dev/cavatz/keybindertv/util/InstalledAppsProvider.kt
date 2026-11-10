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
     * Retourne une seule entrée par application, triée alphabétiquement.
     * Certaines applications exposent plusieurs activités launcher : elles doivent
     * être dédupliquées car la grille Compose utilise packageName comme clé stable.
     */
    fun getInstalledApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps: List<ResolveInfo> = context.packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        return resolvedApps
            .mapNotNull { resolveInfo -> buildAppInfo(resolveInfo) }
            .filter { app -> app.packageName != context.packageName }
            .distinctBy { app -> app.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { app -> app.appLabel })
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
