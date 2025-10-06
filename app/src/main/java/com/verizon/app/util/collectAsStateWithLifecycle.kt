package com.verizon.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A convenience extension function for collecting values from a [StateFlow]
 * in Jetpack Compose while respecting the [Lifecycle] of the current composition.
 *
 * This wrapper around `androidx.lifecycle.compose.collectAsStateWithLifecycle()`
 * automatically sets the initial value to `StateFlow.value` and uses the current
 * [LifecycleOwner] from [LocalLifecycleOwner].
 *
 * ### Why use this extension?
 * - Simplifies collecting a [StateFlow] in a Composable.
 * - Ensures collection stops when the UI is not active (to prevent leaks).
 * - Provides sensible defaults for lifecycle state and coroutine context.
 *
 * ### Example usage:
 * ```kotlin
 * @Composable
 * fun ConfigRoute(viewModel: ConfigViewModel = hiltViewModel()) {
 *     // Automatically collects ViewModel.state with lifecycle awareness
 *     val state by viewModel.state.collectAsStateWithLifecycle()
 *
 *     ConfigContentScreen(
 *         state = state,
 *         getFeatures = viewModel::getFeatures,
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * ### Behavior
 * - Starts collecting when the [Lifecycle] reaches at least [minActiveState].
 * - Stops when the lifecycle falls below [minActiveState].
 * - Emits the latest value stored in [StateFlow.value] as the initial UI state.
 *
 * @param lifecycleOwner The owner of the [Lifecycle] used to control collection.
 *   Defaults to [LocalLifecycleOwner.current] (the current Compose scope).
 * @param minActiveState The minimum lifecycle state required to start collecting.
 *   Defaults to [Lifecycle.State.STARTED].
 * @param context The [CoroutineContext] to run the collection in.
 *   Defaults to [EmptyCoroutineContext].
 *
 * @return A Compose [State] that reflects the latest value emitted by this [StateFlow],
 *   automatically updating the UI whenever the data changes while active.
 *
 * @see androidx.lifecycle.compose.collectAsStateWithLifecycle
 * @see StateFlow
 * @see Lifecycle
 */
@Composable
@Suppress("StateFlowValueCalledInComposition")
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> =
    collectAsStateWithLifecycle(
        initialValue = this.value,
        lifecycle = lifecycleOwner.lifecycle,
        minActiveState = minActiveState,
        context = context
    )
