package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class ManualCalendarOption(val token: String, val label: String)

internal data class ManualCalendarControlsState(
    val options: List<ManualCalendarOption> = emptyList(),
    val selectedToken: String? = null,
    val enabled: Boolean = false,
)

@Composable
internal fun ManualCalendarControlsPanel(
    state: ManualCalendarControlsState,
    chooseLabel: String,
    accessLabel: String,
    unavailableLabel: String,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = state.options.firstOrNull { it.token == state.selectedToken }?.label

    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComposeActionButton(
            label = accessLabel,
            tag = "slice_calendar_access",
            enabled = true,
            onClick = onRequestAccess,
        )
        ComposeActionButton(
            label = selectedLabel ?: chooseLabel,
            tag = "slice_calendar_picker",
            enabled = state.enabled && state.options.isNotEmpty(),
            accessibilityLabel = selectedLabel ?: chooseLabel,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("slice_calendar_options"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.options.forEachIndexed { index, option ->
                    ComposeActionButton(
                        label = option.label,
                        tag = "slice_calendar_option_$index",
                        enabled = state.enabled,
                        onClick = {
                            expanded = false
                            onSelect(option.token)
                        },
                    )
                }
            }
        }
        if (state.options.isEmpty()) {
            BasicText(unavailableLabel, modifier = Modifier.testTag("slice_calendar_unavailable"))
        }
    }
}
