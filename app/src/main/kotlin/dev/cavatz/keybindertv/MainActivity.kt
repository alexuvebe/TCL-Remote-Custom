package dev.cavatz.keybindertv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import dev.cavatz.keybindertv.ui.navigation.KeyBinderNavGraph
import dev.cavatz.keybindertv.ui.theme.KeyBinderTvTheme
import dev.cavatz.keybindertv.update.AppUpdateManager
import kotlinx.coroutines.launch

/**
 * Punto de entrada de MANDO TCL.
 * Comprueba al arrancar si hay una versión estable más reciente y pregunta antes de instalarla.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app: KeyBinderTvApp = application as KeyBinderTvApp
        setContent {
            KeyBinderTvTheme {
                val navController = rememberNavController()
                var availableUpdate by remember { mutableStateOf<String?>(null) }
                var checkingUpdate by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    try {
                        availableUpdate = AppUpdateManager.checkForUpdate(this@MainActivity)
                    } catch (_: Exception) {
                        // Si no hay red o GitHub no responde, la aplicación sigue funcionando normalmente.
                    } finally {
                        checkingUpdate = false
                    }
                }

                KeyBinderNavGraph(
                    navController = navController,
                    mappingRepository = app.mappingRepository
                )

                if (!checkingUpdate && availableUpdate != null) {
                    AlertDialog(
                        onDismissRequest = { availableUpdate = null },
                        title = { Text("Actualización disponible") },
                        text = {
                            Text("Hay una nueva versión de MANDO TCL (${availableUpdate}). ¿Quieres descargarla e instalarla ahora?")
                        },
                        confirmButton = {
                            Button(onClick = {
                                availableUpdate = null
                                scope.launch {
                                    try {
                                        val result = AppUpdateManager.checkAndInstall(this@MainActivity)
                                        Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                                    } catch (error: Exception) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "No se pudo actualizar: ${error.message ?: "error desconocido"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }) {
                                Text("Actualizar")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { availableUpdate = null }) {
                                Text("Más tarde")
                            }
                        }
                    )
                }
            }
        }
    }
}
