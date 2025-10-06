package com.otomin.app.ui.config

import android.util.Log
import com.otomin.app.Constants.TAG
import com.otomin.app.di.IoDispatcher
import com.otomin.app.model.AccessFlag
import com.otomin.app.model.Feature
import com.otomin.app.model.PlanId
import com.otomin.app.model.Role
import com.otomin.app.reopsitory.PlansConfigRepository
import com.otomin.app.ui.BaseUseCase
import com.otomin.app.ui.UseCaseCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use case responsible for resolving the list of available [Feature]s for a given
 * combination of acting role, target role, and plan.
 *
 * This encapsulates the business logic that determines which features are accessible
 * based on the current configuration matrix ([PlansConfigRepository]) — including
 * handling refresh logic and local caching.
 *
 * ## Responsibilities
 * - Retrieve the current configuration (from cache or network).
 * - Resolve the intersection of:
 *   1. Features available in the selected plan, and
 *   2. Feature access flags (R/N) defined for the acting→target role relationship.
 * - Produce a sorted list of [FeatureRow] items that the UI can render.
 *
 * ## Input contract
 * The input is a [Pair]:
 * - `first` = [Selection] — defines acting role, target role, and plan.
 * - `second` = `Boolean refresh` — determines whether to force a remote reload.
 *
 * If `refresh` is true **or** the repository currently holds only the default config,
 * a fresh config is fetched via `repository.fetchAndSet()`.
 * Otherwise, the cached config is read via `repository.get()`.
 *
 * ## Output
 * Emits a [Result] containing a list of [FeatureRow]:
 * - `Result.success(list)` on successful resolution.
 * - `Result.failure(e)` if any exception occurs (e.g. parsing or I/O errors).
 *
 * These results are exposed via [BaseUseCase.outputFlow], which is observed by
 * [ConfigViewModel] to update the UI state.
 *
 * ## Sorting
 * Features are always presented in a consistent order:
 * ```
 * Calls → ScreenTime → Location
 * ```
 * defined by the internal [featureOrder] list.
 *
 * @property coroutineScope Scoped coroutine context used by the base use case.
 * @property ioDispatcher Background thread dispatcher for performing repository operations.
 * @property repository Repository providing access to configuration data.
 *
 * @see PlansConfigRepository
 * @see ConfigViewModel
 * @see FeatureRow
 */
class GetFeaturesUseCase @Inject constructor(
    private val coroutineScope: UseCaseCoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val repository: PlansConfigRepository,
) : BaseUseCase<Pair<Selection, Boolean>, List<FeatureRow>>(
    useCaseScope = coroutineScope
) {

    /**
     * Defines the consistent order in which features appear in the UI.
     * This ensures predictable display regardless of YAML input order.
     */
    private val featureOrder = listOf(Feature.Calls, Feature.ScreenTime, Feature.Location)

    /**
     * Executes the use case with the given input.
     *
     * @param input A pair of:
     *  - [Selection]: acting role, target role, and plan.
     *  - `Boolean`: whether to force a refresh from network.
     *
     * Performs configuration resolution and emits a [Result] containing
     * a list of [FeatureRow] to the output flow.
     */
    override operator fun invoke(input: Pair<Selection, Boolean>) {
        Log.d(TAG, "GetFeaturesUseCase($input)")

        coroutineScope.launch(ioDispatcher) {
            val (selection, refresh) = input

            val result = try {
                // Choose configuration source: network or local
                val cfg = if (refresh || repository.isSetToDefaultConfig()) {
                    repository.fetchAndSet()
                } else {
                    repository.get()
                }

                // Extract plan features and access flags
                val planFeatures = cfg.plans[selection.plan]?.features.orEmpty()
                val flagsForPlan =
                    cfg.access[selection.acting]?.get(selection.target)?.get(selection.plan)
                        .orEmpty()

                // Build a sorted feature list with access indicators
                val list: List<FeatureRow> = planFeatures
                    .sortedBy { featureOrder.indexOf(it) }
                    .map { feature ->
                        FeatureRow(feature, flagsForPlan[feature] == AccessFlag.R)
                    }

                Result.success(list)
            } catch (e: Throwable) {
                Result.failure(e)
            }

            emitToOutput(result)
        }
    }
}

/**
 * Represents the user’s current context selection:
 * which **role** is acting, which **role** is targeted, and under which **plan**.
 *
 * Used as the input for [GetFeaturesUseCase] and stored in [ConfigMviViewState].
 *
 * Example:
 * ```kotlin
 * Selection(
 *   acting = Role.Parent,
 *   target = Role.Child,
 *   plan = PlanId.Premium
 * )
 * ```
 *
 * @property acting The role of the currently logged-in user.
 * @property target The role the acting user is inspecting (e.g., child, member, self).
 * @property plan The plan tier being evaluated (e.g., Free, Basic, Premium).
 */
data class Selection(
    val acting: Role,
    val target: Role,
    val plan: PlanId,
)

/**
 * UI-friendly representation of a feature’s accessibility result.
 *
 * Each [FeatureRow] corresponds to a single feature row in the configuration screen,
 * combining the feature’s name and whether it is **allowed (R)** or **not allowed (N)**.
 *
 * Example:
 * ```kotlin
 * FeatureRow(feature = Feature.Calls, allowed = true)
 * ```
 *
 * @property feature The [Feature] being described.
 * @property allowed Whether the feature is marked as accessible (`R`) for this context.
 *
 * @see Feature
 * @see GetFeaturesUseCase
 */
data class FeatureRow(
    val feature: Feature,
    val allowed: Boolean
)
