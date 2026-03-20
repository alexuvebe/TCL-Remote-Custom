package dev.cavatz.keybindertv.data.export

import dev.cavatz.keybindertv.data.model.ActionTarget
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour [ConfigExporter] et [ConfigImporter].
 * Tests purs JVM — aucune dépendance Android (pas de Bitmap, pas de Context).
 */
class ConfigExporterImporterTest {

    private lateinit var exporter: ConfigExporter
    private lateinit var importer: ConfigImporter

    @Before
    fun setUp() {
        exporter = ConfigExporter()
        importer = ConfigImporter()
    }

    // ─── Tests d'export ─────────────────────────────────────────────────

    @Test
    fun `exportToJson should produce non-empty string`() {
        // Donnée : configuration minimale
        val config = KeyBinderConfig()

        // Acte
        val result: String = exporter.exportToJson(config)

        // Assertion : JSON non vide et contient les champs clés
        assertTrue("Le JSON ne doit pas être vide", result.isNotBlank())
        assertTrue("Le JSON doit contenir le champ version", result.contains("version"))
        assertTrue("Le JSON doit contenir le champ mappings", result.contains("mappings"))
    }

    @Test
    fun `exportToJson should produce pretty-printed JSON`() {
        // Donnée
        val config = KeyBinderConfig()

        // Acte
        val result: String = exporter.exportToJson(config)

        // Assertion : JSON indenté (contient des sauts de ligne)
        assertTrue("Le JSON doit être indenté (pretty print)", result.contains("\n"))
    }

    // ─── Tests de roundtrip ─────────────────────────────────────────────

    @Test
    fun `test_export_then_import_roundtrip`() {
        // Donnée : configuration complète avec mappings
        val original = KeyBinderConfig(
            version = 1,
            mappings = listOf(
                KeyMapping(
                    id = "test-uuid-1",
                    button = ButtonIdentifier(keyCode = 228, userLabel = "YouTube"),
                    shortPressAction = ActionTarget("com.google.android.youtube.tv", "YouTube"),
                    longPressAction = ActionTarget("com.netflix.ninja", "Netflix"),
                    isEnabled = true
                )
            ),
            showOverlayNotification = false,
            longPressThresholdMs = 750L
        )

        // Acte : export puis import
        val jsonString: String = exporter.exportToJson(original)
        val importResult: ConfigImporter.ImportResult = importer.importFromJson(jsonString)

        // Assertion : la config importée est identique à l'originale
        assertTrue("L'import doit réussir", importResult is ConfigImporter.ImportResult.Success)
        val imported = (importResult as ConfigImporter.ImportResult.Success).config
        assertEquals("Les configs doivent être égales après roundtrip", original, imported)
    }

    @Test
    fun `test_import_invalid_json_returns_error`() {
        // Donnée : JSON malformé
        val invalidJson = "{ version: pas_un_nombre, mappings: [ "

        // Acte
        val result: ConfigImporter.ImportResult = importer.importFromJson(invalidJson)

        // Assertion : erreur retournée
        assertTrue(
            "Un JSON malformé doit retourner ImportResult.Error",
            result is ConfigImporter.ImportResult.Error
        )
    }

    @Test
    fun `test_import_missing_version_returns_error`() {
        // Donnée : JSON sans champ version (version = 0 par défaut kotlinx → invalid)
        // On simule un JSON avec version explicitement à 0
        val jsonWithVersion0 = """{"version":0,"mappings":[],"showOverlayNotification":true,"longPressThresholdMs":500}"""

        // Acte
        val result: ConfigImporter.ImportResult = importer.importFromJson(jsonWithVersion0)

        // Assertion : la validation rejette version < 1
        assertTrue(
            "Une version 0 doit retourner ImportResult.Error",
            result is ConfigImporter.ImportResult.Error
        )
        val error = result as ConfigImporter.ImportResult.Error
        assertTrue("Le message doit mentionner la version", error.message.contains("version", ignoreCase = true))
    }

    @Test
    fun `test_import_empty_mappings_succeeds`() {
        // Donnée : configuration valide sans mappings
        val jsonEmptyMappings = """{"version":1,"mappings":[],"showOverlayNotification":true,"longPressThresholdMs":500}"""

        // Acte
        val result: ConfigImporter.ImportResult = importer.importFromJson(jsonEmptyMappings)

        // Assertion : une liste de mappings vide est valide
        assertTrue(
            "Une config sans mappings doit réussir l'import",
            result is ConfigImporter.ImportResult.Success
        )
        val success = result as ConfigImporter.ImportResult.Success
        assertTrue("La liste de mappings doit être vide", success.config.mappings.isEmpty())
    }

    @Test
    fun `import should reject mapping with blank id`() {
        // Donnée : JSON avec un mapping dont l'id est vide
        val jsonWithBlankId = """
            {
              "version": 1,
              "mappings": [
                {
                  "id": "",
                  "button": {"keyCode": 228, "sourcePackage": null, "userLabel": "Test"},
                  "isEnabled": true
                }
              ],
              "showOverlayNotification": true,
              "longPressThresholdMs": 500
            }
        """.trimIndent()

        // Acte
        val result: ConfigImporter.ImportResult = importer.importFromJson(jsonWithBlankId)

        // Assertion : id vide → erreur de validation
        assertTrue(
            "Un mapping avec id vide doit retourner ImportResult.Error",
            result is ConfigImporter.ImportResult.Error
        )
    }

    @Test
    fun `import should reject blank json`() {
        // Donnée : chaîne vide
        val result: ConfigImporter.ImportResult = importer.importFromJson("")

        // Assertion
        assertTrue(
            "Un JSON vide doit retourner ImportResult.Error",
            result is ConfigImporter.ImportResult.Error
        )
    }

    @Test
    fun `roundtrip should preserve all config fields`() {
        // Donnée : configuration avec champs non-défaut
        val original = KeyBinderConfig(
            version = 1,
            mappings = emptyList(),
            showOverlayNotification = false,
            longPressThresholdMs = 1000L
        )

        // Acte
        val json: String = exporter.exportToJson(original)
        val result = importer.importFromJson(json) as ConfigImporter.ImportResult.Success

        // Assertions individuelles
        assertEquals(false, result.config.showOverlayNotification)
        assertEquals(1000L, result.config.longPressThresholdMs)
        assertEquals(1, result.config.version)
    }
}
