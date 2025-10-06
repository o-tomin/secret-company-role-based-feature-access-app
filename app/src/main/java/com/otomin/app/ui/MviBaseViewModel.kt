package com.otomin.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A generic base [ViewModel] that provides state management for
 * the **Model-View-Intent (MVI)** architecture pattern using Kotlin Flows.
 *
 * This class ensures that UI state updates are:
 * - **Reactive** — Exposed via [StateFlow] to the UI.
 * - **Immutable** — The state object is always replaced, never mutated in place.
 * - **Thread-safe** — Uses [Mutex] to guarantee atomic state transitions even
 *   when multiple coroutines update the state concurrently.
 *
 * ## Key Concepts
 * - **`S` (State type)**: Represents the immutable view state (e.g., `ConfigMviViewState`).
 * - **`setState {}`**: Updates state by applying a reducer function on the current value.
 * - **`state`**: A read-only [StateFlow] observed by UI (e.g., in Compose via
 *   `collectAsStateWithLifecycle()`).
 *
 * ## Example Usage
 * ```kotlin
 * @HiltViewModel
 * class ConfigViewModel @Inject constructor(
 *     private val getFeaturesUseCase: GetFeaturesUseCase
 * ) : MviBaseViewModel<ConfigMviViewState>(
 *     initialState = ConfigMviViewState()
 * ) {
 *     fun refresh() = viewModelScope.launch {
 *         setState { copy(isLoading = true) }
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * The [setState] function locks state mutations using a [Mutex], ensuring
 * sequential state updates — critical in concurrent coroutines where
 * ViewModel logic may trigger multiple asynchronous mutations.
 *
 * @param S The type of [MviBaseViewState] representing this ViewModel’s state.
 * @param initialState The initial state to seed the [MutableStateFlow].
 *
 * @see MviBaseViewState
 */
abstract class MviBaseViewModel<S : MviBaseViewState>(
    initialState: S
) : ViewModel() {

    /** Backing [MutableStateFlow] for managing internal state mutations. */
    private val _state = MutableStateFlow(initialState)

    /**
     * Public read-only observable for UI to react to state updates.
     *
     * Use this property to observe state from the UI layer (e.g., Compose).
     */
    val state: StateFlow<S>
        get() = _state

    /** Mutex ensuring thread-safe state transitions. */
    private val stateMutex = Mutex()

    /**
     * Updates the current state atomically by applying a reducer transformation.
     *
     * The reducer receives the current state instance and returns a new one.
     * Internally, this function uses a [Mutex] to prevent concurrent writes
     * to [_state].
     *
     * ### Example:
     * ```kotlin
     * setState {
     *     copy(isLoading = false, error = "Network failed")
     * }
     * ```
     *
     * @param reducer A lambda that transforms the current state into a new one.
     */
    protected suspend fun setState(reducer: S.() -> S) {
        _state.value = stateMutex.withLock {
            _state.value.reducer()
        }
    }
}

/**
 * Marker interface representing a ViewModel state in the MVI architecture.
 *
 * Implementations of this interface should be immutable data classes
 * that describe the entire UI state at any given time.
 *
 * Example:
 * ```kotlin
 * data class ConfigMviViewState(
 *     val isLoading: Boolean,
 *     val error: String
 * ) : MviBaseViewState
 * ```
 */
interface MviBaseViewState
