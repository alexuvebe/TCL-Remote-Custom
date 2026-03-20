package dev.cavatz.keybindertv.data.export

import dev.cavatz.keybindertv.data.model.ActionTarget
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitaires pour [QrCodeCodec].
 *
 * Note : les tests qui créent des Bitmap réels nécessitent un environnement Android
 * (via Robolectric ou tests instrumentés). Ces tests vérifient la logique de
 * sérialisation JSON et les constantes sans instancier de Bitmap.
 */
class QrCodeCodecTest {

    // ─── Tests sur les constantes ────────────────────────────────────────

    @Test
    fun `test_max_qr_size_constant`() {
        // Assertion : la valeur de la constante est bien 2953 (spec QR niveau L)
        assertEquals(
            "MAX_QR_BYTES doit valoir 2953",
            2953,
            QrCodeCodec.MAX_QR_BYTES
        )
    }

    // ─── Tests sur la sérialisation JSON (sans Bitmap) ───────────────────

    @Test
    fun `small config serializes to valid json within qr limit`() {
        // Donnée : configuration avec 1 mapping simple
        val config = KeyBinderConfig(
            version = 1,
            mappings = listOf(
                KeyMapping(
                    id = "uuid-test-1",
                    button = ButtonIdentifier(keyCode = 228, userLabel = "Test"),
                    shortPressAction = ActionTarget("com.example.app", "Test App"),
                    isEnabled = true
                )
            )
        )
        val json = Json { encodeDefaults = true }
        val jsonString: String = json.encodeToString(KeyBinderConfig.serializer(), config)
        val byteCount: Int = jsonString.toByteArray(Charsets.UTF_8).size

        // Assertion : la config tient dans la limite QR
        assert(byteCount <= QrCodeCodec.MAX_QR_BYTES) {
            "Une config avec 1 mapping doit tenir dans ${ QrCodeCodec.MAX_QR_BYTES } bytes (taille actuelle : $byteCount)"
        }
    }

    @Test
    fun `test_too_large_config_exceeds_qr_limit`() {
        // Donnée : configuration avec 100 mappings avec des labels longs
        val longLabel = "com.example.verylongpackagename.application.with.deeply.nested.package"
        val mappings = (1..100).map { index ->
            KeyMapping(
                id = "uuid-$index-padding-to-make-it-longer",
                button = ButtonIdentifier(
                    keyCode = index,
                    sourcePackage = longLabel,
                    userLabel = "Application très longue numéro $index avec un nom très descriptif"
                ),
                shortPressAction = ActionTarget(
                    packageName = "$longLabel.$index",
                    appLabel = "App $index avec un nom d'affichage très long pour saturer le QR"
                ),
                longPressAction = ActionTarget(
                    packageName = "$longLabel.secondary.$index",
                    appLabel = "App secondaire $index avec un nom encore plus long"
                ),
                isEnabled = true
            )
        }
        val config = KeyBinderConfig(mappings = mappings)
        val json = Json { encodeDefaults = true }
        val jsonString: String = json.encodeToString(KeyBinderConfig.serializer(), config)
        val byteCount: Int = jsonString.toByteArray(Charsets.UTF_8).size

        // Assertion : la config dépasse bien la limite QR
        assert(byteCount > QrCodeCodec.MAX_QR_BYTES) {
            "Une config avec 100 mappings longs doit dépasser ${ QrCodeCodec.MAX_QR_BYTES } bytes (taille actuelle : $byteCount)"
        }
    }

    @Test
    fun `json roundtrip for qr content preserves config`() {
        // Donnée : configuration à encoder puis décoder (simulation sans Bitmap)
        val original = KeyBinderConfig(
            version = 1,
            mappings = listOf(
                KeyMapping(
                    id = "roundtrip-uuid",
                    button = ButtonIdentifier(keyCode = 999, userLabel = "Custom"),
                    shortPressAction = ActionTarget("com.test.app", "Test"),
                    isEnabled = true
                )
            ),
            showOverlayNotification = true,
            longPressThresholdMs = 600L
        )

        // Acte : simuler encode → decode via JSON pur (sans Bitmap)
        val codecJson = Json { encodeDefaults = true }
        val encodedJson: String = codecJson.encodeToString(KeyBinderConfig.serializer(), original)
        val importer = ConfigImporter()
        val importResult: ConfigImporter.ImportResult = importer.importFromJson(encodedJson)

        // Assertion : la config survit au cycle encode/decode
        assert(importResult is ConfigImporter.ImportResult.Success) {
            "Le roundtrip JSON doit réussir : $importResult"
        }
        val decoded = (importResult as ConfigImporter.ImportResult.Success).config
        assertEquals("Les configs doivent être égales après roundtrip JSON", original, decoded)
    }

    @Test
    fun `empty config should be within qr size limit`() {
        // Donnée : configuration minimale vide
        val config = KeyBinderConfig()
        val json = Json { encodeDefaults = true }
        val byteCount: Int = json.encodeToString(KeyBinderConfig.serializer(), config)
            .toByteArray(Charsets.UTF_8).size

        // Assertion : une config vide tient largement dans un QR
        assert(byteCount <= QrCodeCodec.MAX_QR_BYTES) {
            "Une config vide ne doit pas dépasser ${ QrCodeCodec.MAX_QR_BYTES } bytes"
        }
    }
}
