package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class CalendarDestinationOption(val token: String, val label: String)

internal data class CalendarDestinationUiState(
    val options: List<CalendarDestinationOption> = emptyList(),
    val selectedToken: String? = null,
    val enabled: Boolean = false,
    val checkedTokens: Set<String>? = null,
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
    val label = selectedDestinationLabel(state, chooseLabel)

    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActionButton(
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
                    val tag = "${tagPrefix}_option_$index"
                    if (state.checkedTokens == null) {
                        ActionButton(
                            label = option.label,
                            tag = tag,
                            enabled = optionsEnabled,
                            onClick = {
                                expanded = false
                                onSelect(option.token)
                            },
                        )
                    } else {
                        val checked = option.token in state.checkedTokens
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag(tag)
                                    .toggleable(checked, optionsEnabled, Role.Checkbox) {
                                        onSelect(option.token)
                                    },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked, onCheckedChange = null, enabled = optionsEnabled)
                            Text(option.label)
                        }
                    }
                }
            }
        }
        if (state.options.isEmpty() && unavailableLabel != null) {
            Text(unavailableLabel, modifier = Modifier.testTag("${tagPrefix}_unavailable"))
        }
    }
}

private fun selectedDestinationLabel(
    state: CalendarDestinationUiState,
    chooseLabel: String,
): String =
    if (state.checkedTokens != null) {
        state.options
            .filter { it.token in state.checkedTokens }
            .joinToString { it.label }
            .ifEmpty { chooseLabel }
    } else {
        state.options.firstOrNull { it.token == state.selectedToken }?.label ?: chooseLabel
    }
