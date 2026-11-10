package dev.cavatz.keybindertv.ui.screen.learning

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.ui.component.tvFocusOutline
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.service.KeyInterceptorService
import dev.cavatz.keybindertv.ui.navigation.Screen
import dev.cavatz.keybindertv.ui.theme.ActionBlue
import dev.cavatz.keybindertv.ui.theme.ActionGray
import dev.cavatz.keybindertv.ui.theme.ActionGreen
import kotlinx.coroutines.launch

/** Espacement vertical entre les éléments */
private val VerticalSpacing = 16.dp

/** Padding global de l'écran */
private val ScreenPadding = 32.dp

/** Type d'appui par défaut après la détection du bouton */
private const val DEFAULT_PRESS_TYPE: String = "short"

/**
 * Écran de détection du bouton de télécommande.
 * Guide l'utilisateur pour appuyer sur le bouton à remapper avec un countdown.
 *
 * @param navController Contrôleur de navigation
 * @param mappingId Identifiant du mapping en cours de création
 * @param repository Repository des mappings pour mettre à jour le bouton détecté
 */
@Composable
fun LearningScreen(
    navController: NavHostController,
    mappingId: String,
    repository: MappingRepository
) {
    val viewModel: LearningViewModel = viewModel(
        factory = LearningViewModel.factory(repository, mappingId)
    )
    val state: LearningViewModel.LearningState by viewModel.state.collectAsStateWithLifecycle()
    val remainingSeconds: Int by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Si le service est inactif, afficher un message d'erreur
    if (KeyInterceptorService.instance == null) {
        ServiceInactiveMessage(onCancel = { navController.popBackStack() })
        return
    }

    LearningContent(
        state = state,
        remainingSeconds = remainingSeconds,
        onConfirm = {
            coroutineScope.launch {
                viewModel.confirmButton()
                // Navigation vers AppChooser pour configurer les actions du bouton détecté
                navController.navigate(
                    Screen.AppChooser.createRoute(mappingId, DEFAULT_PRESS_TYPE)
                ) {
                    popUpTo(Screen.MappingList.route) { inclusive = false }
                }
            }
        },
        onRetry = { viewModel.retry() },
        onCancel = {
            viewModel.cancel()
            navController.popBackStack()
        }
    )
}

/** Contenu stateless de l'écran d'apprentissage */
@Composable
private fun LearningContent(
    state: LearningViewModel.LearningState,
    remainingSeconds: Int,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.learning_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(VerticalSpacing))

        when (state) {
            is LearningViewModel.LearningState.WaitingForButton -> {
                WaitingContent(remainingSeconds = remainingSeconds, onCancel = onCancel)
            }
            is LearningViewModel.LearningState.ButtonDetected -> {
                ButtonDetectedContent(
                    identifier = state.identifier,
                    onConfirm = onConfirm,
                    onRetry = onRetry
                )
            }
            is LearningViewModel.LearningState.Timeout -> {
                TimeoutContent(onRetry = onRetry, onCancel = onCancel)
            }
        }
    }
}

/** Contenu affiché pendant l'attente d'un appui */
@Composable
private fun WaitingContent(remainingSeconds: Int, onCancel: () -> Unit) {
    Text(
        text = stringResource(R.string.learning_instructions),
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(modifier = Modifier.height(VerticalSpacing))
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(VerticalSpacing))
    Text(
        text = "$remainingSeconds s",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(VerticalSpacing))
    TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = ActionGray)) {
        Text(text = stringResource(R.string.learning_cancel))
    }
}

/** Contenu affiché quand un bouton a été détecté */
@Composable
private fun ButtonDetectedContent(
    identifier: ButtonIdentifier,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    val label: String = identifier.userLabel.ifEmpty {
        identifier.keyCode?.toString() ?: identifier.sourcePackage ?: "Botón desconocido"
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(VerticalSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteDiagram(identifier = identifier)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Botón detectado: $label",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Código: ${identifier.keyCode ?: "No disponible"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Aplicación original: ${originalAppFor(identifier)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (remoteButtonPosition(identifier) == null) {
                Text(
                    text = "Posición física no identificada en este esquema",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(VerticalSpacing))
    Row(horizontalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = ActionGreen, contentColor = Color.White)) {
            Text(text = stringResource(R.string.learning_confirm))
        }
        TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = ActionBlue)) {
            Text(text = stringResource(R.string.learning_retry))
        }
    }
}

