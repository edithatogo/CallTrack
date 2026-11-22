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

internal data class CalendarDestinationOption(val token: String, val label: String)

internal data class CalendarDestinationUiState(
    val options: List<CalendarDestinationOption> = emptyList(),
    val selectedToken: String? = null,
    val enabled: Boolean = false,
)

@Composable
internal fun CalendarDestinationPicker(
    state: CalendarDestinationUiState,
    chooseLabel: String,
    tagPrefix: String,
    unavailableLabel: String? = null,
    optionsEnabled: Boolean = state.enabled,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = state.options.firstOrNull { it.token == state.selectedToken }?.label ?: chooseLabel

    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComposeActionButton(
            label = label,
            tag = "${tagPrefix}_picker",
            enabled = state.enabled && state.options.isNotEmpty(),
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_options"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.options.forEachIndexed { index, option ->
                    ComposeActionButton(
                        label = option.label,
                        tag = "${tagPrefix}_option_$index",
                        enabled = optionsEnabled,
                        onClick = {
                            expanded = false
                            onSelect(option.token)
                        },
                    )
                }
            }
        }
        if (state.options.isEmpty() && unavailableLabel != null) {
            BasicText(unavailableLabel, modifier = Modifier.testTag("${tagPrefix}_unavailable"))
        }
    }
}
