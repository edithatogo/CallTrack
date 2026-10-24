package com.markobl.calllogsync.slice

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markobl.calllogsync.R

internal data class AutomaticCallTypesUiState(
    val incoming: Boolean = true,
    val outgoing: Boolean = true,
    val missed: Boolean = true,
)

@Composable
internal fun AutomaticCallTypesPanel(
    state: AutomaticCallTypesUiState,
    incomingLabel: String,
    outgoingLabel: String,
    missedLabel: String,
    enabled: Boolean,
    onStateChange: (AutomaticCallTypesUiState) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CallTypeOption("auto_incoming", incomingLabel, state.incoming, enabled) {
            onStateChange(state.copy(incoming = it))
        }
        CallTypeOption("auto_outgoing", outgoingLabel, state.outgoing, enabled) {
            onStateChange(state.copy(outgoing = it))
        }
        CallTypeOption("auto_missed", missedLabel, state.missed, enabled) {
            onStateChange(state.copy(missed = it))
        }
    }
}

@Composable
private fun CallTypeOption(
    tag: String,
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .clearAndSetSemantics {
                    testTagsAsResourceId = true
                    testTag = tag
                    contentDescription = label
                    role = Role.Checkbox
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                }
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(24.dp).border(1.dp, colorResource(R.color.purple_500)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                BasicText("✓", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
        }
        BasicText(label)
    }
}
