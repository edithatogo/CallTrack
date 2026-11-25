package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class AutomaticSettingsActionsUiState(
    val enableEnabled: Boolean = false,
    val pauseEnabled: Boolean = true,
    val refreshEnabled: Boolean = true,
)

@Composable
internal fun AutomaticStatusActions(
    state: AutomaticSettingsActionsUiState,
    pauseLabel: String,
    refreshLabel: String,
    onPause: () -> Unit,
    onRefresh: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActionButton(pauseLabel, "auto_pause", state.pauseEnabled, onPause, ButtonStyle.Outlined)
        ActionButton(
            refreshLabel,
            "auto_refresh",
            state.refreshEnabled,
            onRefresh,
            ButtonStyle.Text,
        )
    }
}

@Composable
internal fun AutomaticApplyButton(
    state: AutomaticSettingsActionsUiState,
    label: String,
    onApply: () -> Unit,
) {
    ActionButton(
        label = label,
        tag = "auto_enable",
        enabled = state.enableEnabled,
        onClick = onApply,
    )
}
