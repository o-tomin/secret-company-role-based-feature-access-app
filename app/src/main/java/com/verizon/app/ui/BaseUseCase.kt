package com.otomin.app.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A generic **base class for all use cases** in the application’s domain layer.
 *
 * It standardizes how business logic is executed, observed, and cleaned up,
 * following a reactive and coroutine-safe architecture.
 *
 * ## Purpose
 * - Provides a **consistent abstraction** for executing business logic (use cases).
 * - Exposes results via a cold [Flow] of [Result] objects for reactive observation.
 * - Encapsulates coroutine lifecycle management through [UseCaseCoroutineScope].
 *
 * ## Type parameters
 * - **I** → Input type: the data or parameters required to execute the use case.
 * - **O** → Output type: the data produced when the use case completes successfully.
 *
 * ## Flow behavior
 * - Each use case emits results into [_outputFlow] as [Result] objects.
 * - Consumers (e.g., ViewModels) collect from [outputFlow] to receive updates.
 * - The shared flow uses an `extraBufferCapacity` of 100, allowing non-blocking emissions.
 *
 * ## Typical subclass pattern
 * Each use case implements [invoke] and performs its logic on an appropriate dispatcher:
 * ```kotlin
 * class MyUseCase @Inject constructor(
 *     private val scope: UseCaseCoroutineScope,
 *     @IoDispatcher private val io: CoroutineDispatcher
 * ) : BaseUseCase<InputType, OutputType>(scope) {
 *
 *     override fun invoke(input: InputType) {
 *         scope.launch(io) {
 *             val result = runCatching {
 *                 // perform business logic here
 *             }
 *             emitToOutput(result)
 *         }
 *     }
 * }
 * ```
 *
 * ## Lifecycle management
 * - The [cleanup] method allows the caller (e.g., ViewModel) to cancel ongoing jobs.
 * - This is typically called from `ViewModel.onCleared()` or similar lifecycle events.
 *
 * @property useCaseScope A coroutine scope interface providing job control for the use case.
 * @see UseCaseCoroutineScope
 */
abstract class BaseUseCase<I, O>(
    private val useCaseScope: UseCaseCoroutineScope
) {
    /**
     * Internal shared flow for emitting results to observers.
     *
     * Configured with a buffer capacity to tolerate rapid consecutive emissions
     * without suspending the producer coroutine.
     */
    private val _outputFlow = MutableSharedFlow<Result<O>>(extraBufferCapacity = 100)

    /**
     * Public immutable flow for collecting results of the use case execution.
     *
     * Each emission represents either:
     * - [Result.success] containing a computed value, or
     * - [Result.failure] containing an exception.
     *
     * Collected typically by ViewModels to reactively update UI state.
     */
    val outputFlow: Flow<Result<O>>
        get() = _outputFlow

    /**
     * Executes the use case with the given [input].
     *
     * Must be implemented by concrete subclasses. Implementations should:
     * - Launch work on an appropriate coroutine dispatcher (e.g., IO or Default).
     * - Wrap results in [Result] to capture success/failure cleanly.
     * - Call [emitToOutput] to propagate results downstream.
     *
     * @param input The input data required to perform the use case.
     */
    abstract fun invoke(input: I)

    /**
     * Emits a computed [result] into the [outputFlow].
     *
     * Typically called at the end of a coroutine launched inside [invoke].
     *
     * @param result A [Result] wrapping the output or an exception.
     */
    suspend fun emitToOutput(result: Result<O>) {
        _outputFlow.emit(result)
    }

    /**
     * Cancels all active jobs in this use case’s coroutine scope.
     *
     * Should be invoked when the use case is no longer needed
     * (e.g., when a ViewModel is cleared or a screen is destroyed).
     */
    fun cleanup() {
        useCaseScope.cancelJobs()
    }
}