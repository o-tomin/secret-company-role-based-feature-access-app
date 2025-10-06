package com.otomin.app.ui.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.otomin.app.util.collectAsStateWithLifecycle

/**
 * Top-level composable entry point for the **Access Matrix Demo** screen.
 *
 * This composable:
 * - Injects the [ConfigViewModel] via Hilt.
 * - Triggers an initial configuration refresh on first composition.
 * - Observes [ConfigMviViewState] from the ViewModel using a lifecycle-aware state collector.
 * - Renders the correct UI state:
 *   - A loading indicator while fetching data.
 *   - An error message if something fails.
 *   - The main [ConfigContentScreen] when data is successfully loaded.
 *
 * ## Lifecycle behavior
 * Uses [LaunchedEffect(Unit)] to ensure [ConfigViewModel.refresh] is called
 * **once per entry** into this composable, preventing redundant network calls
 * across recompositions.
 *
 * ## UI structure
 * - **Scaffold** provides a [TopAppBar] with a manual “Refresh” button.
 * - **Body** conditionally renders:
 *   - A [CircularProgressIndicator] when `state.isLoading` is true.
 *   - A centered error message when `state.error` is not blank.
 *   - [ConfigContentScreen] when data is ready.
 *
 * ## Example usage
 * Called directly from your app’s main `Activity`:
 * ```kotlin
 * setContent {
 *     ConfigRoute()
 * }
 * ```
 *
 * @param viewModel The injected [ConfigViewModel] instance (default provided by [hiltViewModel]).
 *
 * @see ConfigViewModel
 * @see ConfigMviViewState
 * @see ConfigContentScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigRoute(
    viewModel: ConfigViewModel = hiltViewModel()
) {
    // Trigger initial data load once when the screen appears.
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Observe MVI-style state with lifecycle awareness.
    val state: ConfigMviViewState by viewModel.state.collectAsStateWithLifecycle()

    // Screen layout structure with top app bar and content surface.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access Matrix Demo") },
                actions = {
                    TextButton(onClick = viewModel::refresh) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { pad ->
        when {
            // --- Loading state ---
            state.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // --- Error state ---
            state.error.isNotBlank() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Error: ${state.error}")
                }
            }

            // --- Success/content state ---
            else -> {
                ConfigContentScreen(
                    state = state,
                    getFeatures = viewModel::getFeatures,
                    modifier = Modifier.padding(pad)
                )
            }
        }
    }
}
