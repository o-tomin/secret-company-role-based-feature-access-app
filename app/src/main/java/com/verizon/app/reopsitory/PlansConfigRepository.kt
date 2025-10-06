package com.verizon.app.reopsitory

import android.util.Log
import androidx.datastore.core.DataStore
import com.verizon.app.Constants.TAG
import com.verizon.app.api.ConfigApi
import com.verizon.app.di.IoDispatcher
import com.verizon.app.di.PlansConfigSerializer.defaultValue
import com.verizon.app.model.PlansConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for managing the application's configuration data ([PlansConfig]).
 *
 * It acts as a **single source of truth** between the remote configuration endpoint
 * (fetched via [ConfigApi]) and the locally cached data (stored using [DataStore]).
 *
 * ## Responsibilities
 * - Fetch the latest configuration file (`plans_matrix.yml`) from the network.
 * - Cache the fetched configuration locally using Jetpack DataStore.
 * - Serve the cached configuration when offline or when network fetch fails.
 * - Provide safe read and write access to configuration data across the app.
 *
 * ## Threading
 * All operations run on the [IoDispatcher] to avoid blocking the main thread,
 * since DataStore and network I/O are both suspendable, I/O-bound tasks.
 *
 * ## Fallback behavior
 * - If the network fetch fails, returns the **cached** config if available.
 * - If neither cached nor fetched config is available, falls back to
 *   the serializer’s [defaultValue].
 *
 * ## Example usage
 * ```kotlin
 * class ConfigViewModel @Inject constructor(
 *     private val repository: PlansConfigRepository
 * ) : ViewModel() {
 *
 *     fun refresh() = viewModelScope.launch {
 *         val config = repository.fetchAndSet()
 *         println("Loaded config v${config.version}")
 *     }
 * }
 * ```
 *
 * @property api Retrofit API interface for remote configuration.
 * @property store Jetpack [DataStore] instance persisting [PlansConfig].
 * @property ioDispatcher The I/O coroutine dispatcher used for safe background execution.
 *
 * @see PlansConfig
 * @see ConfigApi
 */
@Singleton
class PlansConfigRepository @Inject constructor(
    private val api: ConfigApi,
    private val store: DataStore<PlansConfig>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Fetches the configuration from the network and updates the local DataStore.
     *
     * - On success: saves the new config and returns it.
     * - On failure: logs the error and returns the cached configuration (if available).
     *
     * @return The latest [PlansConfig] from network or cache.
     */
    suspend fun fetchAndSet(): PlansConfig = withContext(ioDispatcher) {
        val fromNetwork = runCatching {
            api.fetchConfig().also { fresh ->
                store.updateData { fresh }
                Log.d(TAG, "Config fetched & saved (version=${fresh.version})")
            }
        }.onFailure {
            Log.d(TAG, "Remote fetch failed, serving cached config")
        }.getOrNull()

        fromNetwork ?: store.data.first()
    }

    /**
     * Returns the current [PlansConfig] stored in DataStore.
     *
     * This does not trigger a network request.
     *
     * @return The cached configuration, or the default if none has been set yet.
     */
    suspend fun get(): PlansConfig = withContext(ioDispatcher) {
        store.data.first()
    }

    /**
     * Replaces the existing configuration in DataStore with a new one.
     *
     * @param newConfig The new configuration to persist.
     */
    suspend fun set(newConfig: PlansConfig) = withContext(ioDispatcher) {
        store.updateData { newConfig }
    }

    /**
     * Atomically updates the configuration using a transformation function.
     *
     * @param transform A lambda that receives the current [PlansConfig] and returns an updated one.
     */
    suspend fun update(transform: (PlansConfig) -> PlansConfig) = withContext(ioDispatcher) {
        store.updateData { transform(it) }
    }

    /**
     * Checks whether the current DataStore content equals the default configuration.
     *
     * This is useful to determine if no valid configuration has been fetched yet.
     *
     * @return `true` if DataStore currently holds the serializer’s [defaultValue],
     *         `false` if it contains a fetched or updated configuration.
     */
    suspend fun isSetToDefaultConfig(): Boolean = withContext(ioDispatcher) {
        store.data.first() == defaultValue
    }
}
