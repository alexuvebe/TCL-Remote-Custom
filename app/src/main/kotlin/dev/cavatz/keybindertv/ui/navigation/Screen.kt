package dev.cavatz.keybindertv.ui.navigation

/** Destinos de navegación de la aplicación. */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MappingList : Screen("mappings")
    object Learning : Screen("learning/{mappingId}") {
        const val ROUTE: String = "learning/{mappingId}"
        const val ARG_MAPPING_ID: String = "mappingId"
        fun createRoute(mappingId: String): String = "learning/$mappingId"
    }
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
    object Settings : Screen("settings")
    /** Pantalla opcional para guardar una copia de seguridad; no permite importar. */
    object ImportExport : Screen("backup")
}