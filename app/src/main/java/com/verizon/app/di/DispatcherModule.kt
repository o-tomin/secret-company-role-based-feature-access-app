package com.verizon.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provides qualified [CoroutineDispatcher] instances for use across the application.
 *
 * ## Purpose
 * This module standardizes coroutine execution contexts by exposing
 * three distinct dispatchers — IO, Default, and Main — each associated
 * with a dedicated qualifier annotation.
 *
 * Injecting specific dispatchers improves testability and separation
 * of concerns, ensuring that business logic, UI operations, and background work
 * each execute in the most appropriate thread pool.
 *
 * ## Installation scope
 * Installed in [SingletonComponent], meaning:
 * - Each dispatcher is provided as a **singleton** across the entire application.
 * - These instances are created once and shared throughout the app’s lifecycle.
 *
 * ## Example usage
 * ```kotlin
 * class ExampleRepository @Inject constructor(
 *     @IoDispatcher private val ioDispatcher: CoroutineDispatcher
 * ) {
 *     suspend fun loadData() = withContext(ioDispatcher) {
 *         // Perform network or disk operations here
 *     }
 * }
 * ```
 *
 * @see IoDispatcher
 * @see DefaultDispatcher
 * @see MainDispatcher
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * Provides a [Dispatchers.IO] dispatcher for I/O-bound operations.
     *
     * Use this for network requests, database reads/writes,
     * or any other blocking I/O tasks.
     *
     * @return The [CoroutineDispatcher] optimized for offloading blocking I/O work.
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides a [Dispatchers.Default] dispatcher for CPU-bound operations.
     *
     * Use this for computationally intensive work such as JSON parsing,
     * data transformations, or algorithmic logic that does not involve blocking I/O.
     *
     * @return The [CoroutineDispatcher] optimized for parallel CPU-intensive work.
     */
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provides a [Dispatchers.Main] dispatcher for main-thread operations.
     *
     * Use this for UI updates, ViewModel state updates, and Compose recompositions.
     * This dispatcher runs on the Android main thread.
     *
     * @return The [CoroutineDispatcher] bound to the Android main thread.
     */
    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

/**
 * Qualifier annotation for distinguishing the IO dispatcher.
 *
 * Used when injecting a [CoroutineDispatcher] meant for network calls
 * or disk access.
 *
 * Example:
 * ```kotlin
 * @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier annotation for distinguishing the Default dispatcher.
 *
 * Used when injecting a [CoroutineDispatcher] meant for CPU-intensive operations
 * that do not block the main thread.
 *
 * Example:
 * ```kotlin
 * @Inject @DefaultDispatcher lateinit var defaultDispatcher: CoroutineDispatcher
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Qualifier annotation for distinguishing the Main dispatcher.
 *
 * Used when injecting a [CoroutineDispatcher] meant for updating UI or handling
 * operations that must occur on the main thread.
 *
 * Example:
 * ```kotlin
 * @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
