package dev.cavatz.keybindertv

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.backup.GitHubBackupManager
import dev.cavatz.keybindertv.data.repository.KeyBinderConfigSerializer
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.data.repository.MappingRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Nom du fichier DataStore sur le disque */
private const val DATASTORE_FILE_NAME: String = "keybinder_config.json"

/** Extension DataStore liée au contexte Application */
private val Context.configDataStore: DataStore<KeyBinderConfig> by dataStore(
    fileName = DATASTORE_FILE_NAME,
    serializer = KeyBinderConfigSerializer
)

/**
 * Classe Application centrale de KeyBinderTv.
 * Initialise le DataStore et le repository au démarrage du processus.
 * Fournit une référence globale au repository pour le service d'accessibilité.
 */
class KeyBinderTvApp : Application() {

    /** Scope de vie lié à l'application — survit aux recreations d'Activity */
    private val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Repository partagé entre l'UI et le service d'accessibilité */
    lateinit var mappingRepository: MappingRepository
        private set

    lateinit var githubBackupManager: GitHubBackupManager
        private set

    override fun onCreate() {
        super.onCreate()
        mappingRepository = MappingRepositoryImpl(
            dataStore = configDataStore,
            scope = applicationScope
        )
        githubBackupManager = GitHubBackupManager(
            context = this,
            repository = mappingRepository,
            scope = applicationScope
        )
    }
}
