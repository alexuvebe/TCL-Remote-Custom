package dev.cavatz.keybindertv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Graphe de navigation principal de l'application.
 * Chaque écran est déclaré avec un composable vide pour la Phase 1.
 * Les implémentations seront ajoutées lors des phases suivantes.
 *
 * @param navController Contrôleur de navigation partagé entre les écrans
 */
@Composable
fun KeyBinderNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreenPlaceholder()
        }

        composable(route = Screen.MappingList.route) {
            MappingListScreenPlaceholder()
        }

        composable(route = Screen.Learning.route) {
            LearningScreenPlaceholder()
        }

        composable(
            route = Screen.AppChooser.ROUTE,
            arguments = listOf(
                navArgument(Screen.AppChooser.ARG_MAPPING_ID) { type = NavType.StringType },
                navArgument(Screen.AppChooser.ARG_PRESS_TYPE) { type = NavType.StringType }
            )
        ) {
            AppChooserScreenPlaceholder()
        }

        composable(route = Screen.Settings.route) {
            SettingsScreenPlaceholder()
        }

        composable(route = Screen.ImportExport.route) {
            ImportExportScreenPlaceholder()
        }
    }
}

// ---- Composables squelettes — Phase 1 ----
// Chaque écran sera implémenté dans sa propre phase de développement.

@Composable
private fun HomeScreenPlaceholder() { /* Phase 3 */ }

@Composable
private fun MappingListScreenPlaceholder() { /* Phase 3 */ }

@Composable
private fun LearningScreenPlaceholder() { /* Phase 3 */ }

@Composable
private fun AppChooserScreenPlaceholder() { /* Phase 3 */ }

@Composable
private fun SettingsScreenPlaceholder() { /* Phase 3 */ }

@Composable
private fun ImportExportScreenPlaceholder() { /* Phase 4 */ }
