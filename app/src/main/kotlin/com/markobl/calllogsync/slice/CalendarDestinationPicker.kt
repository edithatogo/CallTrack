package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.markobl.calllogsync.R

internal data class CalendarDestinationOption(val token: String, val label: String)

internal fun calendarChoiceLabels(
    calendars: List<AndroidCalendarSink.Choice>,
    unnamed: String,
): Map<Long, String> {
    val byTitle = calendars.groupBy { it.name?.takeIf(String::isNotBlank) ?: unnamed }
    return buildMap {
        for ((title, choices) in byTitle) {
            if (choices.size == 1) {
                put(choices.single().id, title)
            } else {
                val byAccount = choices.groupBy {
                    it.accountName ?: it.accountType ?: it.id.toString()
                }
                for ((account, peers) in byAccount) {
                    for (choice in peers) {
                        put(
                            choice.id,
                            if (peers.size == 1) {
                                "$title ($account)"
                            } else {
                                "$title ($account, ${choice.id})"
                            },
                        )
                    }
                }
            }
        }
    }
}

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
    var restorePickerFocus by remember { mutableStateOf(false) }
    val pickerFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    val hasOptions = state.options.isNotEmpty()
    val enabled = state.enabled && hasOptions
    val keyboardCanChoose = hasOptions && inputMode.inputMode == InputMode.Keyboard
    LaunchedEffect(restorePickerFocus, enabled, keyboardCanChoose) {
        if (restorePickerFocus && (enabled || !keyboardCanChoose)) {
            if (keyboardCanChoose) pickerFocus.requestFocus()
            restorePickerFocus = false
        }
    }
    val checkedTokens = state.checkedTokens

    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CalendarPickerAnchor(
            state = state,
            chooseLabel = chooseLabel,
            tag = "${tagPrefix}_picker",
            expanded = expanded,
            modifier = Modifier.focusRequester(pickerFocus),
        ) {
            expanded = !expanded
        }
        if (expanded) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .testTag("${tagPrefix}_options")
                        .then(if (checkedTokens == null) Modifier.selectableGroup() else Modifier),
            ) {
                state.options.forEachIndexed { index, option ->
                    val selected =
                        checkedTokens?.contains(option.token)
                            ?: (option.token == state.selectedToken)
                    CalendarOptionRow(
                        option.label,
                        "${tagPrefix}_option_$index",
                        checkedTokens != null,
                        selected,
                        optionsEnabled,
                    ) {
                        if (checkedTokens == null) {
                            restorePickerFocus = inputMode.inputMode == InputMode.Keyboard
                            expanded = false
                        }
                        onSelect(option.token)
                    }
                }
            }
        }
        if (unavailableLabel != null) {
            Text(unavailableLabel, modifier = Modifier.testTag("${tagPrefix}_unavailable"))
        }
    }
}

@Composable
private fun CalendarPickerAnchor(
    state: CalendarDestinationUiState,
    chooseLabel: String,
    tag: String,
    expanded: Boolean,
    modifier: Modifier,
    onToggle: () -> Unit,
) {
    val checkedTokens = state.checkedTokens
    val label =
        if (checkedTokens != null) {
            val count = state.options.count { it.token in checkedTokens }
            when (count) {
                0 -> chooseLabel
                1 -> state.options.first { it.token in checkedTokens }.label
                else -> pluralStringResource(R.plurals.calendar_selection_count, count, count)
            }
        } else {
            state.options.firstOrNull { it.token == state.selectedToken }?.label ?: chooseLabel
        }
    val disclosureState =
        stringResource(
            if (expanded) R.string.calendar_picker_expanded else R.string.calendar_picker_collapsed,
        )
    ActionButton(
        label = label,
        tag = tag,
        enabled = state.enabled && state.options.isNotEmpty(),
        onClick = onToggle,
        style = ButtonStyle.Outlined,
        modifier = modifier.semantics { stateDescription = disclosureState },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )
}

@Composable
private fun CalendarOptionRow(
    label: String,
    tag: String,
    multiple: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val target =
        Modifier.heightIn(min = 48.dp).testTag(tag).semantics { text = AnnotatedString(label) }
    val selection =
        if (multiple) {
            target.toggleable(selected, enabled, Role.Checkbox) { onClick() }
        } else {
            target.selectable(selected, enabled, Role.RadioButton, onClick = onClick)
        }
    ListItem(
        leadingContent = {
            if (multiple) {
                Checkbox(selected, onCheckedChange = null, enabled = enabled)
            } else {
                RadioButton(selected, onClick = null, enabled = enabled)
            }
        },
        modifier = selection,
    ) {
        Text(label, modifier = Modifier.clearAndSetSemantics {})
    }
}
