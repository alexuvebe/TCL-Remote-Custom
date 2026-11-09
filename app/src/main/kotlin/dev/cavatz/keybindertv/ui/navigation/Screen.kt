package dev.cavatz.keybindertv.ui.navigation

/** Destinos de navegación de la aplicación. */
sealed class Screen(val route: String) {

    /** Pantalla de inicio */
    object Home : Screen("home")

    /** Lista de asignaciones configuradas */
    object MappingList : Screen("mappings")

    /** Detección de botones del mando */
    object Learning : Screen("learning/{mappingId}") {
        const val ROUTE: String = "learning/{mappingId}"
        const val ARG_MAPPING_ID: String = "mappingId"
        fun createRoute(mappingId: String): String = "learning/$mappingId"
    }

    /** Selector de aplicación para una asignación */
    data class AppChooser(
        val mappingId: String,
        val pressType: String
    ) : Screen(ROUTE) {
        companion object {
            const val ROUTE: String = "appchooser/{mappingId}/{pressType}"
            const val ARG_MAPPING_ID: String = "mappingId"
            const val ARG_PRESS_TYPE: String = "pressType"

            fun createRoute(mappingId: String, pressType: String): String =
                "appchooser/$mappingId/$pressType"
        }
    }

    /** Ajustes globales */
    object Settings : Screen("settings")
}