package dev.cavatz.keybindertv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.ui.screen.appchooser.AppChooserScreen
import dev.cavatz.keybindertv.ui.screen.home.HomeScreen
import dev.cavatz.keybindertv.ui.screen.learning.LearningScreen
import dev.cavatz.keybindertv.ui.screen.mappings.MappingListScreen
import dev.cavatz.keybindertv.ui.screen.settings.SettingsScreen

@Composable
fun KeyBinderNavGraph(
    navController: NavHostController = rememberNavController(),
    mappingRepository: MappingRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                navController = navController,
                repository = mappingRepository
            )
        }

        composable(route = Screen.MappingList.route) {
            MappingListScreen(
                navController = navController,
                repository = mappingRepository
            )
        }

        composable(
            route = Screen.Learning.ROUTE,
            arguments = listOf(
                navArgument(Screen.Learning.ARG_MAPPING_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mappingId: String = backStackEntry.arguments
                ?.getString(Screen.Learning.ARG_MAPPING_ID) ?: return@composable
            LearningScreen(
                navController = navController,
                mappingId = mappingId,
                repository = mappingRepository
            )
        }

        composable(
            route = Screen.AppChooser.ROUTE,
            arguments = listOf(
                navArgument(Screen.AppChooser.ARG_MAPPING_ID) { type = NavType.StringType },
                navArgument(Screen.AppChooser.ARG_PRESS_TYPE) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mappingId: String = backStackEntry.arguments
                ?.getString(Screen.AppChooser.ARG_MAPPING_ID) ?: return@composable
            val pressType: String = backStackEntry.arguments
                ?.getString(Screen.AppChooser.ARG_PRESS_TYPE) ?: return@composable
            AppChooserScreen(
                navController = navController,
                repository = mappingRepository,
                mappingId = mappingId,
                pressType = pressType
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                repository = mappingRepository
            )
        }
    }
}