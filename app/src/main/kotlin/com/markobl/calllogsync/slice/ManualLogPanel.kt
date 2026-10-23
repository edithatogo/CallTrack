package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class ManualLogUiState(
    val logLabel: String,
    val status: String,
    val actionEnabled: Boolean,
)

/**
 * Manual delivery presentation. All provider and permission work stays in the Activity callback.
 */
@Composable
internal fun ManualLogPanel(state: ManualLogUiState, onLogLatest: () -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                testTagsAsResourceId = true
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComposeActionButton(
            label = state.logLabel,
            tag = "slice_log_latest",
            enabled = state.actionEnabled,
            onClick = onLogLatest,
        )
        BasicText(
            text = state.status,
            modifier =
                Modifier.fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("slice_status"),
        )
    }
}
