package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
internal fun AutomaticSettingsActionsPanel(
    state: AutomaticSettingsActionsUiState,
    enableLabel: String,
    pauseLabel: String,
    refreshLabel: String,
    onEnable: () -> Unit,
    onPause: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComposeActionButton(
            label = enableLabel,
            tag = "auto_enable",
            enabled = state.enableEnabled,
            onClick = onEnable,
        )
        ComposeActionButton(
            label = pauseLabel,
            tag = "auto_pause",
            enabled = state.pauseEnabled,
            onClick = onPause,
        )
        ComposeActionButton(
            label = refreshLabel,
            tag = "auto_refresh",
            enabled = state.refreshEnabled,
            onClick = onRefresh,
        )
    }
}
