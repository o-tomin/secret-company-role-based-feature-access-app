package com.verizon.app.di

import com.verizon.app.ui.DefaultUseCaseCoroutineScope
import com.verizon.app.ui.UseCaseCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dagger-Hilt module responsible for providing configuration-related dependencies
 * used within ViewModel-scoped components.
 *
 * This module primarily provides a [UseCaseCoroutineScope], which encapsulates structured
 * coroutine dispatchers and lifecycle management for executing domain-level use cases.
 *
 * ## Why it exists
 * Each ViewModel often requires a consistent coroutine environment
 * (for launching background work, switching contexts, or cancelling jobs when the
 * ViewModel is cleared). This module ensures that all use cases within a ViewModel
 * share the same managed coroutine scope.
 *
 * ## Install scope
 * Annotated with [@InstallIn(ViewModelComponent::class)], meaning:
 * - All provided dependencies live as long as the **ViewModel** that requests them.
 * - Each ViewModel gets its own [UseCaseCoroutineScope] instance.
 *
 * ## Provided dependencies
 * - [UseCaseCoroutineScope] via [DefaultUseCaseCoroutineScope]
 *   which wraps and manages the three dispatcher types:
 *   - **@IoDispatcher** → for network and I/O work
 *   - **@MainDispatcher** → for UI updates or Compose state emissions
 *   - **@DefaultDispatcher** → for CPU-bound work
 *
 * Example usage:
 * ```kotlin
 * @HiltViewModel
 * class ConfigViewModel @Inject constructor(
 *     private val getFeaturesUseCase: GetFeaturesUseCase,
 *     private val coroutineScope: UseCaseCoroutineScope
 * ) : ViewModel() {
 *     // use coroutineScope.launch(...) for structured background operations
 * }
 * ```
 *
 * @see UseCaseCoroutineScope
 * @see DefaultUseCaseCoroutineScope
 */
@Module
@InstallIn(ViewModelComponent::class)
object ConfigModule {

    /**
     * Provides a [UseCaseCoroutineScope] instance configured with
     * all three standard dispatchers (IO, Main, Default).
     *
     * @param ioDispatcher The dispatcher optimized for I/O-bound work such as network or disk access.
     * @param mainDispatcher The dispatcher bound to the Android main thread, used for UI operations.
     * @param defaultDispatcher The dispatcher optimized for CPU-bound work.
     *
     * @return A fully configured [UseCaseCoroutineScope] implementation,
     *         lifecycle-bound to the requesting ViewModel.
     */
    @ViewModelScoped
    @Provides
    fun provideUseCaseCoroutineScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): UseCaseCoroutineScope =
        DefaultUseCaseCoroutineScope(
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher,
            defaultDispatcher = defaultDispatcher
        )
}
