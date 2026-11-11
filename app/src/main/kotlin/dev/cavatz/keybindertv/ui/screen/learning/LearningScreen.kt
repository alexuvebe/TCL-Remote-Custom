package dev.cavatz.keybindertv.ui.screen.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import dev.cavatz.keybindertv.R
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.service.KeyInterceptorService
import dev.cavatz.keybindertv.ui.navigation.Screen
import kotlinx.coroutines.launch

/** Espacement vertical entre les éléments */
private val VerticalSpacing = 12.dp

/** Padding global de l'écran */
private val ScreenPadding = 16.dp

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
    TextButton(onClick = onCancel) {
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
        identifier.keyCode?.toString() ?: identifier.sourcePackage ?: "Bouton inconnu"
    }
    Text(
        text = "Bouton détecté : $label",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(VerticalSpacing))
    Row(horizontalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
        Button(onClick = onConfirm) {
            Text(text = stringResource(R.string.learning_confirm))
        }
        TextButton(onClick = onRetry) {
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
        Button(onClick = onRetry) {
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
