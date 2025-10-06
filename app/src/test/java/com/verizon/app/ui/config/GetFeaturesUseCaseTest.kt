package com.verizon.app.ui.config

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.verizon.app.model.AccessFlag
import com.verizon.app.model.Feature
import com.verizon.app.model.Plan
import com.verizon.app.model.PlanId
import com.verizon.app.model.PlansConfig
import com.verizon.app.model.Role
import com.verizon.app.reopsitory.PlansConfigRepository
import com.verizon.app.ui.BaseUseCaseTest
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetFeaturesUseCaseTest : BaseUseCaseTest() {

    @MockK
    lateinit var repository: PlansConfigRepository

    @Test
    fun `GetFeaturesUseCase Successful refresh = true`() = runTest {
        val getFeaturesUseCase = GetFeaturesUseCase(
            coroutineScope = getUseCaseCoroutineScope(this),
            ioDispatcher = testDispatcher,
            repository = repository,
        )

        coEvery { repository.isSetToDefaultConfig() } returns false
        coEvery { repository.fetchAndSet() } returns DEFAULT_CONFIG

        val selection = Selection(
            acting = Role.Parent,
            target = Role.Self,
            plan = PlanId.Free,
        )

        val refresh = true

        // new coroutine as invocation and collection done on same test dispatcher
        launch { getFeaturesUseCase(input = selection to refresh) }

        getFeaturesUseCase.outputFlow.test {
            awaitItem().onSuccess {
                assertThat(it).isEqualTo(
                    listOf(
                        FeatureRow(
                            feature = Feature.Calls,
                            allowed = true
                        )
                    )
                )
            }.onFailure {
                Assert.fail(it.stackTraceToString())
            }

            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `GetFeaturesUseCase Successful refresh = false isSetToDefaultConfig = true`() = runTest {
        val getFeaturesUseCase = GetFeaturesUseCase(
            coroutineScope = getUseCaseCoroutineScope(this),
            ioDispatcher = testDispatcher,
            repository = repository,
        )

        coEvery { repository.isSetToDefaultConfig() } returns true
        coEvery { repository.fetchAndSet() } returns DEFAULT_CONFIG

        val selection = Selection(
            acting = Role.Parent,
            target = Role.Self,
            plan = PlanId.Free,
        )

        val refresh = false

        // new coroutine as invocation and collection done on same test dispatcher
        launch { getFeaturesUseCase(input = selection to refresh) }

        getFeaturesUseCase.outputFlow.test {
            awaitItem().onSuccess {
                assertThat(it).isEqualTo(
                    listOf(
                        FeatureRow(
                            feature = Feature.Calls,
                            allowed = true
                        )
                    )
                )
            }.onFailure {
                Assert.fail(it.stackTraceToString())
            }

            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `GetFeaturesUseCase Successful refresh = false isSetToDefaultConfig = false`() = runTest {
        val getFeaturesUseCase = GetFeaturesUseCase(
            coroutineScope = getUseCaseCoroutineScope(this),
            ioDispatcher = testDispatcher,
            repository = repository,
        )

        coEvery { repository.isSetToDefaultConfig() } returns false
        coEvery { repository.get() } returns DEFAULT_CONFIG

        val selection = Selection(
            acting = Role.Parent,
            target = Role.Self,
            plan = PlanId.Free,
        )

        val refresh = false

        // new coroutine as invocation and collection done on same test dispatcher
        launch { getFeaturesUseCase(input = selection to refresh) }

        getFeaturesUseCase.outputFlow.test {
            awaitItem().onSuccess {
                assertThat(it).isEqualTo(
                    listOf(
                        FeatureRow(
                            feature = Feature.Calls,
                            allowed = true
                        )
                    )
                )
            }.onFailure {
                Assert.fail(it.stackTraceToString())
            }

            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `GetFeaturesUseCase Successful throws exception`() = runTest {
        val getFeaturesUseCase = GetFeaturesUseCase(
            coroutineScope = getUseCaseCoroutineScope(this),
            ioDispatcher = testDispatcher,
            repository = repository,
        )

        coEvery { repository.isSetToDefaultConfig() } returns false
        val error = Throwable("too bad...")
        coEvery { repository.get() } throws error

        val selection = Selection(
            acting = Role.Parent,
            target = Role.Self,
            plan = PlanId.Free,
        )

        val refresh = false

        // new coroutine as invocation and collection done on same test dispatcher
        launch { getFeaturesUseCase(input = selection to refresh) }

        getFeaturesUseCase.outputFlow.test {
            awaitItem().onSuccess {
                Assert.fail("You had to handle this!")
            }.onFailure {
                assertThat(it).isEqualTo(error)
            }

            expectNoEvents()
            cancel()
        }
    }

    companion object {
        val DEFAULT_CONFIG: PlansConfig = PlansConfig(
            version = 1,
            generatedAt = "2025-10-04",
            notes = listOf(
                "R = allowed/accessible; N = not allowed.",
                "Matrix is explicit per acting_role → target_role → plan → feature.",
                "Features available under each plan are listed in `plans`.",
                "If a plan does not include a feature, it should remain N (even if an R is mistakenly set)."
            ),
            features = listOf(
                Feature.Calls,
                Feature.ScreenTime,
                Feature.Location
            ),
            plans = mapOf(
                PlanId.Free to Plan(setOf(Feature.Calls)),
                PlanId.Basic to Plan(setOf(Feature.Calls, Feature.ScreenTime)),
                PlanId.Premium to Plan(setOf(Feature.Calls, Feature.ScreenTime, Feature.Location))
            ),
            roles = setOf(Role.Parent, Role.Child, Role.Member, Role.Self),
            access = mapOf(
                Role.Parent to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to accessRow(r = "RNN"),
                        PlanId.Basic to accessRow(r = "RRN"),
                        PlanId.Premium to accessRow(r = "RRR")
                    ),
                    Role.Parent to allN(),
                    Role.Child to mapOf(
                        PlanId.Free to accessRow("RNN"),
                        PlanId.Basic to accessRow("RRN"),
                        PlanId.Premium to accessRow("RRR")
                    ),
                    Role.Member to allN()
                ),
                Role.Child to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to accessRow("RNN"),
                        PlanId.Basic to accessRow("RRN"),
                        PlanId.Premium to accessRow("RRR")
                    ),
                    Role.Parent to allN(),
                    Role.Child to allN(),
                    Role.Member to allN()
                ),
                Role.Member to mapOf(
                    Role.Self to mapOf(
                        PlanId.Free to accessRow("RNN"),
                        PlanId.Basic to accessRow("RNN"),
                        PlanId.Premium to accessRow("RNN")
                    ),
                    Role.Parent to allN(),
                    Role.Child to allN(),
                    Role.Member to allN()
                )
            )
        )

        private fun accessRow(r: String): Map<Feature, AccessFlag> {
            val (calls, screen, loc) = r.map { if (it == 'R') AccessFlag.R else AccessFlag.N }
            return mapOf(
                Feature.Calls to calls,
                Feature.ScreenTime to screen,
                Feature.Location to loc
            )
        }

        private fun allN(): Map<PlanId, Map<Feature, AccessFlag>> = mapOf(
            PlanId.Free to accessRow("NNN"),
            PlanId.Basic to accessRow("NNN"),
            PlanId.Premium to accessRow("NNN")
        )
    }
}
