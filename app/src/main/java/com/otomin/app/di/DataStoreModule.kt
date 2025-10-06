package com.otomin.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.otomin.app.di.PlansConfigSerializer.defaultValue
import com.otomin.app.model.Feature
import com.otomin.app.model.Plan
import com.otomin.app.model.PlanId
import com.otomin.app.model.PlansConfig
import com.otomin.app.model.Role
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Singleton

/**
 * Provides a [DataStore] instance for persisting and caching the [PlansConfig] model.
 *
 * ## Purpose
 * This module defines how the app stores, retrieves, and serializes configuration data
 * (plans, roles, and features) locally using Jetpack DataStore. It enables the app to:
 * - Persist the most recently fetched configuration between launches.
 * - Serve cached data when offline.
 * - Bootstrap a default fallback when no configuration exists yet.
 *
 * ## Installation scope
 * Annotated with [@InstallIn(SingletonComponent::class)], meaning:
 * - There is a single shared DataStore instance across the entire app process.
 * - The same instance is injected wherever [DataStore]<[PlansConfig]> is requested.
 *
 * ## Components
 * - `internalPlansDataStore`: Defines the delegate for the actual DataStore file.
 * - [PlansConfigSerializer]: Defines how [PlansConfig] objects are serialized/deserialized to JSON.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides a singleton [DataStore] instance for [PlansConfig].
     *
     * @param context The application [Context], required by DataStore to resolve file storage.
     * @return The shared [DataStore] instance managing persistence of [PlansConfig].
     *
     * This uses the delegated property [internalPlansDataStore], configured with the
     * [PlansConfigSerializer].
     */
    @Provides
    @Singleton
    fun providePlansConfigDataStore(
        @ApplicationContext context: Context
    ): DataStore<PlansConfig> = context.internalPlansDataStore
}

/**
 * Extension property that defines the actual [DataStore] delegate
 * for persisting [PlansConfig] data inside `plans_config.json`.
 *
 * Declared as a private property on [Context] to prevent external access.
 *
 * The [PlansConfigSerializer] handles encoding and decoding operations
 * to/from JSON using [kotlinx.serialization].
 */
private val Context.internalPlansDataStore: DataStore<PlansConfig> by dataStore(
    fileName = "plans_config.json",
    serializer = PlansConfigSerializer
)

/**
 * Custom [Serializer] for [PlansConfig].
 *
 * This class controls how the app reads from and writes to the DataStore file.
 * It uses [kotlinx.serialization] with a `Json` configuration that:
 * - Ignores unknown keys to maintain forward compatibility.
 * - Includes default values in serialization.
 * - Avoids pretty-printing for efficiency.
 *
 * When deserialization fails (e.g., due to corruption or schema mismatch),
 * a safe default [PlansConfig] is returned to keep the app functional.
 */
object PlansConfigSerializer : Serializer<PlansConfig> {

    /** JSON adapter used for serialization/deserialization of [PlansConfig]. */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Default fallback configuration used when no valid data exists.
     *
     * This ensures the app has a minimal valid [PlansConfig] structure
     * even before any remote configuration is fetched.
     */
    override val defaultValue: PlansConfig = PlansConfig(
        version = 0,
        generatedAt = "",
        notes = listOf("Default fallback configuration (no remote data loaded)"),
        features = listOf(Feature.Calls), // minimal supported feature set
        plans = mapOf(
            PlanId.Free to Plan(setOf(Feature.Calls)),
            PlanId.Basic to Plan(setOf(Feature.Calls)),
            PlanId.Premium to Plan(setOf(Feature.Calls))
        ),
        roles = setOf(Role.Parent, Role.Child, Role.Member),
        access = mapOf(
            Role.Parent to emptyMap(),
            Role.Child to emptyMap(),
            Role.Member to emptyMap()
        )
    )

    /**
     * Reads a [PlansConfig] instance from the given [InputStream].
     *
     * If the file is empty or corrupted, returns [defaultValue].
     *
     * @param input The [InputStream] from the underlying DataStore file.
     * @return The deserialized [PlansConfig], or the fallback default.
     */
    override suspend fun readFrom(input: InputStream): PlansConfig =
        runCatching {
            json.decodeFromString(PlansConfig.serializer(), input.readBytes().decodeToString())
        }.getOrElse { defaultValue }

    /**
     * Writes the given [PlansConfig] instance to the given [OutputStream].
     *
     * @param t The [PlansConfig] instance to persist.
     * @param output The target [OutputStream] provided by DataStore.
     */
    override suspend fun writeTo(t: PlansConfig, output: OutputStream) {
        output.write(json.encodeToString(PlansConfig.serializer(), t).encodeToByteArray())
    }
}
