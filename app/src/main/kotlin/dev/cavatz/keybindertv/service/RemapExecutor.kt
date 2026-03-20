package dev.cavatz.keybindertv.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dev.cavatz.keybindertv.data.model.ActionTarget

/**
 * Exécute le lancement d'une application cible et optionnellement
 * affiche un toast de confirmation et tue l'application d'origine.
 *
 * @param context Contexte Android (ApplicationContext recommandé)
 */
class RemapExecutor(private val context: Context) {

    companion object {
        private const val KILL_DELAY_MS: Long = 300L
        private const val TAG: String = "RemapExecutor"
    }

    private val handler: Handler = Handler(Looper.getMainLooper())

    /**
     * Lance l'application cible.
     * - Intent avec FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP
     * - Toast overlay si [showOverlay] = true
     *
     * @param target      Application à lancer
     * @param showOverlay Afficher un toast de confirmation
     */
    fun launch(target: ActionTarget, showOverlay: Boolean) {
        val launched: Boolean = startTargetApp(target)
        if (!launched) return

        if (showOverlay) {
            showConfirmationToast(target.appLabel)
        }
    }

    /**
     * Lance l'application cible ET kill l'application source après [KILL_DELAY_MS] ms.
     * Utilisé pour Flux B : l'app originale s'est déjà lancée, on la remplace.
     *
     * @param target        Application cible à lancer
     * @param sourcePackage Package de l'app originale à tuer
     * @param showOverlay   Afficher un toast de confirmation
     */
    fun launchAndKillSource(target: ActionTarget, sourcePackage: String, showOverlay: Boolean) {
        val launched: Boolean = startTargetApp(target)
        if (!launched) return

        if (showOverlay) {
            showConfirmationToast(target.appLabel)
        }

        // Tuer l'app source après un court délai pour laisser la cible s'initialiser
        handler.postDelayed({
            killBackgroundProcess(sourcePackage)
        }, KILL_DELAY_MS)
    }

    /**
     * Construit et lance l'Intent vers l'application cible.
     * Retourne false si le package manager ne trouve pas l'application.
     */
    private fun startTargetApp(target: ActionTarget): Boolean {
        val launchIntent: Intent? = context.packageManager
            .getLaunchIntentForPackage(target.packageName)

        if (launchIntent == null) {
            Log.w(TAG, "Package introuvable : ${target.packageName}")
            return false
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return try {
            context.startActivity(launchIntent)
            Log.d(TAG, "Application lancée : ${target.packageName}")
            true
        } catch (exception: Exception) {
            Log.e(TAG, "Erreur lors du lancement de ${target.packageName}", exception)
            false
        }
    }

    /**
     * Tente de tuer le processus en arrière-plan du package indiqué.
     * Nécessite la permission KILL_BACKGROUND_PROCESSES dans le manifest.
     */
    private fun killBackgroundProcess(packageName: String) {
        try {
            val activityManager: ActivityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            Log.d(TAG, "Processus tué : $packageName")
        } catch (exception: Exception) {
            Log.w(TAG, "Impossible de tuer $packageName : ${exception.message}")
        }
    }

    /** Affiche un toast bref indiquant l'application cible lancée. */
    private fun showConfirmationToast(appLabel: String) {
        handler.post {
            Toast.makeText(context, "→ $appLabel", Toast.LENGTH_SHORT).show()
        }
    }
}
