package dev.cavatz.keybindertv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dev.cavatz.keybindertv.ui.navigation.KeyBinderNavGraph
import dev.cavatz.keybindertv.ui.theme.KeyBinderTvTheme

/**
 * Point d'entrée unique de l'application.
 * Configurée comme LEANBACK_LAUNCHER pour apparaître dans le launcher Android TV.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app: KeyBinderTvApp = application as KeyBinderTvApp
        setContent {
            KeyBinderTvTheme {
                val navController = rememberNavController()
                KeyBinderNavGraph(
                    navController = navController,
                    mappingRepository = app.mappingRepository
                )
            }
        }
    }
}
