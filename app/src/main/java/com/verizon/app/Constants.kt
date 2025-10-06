package com.verizon.app

/**
 * A centralized object for application-wide constants.
 *
 * This object holds values that are used across multiple layers
 * of the Verizon Role-Based Access demo application.
 *
 * Keeping constants in a single place improves maintainability,
 * avoids duplication, and ensures consistent usage throughout
 * the app’s codebase.
 *
 * ### Example
 * ```kotlin
 * Log.d(Constants.TAG, "Configuration loaded successfully")
 * ```
 */
object Constants {

    /**
     * Global log tag used across the application for Android `Log` messages.
     *
     * This tag helps easily filter and identify app-specific log output
     * in Android Studio’s **Logcat**.
     *
     * Example usage:
     * ```kotlin
     * Log.d(Constants.TAG, "Feature access matrix updated")
     * ```
     */
    const val TAG = "Verizon"
}
