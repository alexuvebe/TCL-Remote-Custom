package dev.cavatz.keybindertv.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires de la logique de déduplication de [KeyInterceptorService].
 *
 * La logique est extraite dans [DeduplicationLogic] pour pouvoir être testée
 * en JVM pur, sans dépendance à Android framework.
 */
class DeduplicationTest {

    /**
     * Classe extraite de la logique isDuplicate de [KeyInterceptorService],
     * injectable avec une horloge simulable pour les tests.
     *
     * @param windowMs      Fenêtre de déduplication en millisecondes
     * @param currentTimeMs Fonction fournissant l'heure courante (injectable pour les tests)
     */
    class DeduplicationLogic(
        private val windowMs: Long = 2000L,
        private val currentTimeMs: () -> Long = { System.currentTimeMillis() }
    ) {
        private val recentlyHandled: MutableMap<String, Long> = mutableMapOf()

        /**
         * Retourne true si [key] a déjà été traitée dans la fenêtre [windowMs].
         * Enregistre la clé si nouvelle. Nettoie les entrées expirées.
         */
        fun isDuplicate(key: String): Boolean {
            val now: Long = currentTimeMs()
            val last: Long? = recentlyHandled[key]

            if (last != null && now - last < windowMs) return true

            recentlyHandled[key] = now
            recentlyHandled.entries.removeIf { now - it.value > windowMs * 2 }
            return false
        }

        /** Retourne le nombre d'entrées actuellement en mémoire (pour vérifier le nettoyage). */
        fun entryCount(): Int = recentlyHandled.size
    }

    private var fakeTimeMs: Long = 0L
    private lateinit var dedup: DeduplicationLogic

    @Before
    fun setUp() {
        fakeTimeMs = 1_000_000L
        dedup = DeduplicationLogic(
            windowMs = 2000L,
            currentTimeMs = { fakeTimeMs }
        )
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test_same_key_within_window_is_duplicate`() {
        // Premier appel → pas un doublon
        assertFalse("Premier appel non dupliqué", dedup.isDuplicate("keycode_4"))

        // Deuxième appel dans la fenêtre → doublon
        fakeTimeMs += 500L
        assertTrue("Deuxième appel dans la fenêtre est dupliqué", dedup.isDuplicate("keycode_4"))
    }

    @Test
    fun `test_same_key_after_window_expiry_is_not_duplicate`() {
        assertFalse("Premier appel non dupliqué", dedup.isDuplicate("keycode_4"))

        // Avancer au-delà de la fenêtre
        fakeTimeMs += 2001L
        assertFalse("Appel après expiration non dupliqué", dedup.isDuplicate("keycode_4"))
    }

    @Test
    fun `test_different_keys_are_independent`() {
        assertFalse("Clé A non dupliquée", dedup.isDuplicate("keycode_4"))

        // Clé différente → pas un doublon même immédiatement après
        assertFalse("Clé B différente non dupliquée", dedup.isDuplicate("keycode_5"))
    }

    @Test
    fun `test_package_key_within_window_is_duplicate`() {
        assertFalse("Paquet source non dupliqué", dedup.isDuplicate("package_com.foo.app"))

        fakeTimeMs += 100L
        assertTrue("Paquet source dans la fenêtre est dupliqué", dedup.isDuplicate("package_com.foo.app"))
    }

    @Test
    fun `test_old_entries_are_cleaned_up`() {
        // Remplir avec plusieurs clés
        dedup.isDuplicate("key_1")
        dedup.isDuplicate("key_2")
        dedup.isDuplicate("key_3")

        val countAfterInserts: Int = dedup.entryCount()
        assertTrue("3 entrées présentes", countAfterInserts == 3)

        // Avancer au-delà de windowMs * 2 pour déclencher le nettoyage
        fakeTimeMs += 4001L

        // Un nouvel appel déclenche le nettoyage
        dedup.isDuplicate("key_new")

        // Seule la nouvelle clé doit rester
        val countAfterCleanup: Int = dedup.entryCount()
        assertTrue(
            "Seule la nouvelle entrée survit au nettoyage (attendu 1, obtenu $countAfterCleanup)",
            countAfterCleanup == 1
        )
    }

    @Test
    fun `test_map_does_not_grow_indefinitely`() {
        // Simuler 100 clés différentes sur 5000ms → toutes expirées après 4001ms
        repeat(100) { index ->
            fakeTimeMs += 50L
            dedup.isDuplicate("key_$index")
        }

        // Avancer pour expirer toutes les entrées
        fakeTimeMs += 4001L

        // Un seul appel déclenche le nettoyage
        dedup.isDuplicate("key_final")

        assertTrue(
            "Map ne doit pas dépasser 1 entrée après nettoyage",
            dedup.entryCount() == 1
        )
    }

    @Test
    fun `test_boundary_at_exact_window_edge_is_duplicate`() {
        assertFalse("Premier appel", dedup.isDuplicate("keycode_1"))

        // Exactement à la limite — still inside (strict less than)
        fakeTimeMs += 1999L
        assertTrue("À 1999ms dans une fenêtre de 2000ms → dupliqué", dedup.isDuplicate("keycode_1"))
    }

    @Test
    fun `test_boundary_just_after_window_is_not_duplicate`() {
        assertFalse("Premier appel", dedup.isDuplicate("keycode_1"))

        fakeTimeMs += 2000L
        assertFalse("À exactement 2000ms → non dupliqué", dedup.isDuplicate("keycode_1"))
    }
}