/** Contenu affiché en cas de timeout */
@Composable
private fun TimeoutContent(onRetry: () -> Unit, onCancel: () -> Unit) {
    Text(
        text = stringResource(R.string.learning_timeout),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(VerticalSpacing))
    Row(horizontalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White)) {
            Text(text = stringResource(R.string.learning_retry))
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.learning_cancel))
        }
    }
}

/** Message d'erreur quand le service d'accessibilité est inactif */
@Composable
private fun ServiceInactiveMessage(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.learning_service_inactive),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(VerticalSpacing))
        Button(onClick = onCancel) {
            Text(text = stringResource(R.string.learning_cancel))
        }
    }
}



/** Esquema genérico del mando TCL; solo resalta posiciones con código conocido. */
@Composable
private fun RemoteDiagram(identifier: ButtonIdentifier) {
    val position = remoteButtonPosition(identifier)
    Canvas(modifier = Modifier.size(width = 104.dp, height = 230.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFF303944),
            cornerRadius = CornerRadius(w * 0.22f)
        )
        drawCircle(Color(0xFF9AA6B2), radius = w * 0.07f, center = Offset(w * 0.78f, h * 0.09f))
        drawCircle(Color(0xFF657180), radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.34f))
        drawCircle(Color(0xFF9AA6B2), radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.34f))
        drawLine(Color(0xFF9AA6B2), Offset(w * 0.5f, h * 0.20f), Offset(w * 0.5f, h * 0.48f), strokeWidth = 3f)
        drawLine(Color(0xFF9AA6B2), Offset(w * 0.36f, h * 0.34f), Offset(w * 0.64f, h * 0.34f), strokeWidth = 3f)
        drawCircle(Color(0xFF657180), radius = w * 0.055f, center = Offset(w * 0.32f, h * 0.53f))
        drawCircle(Color(0xFF657180), radius = w * 0.055f, center = Offset(w * 0.68f, h * 0.53f))
        drawRoundRect(Color(0xFF657180), topLeft = Offset(w * 0.24f, h * 0.64f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.055f), cornerRadius = CornerRadius(8f))
        drawRoundRect(Color(0xFF657180), topLeft = Offset(w * 0.56f, h * 0.64f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.055f), cornerRadius = CornerRadius(8f))
        drawRoundRect(Color(0xFFE53935), topLeft = Offset(w * 0.31f, h * 0.81f), size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.065f), cornerRadius = CornerRadius(8f))
        if (position != null) {
            drawCircle(
                color = Color(0xFFFFD740),
                radius = w * 0.12f,
                center = Offset(w * position.first, h * position.second),
                style = Stroke(width = 6f)
            )
        }
    }
}

/** Coordenadas normalizadas de teclas cuya posición se puede representar con seguridad. */
private fun remoteButtonPosition(identifier: ButtonIdentifier): Pair<Float, Float>? {
    return when (identifier.keyCode) {
        4062, 4099, 4065 -> 0.5f to 0.842f // Acceso directo de aplicaciones
        KeyEvent.KEYCODE_DPAD_UP -> 0.5f to 0.22f
        KeyEvent.KEYCODE_DPAD_DOWN -> 0.5f to 0.46f
        KeyEvent.KEYCODE_DPAD_LEFT -> 0.38f to 0.34f
        KeyEvent.KEYCODE_DPAD_RIGHT -> 0.62f to 0.34f
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> 0.5f to 0.34f
        KeyEvent.KEYCODE_BACK -> 0.32f to 0.53f
        KeyEvent.KEYCODE_HOME -> 0.68f to 0.53f
        else -> null
    }
}

/** Devuelve la aplicación de fábrica únicamente para códigos verificados del mando TCL. */
private fun originalAppFor(identifier: ButtonIdentifier): String {
    return when (identifier.keyCode) {
        4062 -> "Netflix"
        4065 -> "YouTube"
        4099 -> "Amazon Prime Video"
        else -> "No identificada"
    }
}
