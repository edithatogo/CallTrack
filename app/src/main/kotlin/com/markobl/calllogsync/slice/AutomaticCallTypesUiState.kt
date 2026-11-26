package com.markobl.calllogsync.slice

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.markobl.calllogsync.R

internal fun Context.callTypeLabels(incoming: Boolean, outgoing: Boolean, missed: Boolean): String =
    buildList {
        if (incoming) add(getString(R.string.auto_incoming))
        if (outgoing) add(getString(R.string.auto_outgoing))
        if (missed) add(getString(R.string.auto_missed))
    }
    .joinToString()

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
    FlowRow(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CallTypeOption(
            "auto_incoming",
            incomingLabel,
            state.incoming,
            onCheckedChange = { onStateChange(state.copy(incoming = it)) },
            enabled = enabled,
        )
        CallTypeOption(
            "auto_outgoing",
            outgoingLabel,
            state.outgoing,
            onCheckedChange = { onStateChange(state.copy(outgoing = it)) },
            enabled = enabled,
        )
        CallTypeOption(
            "auto_missed",
            missedLabel,
            state.missed,
            onCheckedChange = { onStateChange(state.copy(missed = it)) },
            enabled = enabled,
        )
    }
}

@Composable
internal fun CallTypeOption(
    tag: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .clearAndSetSemantics {
                    testTagsAsResourceId = true
                    testTag = tag
                    role = Role.Checkbox
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    contentDescription = label
                }
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label)
    }
}
