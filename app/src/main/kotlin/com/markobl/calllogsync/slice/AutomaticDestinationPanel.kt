package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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

internal data class AutomaticDestinationOption(val token: String, val label: String)

internal data class AutomaticDestinationUiState(
    val options: List<AutomaticDestinationOption> = emptyList(),
    val selectedToken: String? = null,
    val enabled: Boolean = false,
)

@Composable
internal fun AutomaticDestinationPanel(
    state: AutomaticDestinationUiState,
    chooseLabel: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = state.options.firstOrNull { it.token == state.selectedToken }?.label

    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComposeActionButton(
            label = selectedLabel ?: chooseLabel,
            tag = "auto_calendar_picker",
            enabled = state.enabled && state.options.isNotEmpty(),
            accessibilityLabel = selectedLabel ?: chooseLabel,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("auto_calendar_options"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.options.forEachIndexed { index, option ->
                    ComposeActionButton(
                        label = option.label,
                        tag = "auto_calendar_option_$index",
                        enabled = state.enabled,
                        onClick = {
                            expanded = false
                            onSelect(option.token)
                        },
                    )
                }
            }
        }
    }
}
