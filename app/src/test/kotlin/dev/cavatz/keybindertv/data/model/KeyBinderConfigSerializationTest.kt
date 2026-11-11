package dev.cavatz.keybindertv.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests de sérialisation/désérialisation de [KeyBinderConfig] via kotlinx.serialization-json.
 * Vérifie l'intégrité des données lors d'un roundtrip JSON.
 */
class KeyBinderConfigSerializationTest {

    private lateinit var json: Json

    @Before
    fun setUp() {
        json = Json { ignoreUnknownKeys = true }
    }

    @Test
    fun `should serialize and deserialize empty config correctly`() {
        // Donnée : configuration vide avec valeurs par défaut
        val original = KeyBinderConfig()

        // Acte : sérialisation puis désérialisation
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded: KeyBinderConfig = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        // Assertion : les deux objets sont égaux
        assertEquals(original, decoded)
        assertEquals(1, decoded.version)
        assertEquals(emptyList<KeyMapping>(), decoded.mappings)
        assertEquals(true, decoded.showOverlayNotification)
        assertEquals(500L, decoded.longPressThresholdMs)
    }

    @Test
    fun `should serialize and deserialize config with multiple mappings`() {
        // Donnée : configuration avec deux mappings complets
        val mapping1 = KeyMapping(
            id = "uuid-1",
            button = ButtonIdentifier(keyCode = 228, userLabel = "YouTube"),
            shortPressAction = ActionTarget("com.google.android.youtube.tv", "YouTube"),
            longPressAction = ActionTarget("com.netflix.ninja", "Netflix"),
            isEnabled = true
        )
        val mapping2 = KeyMapping(
            id = "uuid-2",
            button = ButtonIdentifier(
                keyCode = 229,
                sourcePackage = "com.tcl.launcher",
                userLabel = "TCL Channel"
            ),
            shortPressAction = ActionTarget("com.pluto.tv", "Pluto TV"),
            isEnabled = false
        )
        val original = KeyBinderConfig(
            version = 1,
            mappings = listOf(mapping1, mapping2),
            showOverlayNotification = false,
            longPressThresholdMs = 700L
        )

        // Acte : roundtrip JSON
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded: KeyBinderConfig = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        // Assertion : égalité complète
        assertEquals(original, decoded)
        assertEquals(2, decoded.mappings.size)
        assertEquals("uuid-1", decoded.mappings[0].id)
        assertEquals(228, decoded.mappings[0].button.keyCode)
        assertEquals("Netflix", decoded.mappings[0].longPressAction?.appLabel)
        assertEquals(false, decoded.mappings[1].isEnabled)
    }

    @Test
    fun `should handle null shortPressAction with non-null longPressAction`() {
        // Donnée : seul l'appui long est configuré
        val mapping = KeyMapping(
            id = "uuid-long-only",
            button = ButtonIdentifier(keyCode = 230, userLabel = "Hub"),
            shortPressAction = null,
            longPressAction = ActionTarget("com.amazon.amazonvideo.livingroom", "Prime Video"),
            isEnabled = true
        )
        val original = KeyBinderConfig(mappings = listOf(mapping))

        // Acte : roundtrip JSON
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded: KeyBinderConfig = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        // Assertion : null préservé, longPressAction intact
        assertEquals(original, decoded)
        assertNull(decoded.mappings[0].shortPressAction)
        assertNotNull(decoded.mappings[0].longPressAction)
        assertEquals("Prime Video", decoded.mappings[0].longPressAction?.appLabel)
    }

    @Test
    fun `should preserve all ButtonIdentifier fields through serialization`() {
        // Donnée : bouton avec tous les champs renseignés
        val button = ButtonIdentifier(
            keyCode = 999,
            sourcePackage = "com.tcl.launcher",
            userLabel = "Bouton mystère"
        )
        val original = KeyBinderConfig(
            mappings = listOf(
                KeyMapping(id = "uuid-full", button = button)
            )
        )

        // Acte : roundtrip
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded: KeyBinderConfig = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        // Assertion : tous les champs du ButtonIdentifier sont préservés
        val decodedButton: ButtonIdentifier = decoded.mappings[0].button
        assertEquals(999, decodedButton.keyCode)
        assertEquals("com.tcl.launcher", decodedButton.sourcePackage)
        assertEquals("Bouton mystère", decodedButton.userLabel)
    }

    @Test
    fun `should preserve global and independent press switches`() {
        val original = KeyBinderConfig(
            globalEnabled = false,
            mappings = listOf(
                KeyMapping(
                    id = "independent-actions",
                    button = ButtonIdentifier(keyCode = 4099, userLabel = "Prime Video"),
                    shortPressAction = ActionTarget("com.example.short", "Corta"),
                    longPressAction = ActionTarget("com.example.long", "Larga"),
                    shortPressEnabled = false,
                    longPressEnabled = true,
                    isEnabled = true
                )
            )
        )

        val encoded = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals(false, decoded.globalEnabled)
        assertEquals(false, decoded.mappings.single().shortPressEnabled)
        assertEquals(true, decoded.mappings.single().longPressEnabled)
    }

    @Test
    fun `should serialize custom longPressThresholdMs correctly`() {
        // Donnée : seuil personnalisé
        val original = KeyBinderConfig(longPressThresholdMs = 1200L)

        // Acte : roundtrip
        val encoded: String = json.encodeToString(KeyBinderConfig.serializer(), original)
        val decoded: KeyBinderConfig = json.decodeFromString(KeyBinderConfig.serializer(), encoded)

        // Assertion : valeur Long préservée
        assertEquals(1200L, decoded.longPressThresholdMs)
    }
}
