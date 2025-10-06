package com.otomin.app.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Displays a single feature and its accessibility state (allowed or not allowed)
 * within a card-style row on the configuration screen.
 *
 * Each row corresponds to one [FeatureRow] emitted by [GetFeaturesUseCase],
 * showing:
 * - The **feature name** (e.g. “Calls”, “ScreenTime”, “Location”)
 * - A trailing **chip** labeled “R” (allowed) or “N” (not allowed)
 *
 * The chip color and text dynamically reflect the [FeatureRow.allowed] flag:
 * - `allowed = true` → colored using `MaterialTheme.colorScheme.primary` and label “R”
 * - `allowed = false` → outlined using `MaterialTheme.colorScheme.outline` and label “N”
 *
 * ## Visual structure
 * ```
 * ┌──────────────────────────────────────────────┐
 * │ Calls                             [ R ]      │
 * └──────────────────────────────────────────────┘
 * ```
 *
 * ## Parameters
 * @param row The [FeatureRow] model representing the feature and its access flag.
 *
 * ## Theming
 * - Uses [MaterialTheme.colorScheme] for color consistency.
 * - Layout spacing defined with 12.dp horizontal and 8.dp vertical padding.
 * - Built on [ElevatedCard] for a clean, modern Material 3 appearance.
 *
 * @see FeatureRow
 * @see ConfigContentScreen
 */
@Composable
fun FeatureRowItem(row: FeatureRow) {
    val color =
        if (row.allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val label = if (row.allowed) "R" else "N"

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feature name on the left
            Text(
                text = row.feature.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )

            // Read-only chip showing "R" or "N"
            AssistChip(
                onClick = {},
                label = { Text(label) },
                enabled = false,
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = color.copy(alpha = 0.15f),
                    disabledLabelColor = color
                )
            )
        }
    }
}
