package com.otomin.app.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.otomin.app.model.PlanId
import com.otomin.app.model.Role

/**
 * Main content for the “Plans Config” screen.
 *
 * Renders:
 * - Acting/Target role dropdowns
 * - Plan dropdown
 * - A list of feature rows (R/N)
 *
 * This composable is **pure UI**: it takes the current [ConfigMviViewState] and a single
 * event dispatcher ([getFeatures]) that is invoked whenever the user changes selection.
 *
 * Typical usage:
 * ```
 * ConfigContentScreen(
 *   state = viewState,
 *   getFeatures = { selection -> vm.onSelectionChanged(selection) },
 *   modifier = Modifier.fillMaxSize()
 * )
 * ```
 *
 * @param state Current immutable screen state (selection, options, rows, loading/error).
 * @param getFeatures Callback fired when acting/target/plan changes. The new [Selection] is
 *        passed upward; the caller should resolve features and update [state.rows].
 * @param modifier Layout modifier for the surrounding container.
 */
@Composable
fun ConfigContentScreen(
    state: ConfigMviViewState,
    getFeatures: (Selection) -> Unit,
    modifier: Modifier,
) = LazyColumn(
    modifier = modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    // Role pickers
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActingRoleDropDown(state.selection.acting, state.actingRoleOptions) { role ->
                getFeatures(state.selection.copy(acting = role))
            }
            TargetRoleDropDown(state.selection.target, state.targetRoleOptions) { role ->
                getFeatures(state.selection.copy(target = role))
            }
        }
    }

    // Plan picker
    item {
        PlanDropdown(state.selection.plan, state.planOptions) { plan ->
            getFeatures(state.selection.copy(plan = plan))
        }
    }

    // Features header
    item {
        Text(
            "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    // Feature rows (R/N)
    items(state.rows.size) { i ->
        FeatureRowItem(state.rows[i])
    }

    // Bottom spacer so last item isn’t flush with the nav bar/edge
    item { Spacer(Modifier.height(48.dp)) }
}

/**
 * Dropdown for selecting the **acting** role.
 *
 * @param selectedText Currently selected role value.
 * @param options Available role options to show.
 * @param onPick Invoked with the chosen role.
 */
@Composable
fun ActingRoleDropDown(
    selectedText: Role,
    options: List<Role>,
    onPick: (Role) -> Unit
) = EnumDropdown(
    label = "Acting",
    selectedText = selectedText.name,
    options = options.map { it.name },
    onPick = { picked ->
        options.firstOrNull { it.name == picked }?.let(onPick)
    }
)

/**
 * Dropdown for selecting the **target** role (the profile being acted upon).
 *
 * @param selectedText Currently selected target role value.
 * @param options Available role options to show (commonly includes `Self`).
 * @param onPick Invoked with the chosen role.
 */
@Composable
fun TargetRoleDropDown(
    selectedText: Role,
    options: List<Role>,
    onPick: (Role) -> Unit
) = EnumDropdown(
    label = "Target",
    selectedText = selectedText.name,
    options = options.map { it.name },
    onPick = { picked ->
        options.firstOrNull { it.name == picked }?.let(onPick)
    }
)

/**
 * Dropdown for selecting a [PlanId].
 *
 * @param value Currently selected plan.
 * @param options Available plans to display.
 * @param onPick Invoked with the newly selected plan.
 */
@Composable
private fun PlanDropdown(
    value: PlanId,
    options: List<PlanId>,
    onPick: (PlanId) -> Unit
) = EnumDropdown(
    label = "Plan",
    selectedText = value.name,
    options = options.map { it.name },
    onPick = { picked ->
        options.firstOrNull { it.name == picked }?.let(onPick)
    }
)

/**
 * Design-time preview showing initial state.
 *
 * This preview uses a minimal, self-contained [ConfigMviViewState] with:
 * - Parent acting on Self
 * - Free plan selected
 * - Empty feature rows (loading true)
 */
@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PreviewConfigScreen() {
    MaterialTheme {
        ConfigContentScreen(
            state = ConfigMviViewState(
                selection = Selection(
                    acting = Role.Parent,
                    target = Role.Self,
                    plan = PlanId.Free,
                ),
                rows = emptyList(),
                actingRoleOptions = listOf(Role.Parent, Role.Child, Role.Member),
                targetRoleOptions = listOf(Role.Parent, Role.Child, Role.Member, Role.Self),
                planOptions = PlanId.entries.toList(),
                isLoading = true,
                error = "",
            ),
            getFeatures = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}