package com.verizon.app.ui

import android.util.Log
import com.verizon.app.Constants.TAG
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.coroutines.CoroutineContext

/**
 * A reusable base class for unit tests that involve coroutine-based use cases.
 *
 * This class standardizes the setup and teardown of coroutine dispatchers,
 * lifecycle-aware scopes, and static log mocking to ensure predictable
 * and isolated test execution.
 *
 * ### Purpose
 * Many use cases in the Verizon Role-Based Access app rely on structured concurrency
 * and injected coroutine dispatchers (e.g., `@IoDispatcher`). This base class
 * ensures that those coroutines execute deterministically in a test environment
 * using a [TestDispatcher].
 *
 * ### Responsibilities
 * - Replace `Dispatchers.Main` with a test dispatcher for consistent scheduling.
 * - Initialize `MockK` annotations for dependency mocking.
 * - Mock Android’s [Log] static calls to avoid runtime crashes in JVM tests.
 * - Provide a helper for creating a test [UseCaseCoroutineScope].
 *
 * ### Example Usage
 * ```kotlin
 * @OptIn(ExperimentalCoroutinesApi::class)
 * class GetFeaturesUseCaseTest : BaseUseCaseTest() {
 *
 *     @Test
 *     fun `when refresh true - repository fetches remote config`() = runTest(testDispatcher) {
 *         val repo = mockk<PlansConfigRepository>()
 *         val scope = getUseCaseCoroutineScope(this)
 *         val useCase = GetFeaturesUseCase(scope, testDispatcher, repo)
 *
 *         // Test behavior...
 *     }
 * }
 * ```
 *
 * @see UseCaseCoroutineScope
 * @see kotlinx.coroutines.test.UnconfinedTestDispatcher
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUseCaseTest {

    /** Test dispatcher replacing `Dispatchers.Main` for coroutine tests. */
    protected lateinit var testDispatcher: TestDispatcher

    /**
     * Sets up the coroutine environment and initializes mocks before each test.
     *
     * Responsibilities:
     * - Assigns [UnconfinedTestDispatcher] as `Dispatchers.Main`.
     * - Initializes MockK annotations.
     * - Mocks Android’s static [Log] methods to prevent `RuntimeException`s.
     */
    @Before
    open fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)

        // Mock Android Log calls — required for JVM-only test environments
        mockkStatic(Log::class)
        every { Log.d(TAG, any()) } returns 0
        every { Log.e(TAG, any()) } returns 0
    }

    /**
     * Restores coroutine dispatchers and clears all mocks after each test.
     *
     * - Resets `Dispatchers.Main` to its original state.
     * - Clears all MockK configurations.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    companion object {

        /**
         * Creates a [UseCaseCoroutineScope] that uses the provided [TestScope]
         * for launching coroutines within use case tests.
         *
         * This helper allows use cases to run with a controlled coroutine context
         * that can be explicitly advanced, paused, or cancelled.
         *
         * @param testScope The test scope to bind the coroutine context to.
         * @return A test-specific implementation of [UseCaseCoroutineScope].
         */
        fun getUseCaseCoroutineScope(testScope: TestScope): UseCaseCoroutineScope {
            return object : UseCaseCoroutineScope {
                override fun cancelJobs() {
                    testScope.cancel()
                }

                override val coroutineContext: CoroutineContext
                    get() = testScope.coroutineContext
            }
        }
    }
}
