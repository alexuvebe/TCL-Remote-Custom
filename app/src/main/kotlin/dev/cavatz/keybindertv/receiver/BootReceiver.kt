package dev.cavatz.keybindertv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Receiver démarré au boot de l'appareil.
 * Vérifie que le service d'accessibilité est actif ; si non, logue un avertissement.
 *
 * Note : On ne peut pas démarrer un AccessibilityService programmatiquement —
 * l'utilisateur doit l'activer manuellement via les paramètres Android.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG: String = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot détecté — vérification du service d'accessibilité")

        if (isAccessibilityServiceEnabled(context)) {
            Log.i(TAG, "Service d'accessibilité actif — aucune action requise")
        } else {
            Log.w(TAG, "Service d'accessibilité inactif — l'utilisateur doit l'activer manuellement")
        }
    }

    /**
     * Vérifie si le service d'accessibilité KeyBinderTv est activé dans les paramètres système.
     *
     * @param context Contexte Android pour accéder au ContentResolver
     * @return true si le service est activé, false sinon
     */
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices: String = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(context.packageName)
    }
}
