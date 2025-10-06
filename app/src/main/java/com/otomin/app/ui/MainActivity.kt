package com.otomin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.otomin.app.ui.config.ConfigRoute
import dagger.hilt.android.AndroidEntryPoint

/**
 * The main entry point for the otomin Role-Based Access demo application.
 *
 * This activity serves as the root host for all Jetpack Compose UI content
 * and initializes dependency injection via Hilt.
 *
 * ## Responsibilities
 * - Initializes the Android Activity lifecycle.
 * - Enables **edge-to-edge** layout behavior using [enableEdgeToEdge],
 *   allowing system bars (status and navigation) to blend with the app UI.
 * - Sets the main Compose content via [setContent], rendering [ConfigRoute]
 *   — the root composable that manages the access matrix demo screen.
 *
 * ## Architecture Overview
 * ```
 * MainActivity
 *   └── ConfigRoute (Composable)
 *         ├── ConfigViewModel (Hilt-injected)
 *         ├── ConfigContentScreen
 *         └── FeatureRowItem / EnumDropdown / etc.
 * ```
 *
 * ## Hilt Integration
 * Annotated with [@AndroidEntryPoint], which enables dependency injection
 * for all Hilt-managed components (e.g., [ConfigViewModel]) used within
 * this activity’s composable hierarchy.
 *
 * ## Example behavior
 * On launch, the activity:
 * 1. Enables immersive edge-to-edge layout.
 * 2. Loads [ConfigRoute], which:
 *    - Fetches configuration data from network or cache.
 *    - Displays dropdowns and feature matrix dynamically.
 *
 * @see ConfigRoute
 * @see ConfigViewModel
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is first created.
     *
     * Sets up edge-to-edge layout and inflates the Compose UI hierarchy.
     *
     * @param savedInstanceState Optional saved state bundle for activity recreation.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConfigRoute()
        }
    }
}
