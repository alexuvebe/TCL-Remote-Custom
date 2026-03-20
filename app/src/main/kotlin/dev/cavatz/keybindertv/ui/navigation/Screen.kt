package dev.cavatz.keybindertv.ui.navigation

/**
 * Destinations de navigation de l'application.
 * Chaque objet/classe scellée représente un écran avec sa route associée.
 */
sealed class Screen(val route: String) {

    /** Écran d'accueil — vue synthétique des mappings actifs */
    object Home : Screen("home")

    /** Liste complète des mappings configurés */
    object MappingList : Screen("mappings")

    /**
     * Mode apprentissage — détection des boutons de télécommande.
     *
     * @param mappingId Identifiant UUID du mapping en cours de création
     */
    object Learning : Screen("learning/{mappingId}") {
        companion object {
            /** Route template avec argument */
            const val ROUTE: String = "learning/{mappingId}"

            /** Nom de l'argument de navigation */
            const val ARG_MAPPING_ID: String = "mappingId"

            /** Construit la route concrète avec l'identifiant du mapping */
            fun createRoute(mappingId: String): String = "learning/$mappingId"
        }
    }

    /**
     * Sélecteur d'application pour un mapping donné.
     *
     * @param mappingId Identifiant UUID du mapping à modifier
     * @param pressType Type d'appui : "short" ou "long"
     */
    data class AppChooser(
        val mappingId: String,
        val pressType: String
    ) : Screen(ROUTE) {

        companion object {
            /** Route template utilisée pour la déclaration dans le NavGraph */
            const val ROUTE: String = "appchooser/{mappingId}/{pressType}"

            /** Constantes pour les noms d'arguments de navigation */
            const val ARG_MAPPING_ID: String = "mappingId"
            const val ARG_PRESS_TYPE: String = "pressType"

            /** Construit la route concrète avec les valeurs d'arguments */
            fun createRoute(mappingId: String, pressType: String): String =
                "appchooser/$mappingId/$pressType"
        }
    }

    /** Écran de paramètres globaux */
    object Settings : Screen("settings")

    /** Import / Export de la configuration via QR code ou fichier */
    object ImportExport : Screen("importexport")
}
