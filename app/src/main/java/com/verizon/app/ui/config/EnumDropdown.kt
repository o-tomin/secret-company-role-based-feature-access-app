package com.otomin.app.ui.config

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A reusable composable dropdown for displaying and selecting from a list of enum-like string options.
 *
 * This component is used throughout the configuration UI to allow users to pick:
 * - **Acting role**
 * - **Target role**
 * - **Plan**
 *
 * It displays a Material 3 [OutlinedTextField] as the anchor element, and expands into
 * a dropdown menu when tapped.
 *
 * ## Behavior
 * - Displays the current [selectedText] in a read-only text field.
 * - Expands to show all [options] when the field is clicked.
 * - Invokes [onPick] with the chosen option and automatically closes the dropdown.
 *
 * ## Example usage
 * ```kotlin
 * EnumDropdown(
 *     label = "Plan",
 *     selectedText = "Free",
 *     options = listOf("Free", "Basic", "Premium"),
 *     onPick = { selected -> println("Picked $selected") }
 * )
 * ```
 *
 * ## Accessibility & constraints
 * - The dropdown width is constrained to a minimum of 160 dp for consistent sizing.
 * - Uses Material3’s [ExposedDropdownMenuBox] to align menu positioning with the text field.
 * - The text field is marked **read-only** to prevent user editing of the selected value.
 *
 * @param label The field label displayed above the text box (e.g., “Plan”, “Target”).
 * @param selectedText The currently selected option as a displayable string.
 * @param options The list of available string options to present in the dropdown.
 * @param onPick Callback invoked when a user selects a new option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnumDropdown(
    label: String,
    selectedText: String,
    options: List<String>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .widthIn(min = 160.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onPick(opt)
                    }
                )
            }
        }
    }
}