package com.otomin.app.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Defines a lifecycle-aware coroutine scope abstraction for running
 * **UseCases** (business logic units) within the application.
 *
 * The [UseCaseCoroutineScope] interface allows for structured coroutine
 * management that can be shared across multiple use cases — enabling cancellation
 * and ensuring all launched coroutines are bound to a controlled job hierarchy.
 *
 * ### Why a Custom Scope?
 * Unlike `viewModelScope`, use cases often need their own structured concurrency context
 * to manage background operations independently of UI lifecycles. This abstraction
 * allows use cases to:
 * - Use Hilt-injected dispatchers (IO, Main, Default).
 * - Cancel all background work cleanly when no longer needed.
 *
 * ### Example
 * ```kotlin
 * class FetchDataUseCase @Inject constructor(
 *     private val coroutineScope: UseCaseCoroutineScope,
 *     @IoDispatcher private val ioDispatcher: CoroutineDispatcher
 * ) {
 *     operator fun invoke() {
 *         coroutineScope.launch(ioDispatcher) {
 *             // Perform background work safely within the use case
 *         }
 *     }
 * }
 * ```
 */
interface UseCaseCoroutineScope : CoroutineScope {
    /**
     * Cancels all coroutines launched in this scope.
     *
     * This function is typically called during cleanup — e.g., when the
     * associated ViewModel or use case is being cleared or disposed.
     */
    fun cancelJobs()
}

/**
 * Default implementation of [UseCaseCoroutineScope] that composes a coroutine
 * context from multiple injected dispatchers and a [SupervisorJob].
 *
 * - Uses a **SupervisorJob** to ensure that child coroutine failures
 *   do not automatically cancel sibling jobs.
 * - Aggregates multiple dispatchers (IO, Main, Default) for flexibility.
 *
 * @property ioDispatcher The dispatcher for IO-bound work (network, disk, etc.).
 * @property mainDispatcher The dispatcher for main-thread UI-related tasks.
 * @property defaultDispatcher The dispatcher for CPU-intensive tasks.
 *
 * ### Example (Hilt Module)
 * ```kotlin
 * @Module
 * @InstallIn(ViewModelComponent::class)
 * object ConfigModule {
 *
 *     @Provides
 *     @ViewModelScoped
 *     fun provideUseCaseCoroutineScope(
 *         @IoDispatcher io: CoroutineDispatcher,
 *         @MainDispatcher main: CoroutineDispatcher,
 *         @DefaultDispatcher default: CoroutineDispatcher
 *     ): UseCaseCoroutineScope =
 *         DefaultUseCaseCoroutineScope(io, main, default)
 * }
 * ```
 */
class DefaultUseCaseCoroutineScope(
    ioDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    defaultDispatcher: CoroutineDispatcher,
) : UseCaseCoroutineScope {

    /**
     * The composed coroutine context combining:
     * - A [SupervisorJob] for structured concurrency
     * - The three provided dispatchers
     */
    override val coroutineContext =
        SupervisorJob() + ioDispatcher + mainDispatcher + defaultDispatcher

    /**
     * Cancels all active and pending coroutines launched in this scope.
     *
     * This cleanup method should be called when a use case’s lifetime ends,
     * for example when the parent ViewModel is cleared.
     */
    override fun cancelJobs() {
        coroutineContext.cancel()
    }
}
