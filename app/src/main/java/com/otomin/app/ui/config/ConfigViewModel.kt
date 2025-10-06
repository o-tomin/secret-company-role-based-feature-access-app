package com.otomin.app.ui.config

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.otomin.app.Constants.TAG
import com.otomin.app.model.PlanId
import com.otomin.app.model.Role
import com.otomin.app.ui.MviBaseViewModel
import com.otomin.app.ui.MviBaseViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for orchestrating the **Access Matrix Config screen**.
 *
 * This ViewModel follows the **MVI (Model–View–Intent)** pattern and acts as the state holder
 * for [ConfigRoute] and [ConfigContentScreen]. It manages the user’s current selection of
 * roles and plan, triggers feature resolution via [GetFeaturesUseCase], and emits
 * a single immutable [ConfigMviViewState] stream to the UI.
 *
 * ## Responsibilities
 * - Initialize with a default selection (Parent → Self → Free).
 * - Observe results from [GetFeaturesUseCase.outputFlow].
 * - Expose functions to **refresh** data or **update feature rows** on user interaction.
 * - Handle error propagation and loading state toggling.
 *
 * ## Data flow
 * ```
 * UI (ConfigRoute / ConfigContentScreen)
 *     ↓
 *   ViewModel → GetFeaturesUseCase(selection, refresh)
 *     ↓
 *   Repository → Network or DataStore
 *     ↓
 *   Emits List<FeatureRow> → ViewModel
 *     ↓
 *   Updates ConfigMviViewState → Compose recomposes UI
 * ```
 *
 * ## Error handling
 * - Exceptions from the use case are caught via `.onFailure` and written to `state.error`.
 * - Logs full stack traces to Logcat using [Log.e].
 * - Always resets `isLoading = false` after each use case result emission.
 *
 * @property getFeaturesUseCase Business logic layer use case that resolves allowed features
 * based on the current [Selection] (acting, target, plan).
 */
@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val getFeaturesUseCase: GetFeaturesUseCase,
) : MviBaseViewModel<ConfigMviViewState>(
    initialState = ConfigMviViewState(
        selection = Selection(
            acting = Role.Parent,
            target = Role.Self,
            plan = PlanId.Free,
        ),
        rows = emptyList(),
        actingRoleOptions = listOf(Role.Parent, Role.Child, Role.Member),
        targetRoleOptions = listOf(Role.Parent, Role.Child, Role.Member, Role.Self),
        planOptions = listOf(PlanId.Free, PlanId.Basic, PlanId.Premium),
        isLoading = false,
        error = "",
    )
) {

    init {
        // Observe results emitted by GetFeaturesUseCase
        getFeaturesUseCase.outputFlow
            .onEach { result ->
                result.onSuccess { rows ->
                    setState { copy(rows = rows) }
                }.onFailure { e ->
                    Log.e(TAG, e.stackTraceToString())
                    setState { copy(error = e.message.orEmpty()) }
                }
                setState { copy(isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Refreshes the entire configuration by forcing a remote fetch.
     *
     * Invoked when the user taps the **Refresh** button or when the screen first appears.
     *
     * Sets [ConfigMviViewState.isLoading] to `true`, triggers the use case with `refresh = true`,
     * and lets the flow observer update the state when complete.
     */
    fun refresh() = viewModelScope.launch {
        setState { copy(isLoading = true) }
        getFeaturesUseCase(state.value.selection to true)
    }

    /**
     * Retrieves features for a newly selected combination of roles and plan.
     *
     * Called whenever the user changes dropdown selections on the screen.
     *
     * Updates both [ConfigMviViewState.selection] and [ConfigMviViewState.isLoading],
     * then delegates resolution to [GetFeaturesUseCase] with `refresh = false`
     * (cached or last-known config is reused).
     *
     * @param selection The new combination of acting role, target role, and plan to evaluate.
     */
    fun getFeatures(selection: Selection) = viewModelScope.launch {
        setState { copy(isLoading = true, selection = selection) }
        getFeaturesUseCase(selection to false)
    }
}

/**
 * Immutable UI state model for the **Access Matrix Config screen**.
 *
 * Represents the current configuration selection, available options, and derived feature list.
 * Designed for MVI architecture — it is a single source of truth for all displayed values.
 *
 * ## Fields
 * | Property | Type | Description |
 * |-----------|------|-------------|
 * | `selection` | [Selection] | Current acting, target, and plan combination. |
 * | `rows` | List<[FeatureRow]> | Features resolved for the current selection. |
 * | `actingRoleOptions` | List<[Role]> | Available roles for the acting user dropdown. |
 * | `targetRoleOptions` | List<[Role]> | Available target roles (includes `Self`). |
 * | `planOptions` | List<[PlanId]> | Available plan choices. |
 * | `isLoading` | Boolean | Indicates if a data fetch is in progress. |
 * | `error` | String | Non-empty message indicates an error to display. |
 *
 * ## Typical lifecycle
 * - Initial state set by [ConfigViewModel].
 * - Updated via `setState` calls on user interaction or data result.
 * - Collected by the UI through a lifecycle-aware Flow.
 *
 * @see ConfigViewModel
 * @see FeatureRow
 */
data class ConfigMviViewState(
    val selection: Selection,
    val rows: List<FeatureRow>,
    val actingRoleOptions: List<Role>,
    val targetRoleOptions: List<Role>,
    val planOptions: List<PlanId>,
    val isLoading: Boolean,
    val error: String,
) : MviBaseViewState
