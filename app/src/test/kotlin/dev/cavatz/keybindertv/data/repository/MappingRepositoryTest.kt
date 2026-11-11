package dev.cavatz.keybindertv.data.repository

import androidx.datastore.core.DataStore
import dev.cavatz.keybindertv.data.model.ActionTarget
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DataStore en mémoire pour les tests — évite toute écriture sur disque.
 * Simule le comportement de DataStore via un MutableStateFlow.
 */
private class InMemoryDataStore(
    initialValue: KeyBinderConfig = KeyBinderConfig()
) : DataStore<KeyBinderConfig> {

    private val state: MutableStateFlow<KeyBinderConfig> = MutableStateFlow(initialValue)

    override val data: Flow<KeyBinderConfig> = state

    override suspend fun updateData(
        transform: suspend (t: KeyBinderConfig) -> KeyBinderConfig
    ): KeyBinderConfig {
        val newValue: KeyBinderConfig = transform(state.value)
        state.update { newValue }
        return newValue
    }
}

/**
 * Tests unitaires de [MappingRepositoryImpl].
 * Toutes les interactions avec DataStore sont simulées en mémoire.
 */
class MappingRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val repositoryScope = CoroutineScope(SupervisorJob() + testDispatcher)

    private lateinit var dataStore: InMemoryDataStore
    private lateinit var repository: MappingRepositoryImpl

    // ---- Fixtures ----

    private val buttonYoutube = ButtonIdentifier(keyCode = 228, userLabel = "YouTube")
    private val actionYoutube = ActionTarget("com.google.android.youtube.tv", "YouTube")
    private val actionNetflix = ActionTarget("com.netflix.ninja", "Netflix")

    private val mappingYoutube = KeyMapping(
        id = "uuid-youtube",
        button = buttonYoutube,
        shortPressAction = actionYoutube,
        isEnabled = true
    )
    private val mappingTcl = KeyMapping(
        id = "uuid-tcl",
        button = ButtonIdentifier(keyCode = 229, userLabel = "TCL Channel"),
        shortPressAction = ActionTarget("com.tcl.tclchannels", "TCL Channel"),
        isEnabled = true
    )

    @org.junit.After
    fun tearDown() {
        repositoryScope.cancel()
        testScope.cancel()
    }

    @Before
    fun setUp() {
        dataStore = InMemoryDataStore()
        repository = MappingRepositoryImpl(dataStore = dataStore, scope = repositoryScope)
    }

    @Test
    fun `should add mapping to configFlow`() = testScope.runTest {
        // Acte
        repository.addMapping(mappingYoutube)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertEquals(1, config.mappings.size)
        assertEquals("uuid-youtube", config.mappings[0].id)
    }

    @Test
    fun `should add multiple mappings and preserve order`() = testScope.runTest {
        // Acte
        repository.addMapping(mappingYoutube)
        repository.addMapping(mappingTcl)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertEquals(2, config.mappings.size)
        assertEquals("uuid-youtube", config.mappings[0].id)
        assertEquals("uuid-tcl", config.mappings[1].id)
    }

    @Test
    fun `should throw when adding mapping with duplicate id`() = testScope.runTest {
        // Pré-condition
        repository.addMapping(mappingYoutube)

        // Acte + Assertion
        var exceptionThrown = false
        try {
            repository.addMapping(mappingYoutube.copy(button = buttonYoutube))
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Doit lever IllegalArgumentException pour id dupliqué", exceptionThrown)
    }

    @Test
    fun `should remove correct mapping by id`() = testScope.runTest {
        // Pré-condition
        repository.addMapping(mappingYoutube)
        repository.addMapping(mappingTcl)

        // Acte
        repository.removeMapping("uuid-youtube")

        // Assertion : seul TCL reste
        val config: KeyBinderConfig = repository.configFlow.first()
        assertEquals(1, config.mappings.size)
        assertEquals("uuid-tcl", config.mappings[0].id)
    }

    @Test
    fun `should be silent when removing unknown id`() = testScope.runTest {
        // Pré-condition
        repository.addMapping(mappingYoutube)

        // Acte : suppression d'un id inexistant
        repository.removeMapping("uuid-inconnu")

        // Assertion : rien ne change
        val config: KeyBinderConfig = repository.configFlow.first()
        assertEquals(1, config.mappings.size)
    }

    @Test
    fun `should toggle mapping to disabled`() = testScope.runTest {
        // Pré-condition
        repository.addMapping(mappingYoutube)

        // Acte
        repository.toggleMapping("uuid-youtube", enabled = false)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertFalse(config.mappings[0].isEnabled)
    }

    @Test
    fun `should toggle mapping back to enabled`() = testScope.runTest {
        // Pré-condition : mapping désactivé au départ
        repository.addMapping(mappingYoutube.copy(isEnabled = false))

        // Acte
        repository.toggleMapping("uuid-youtube", enabled = true)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertTrue(config.mappings[0].isEnabled)
    }

    @Test
    fun `should update mapping fields correctly`() = testScope.runTest {
        // Pré-condition
        repository.addMapping(mappingYoutube)

        // Acte : on ajoute une action longPressAction
        val updatedMapping: KeyMapping = mappingYoutube.copy(longPressAction = actionNetflix)
        repository.updateMapping(updatedMapping)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertEquals("Netflix", config.mappings[0].longPressAction?.appLabel)
        assertEquals(actionYoutube, config.mappings[0].shortPressAction)
    }

    @Test
    fun `should update settings — showOverlayNotification and longPressThresholdMs`() = testScope.runTest {
        // Acte
        repository.updateSettings(showOverlay = false, longPressThresholdMs = 800L)

        // Assertion
        val config: KeyBinderConfig = repository.configFlow.first()
        assertFalse(config.showOverlayNotification)
        assertEquals(800L, config.longPressThresholdMs)
    }

    @Test
    fun `should return synchronous config via getConfigSync after mutation`() = testScope.runTest {
        // Pré-condition : on ajoute un mapping
        repository.addMapping(mappingYoutube)
        // Force le collecteur du cache à s'exécuter
        testDispatcher.scheduler.advanceUntilIdle()

        // Acte
        val syncConfig: KeyBinderConfig = repository.getConfigSync()

        // Assertion : le cache synchrone reflète l'état actuel
        assertEquals(1, syncConfig.mappings.size)
        assertEquals("uuid-youtube", syncConfig.mappings[0].id)
    }

    @Test
    fun `should return default config via getConfigSync before any mutation`() {
        // Acte : appel synchrone sans aucune mutation préalable
        val syncConfig: KeyBinderConfig = repository.getConfigSync()

        // Assertion : valeurs par défaut
        assertEquals(KeyBinderConfig(), syncConfig)
    }
}
