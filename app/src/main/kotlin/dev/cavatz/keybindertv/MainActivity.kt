package dev.cavatz.keybindertv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dev.cavatz.keybindertv.ui.navigation.KeyBinderNavGraph
import dev.cavatz.keybindertv.ui.theme.KeyBinderTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KeyBinderTvApp
        app.updateManager.checkForUpdates()
        setContent {
            KeyBinderTvTheme {
                val navController = rememberNavController()
                val updateState by app.updateManager.state.collectAsStateWithLifecycle()
                KeyBinderNavGraph(
                    navController = navController,
                    mappingRepository = app.mappingRepository
                )
                if (updateState.updateAvailable) {
                    AlertDialog(
                        onDismissRequest = { app.updateManager.dismissNotice() },
                        title = { Text("Nueva versión de MANDO TCL") },
                        text = {
                            Text(
                                buildString {
                                    append(updateState.latestVersion)
                                    if (updateState.releaseNotes.isNotBlank()) {
                                        append("\n\n")
                                        append(updateState.releaseNotes.take(700))
                                    }
                                }
                            )
                        },
                        confirmButton = {
                            Button(onClick = { app.updateManager.downloadAndInstall() }) {
                                Text("Descargar e instalar")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { app.updateManager.dismissNotice() }) {
                                Text("Más tarde")
                            }
                        }
                    )
                }
            }
        }
    }
}
