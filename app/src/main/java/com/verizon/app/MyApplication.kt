package com.otomin.app

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

/**
 * The main [android.app.Application] class for the otomin Role-Based Access app.
 *
 * This class serves as the **entry point** for the application and initializes
 * **Hilt dependency injection** at process startup. It also extends
 * [MultiDexApplication] to support apps that exceed the 65K method reference limit.
 *
 * ### Key Responsibilities
 * - Bootstraps the **Hilt dependency injection graph** using the `@HiltAndroidApp` annotation.
 * - Serves as the **root container** for all Hilt components (`SingletonComponent`).
 * - Enables **MultiDex support**, ensuring compatibility with large codebases.
 *
 * ### Lifecycle
 * The Android framework instantiates this class before any activity, service,
 * or broadcast receiver is created — making it an ideal place for initializing
 * global dependencies and configuration.
 *
 * ### Example
 * ```xml
 * <application
 *     android:name=".MyApplication"
 *     android:icon="@mipmap/ic_launcher"
 *     android:label="@string/app_name"
 *     android:theme="@style/Theme.otomin" />
 * ```
 *
 * @see dagger.hilt.android.HiltAndroidApp
 * @see androidx.multidex.MultiDexApplication
 */
@HiltAndroidApp
class MyApplication : MultiDexApplication()
