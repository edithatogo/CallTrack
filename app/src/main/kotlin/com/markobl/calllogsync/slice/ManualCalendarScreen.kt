package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/** Presentation for manual logging. Provider, permission and delivery effects stay in Activity. */
@Composable
internal fun ManualCalendarScreen(
    intro: String,
    calendarState: ManualCalendarControlsState,
    logState: ManualLogUiState,
    chooseLabel: String,
    accessLabel: String,
    unavailableLabel: String,
    automaticLabel: String,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
    onLogLatest: () -> Unit,
    onOpenAutomatic: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(16.dp)
                .semantics { testTagsAsResourceId = true }
                .testTag("slice_screen")
    ) {
        BasicText(intro, modifier = Modifier.fillMaxWidth().testTag("slice_intro"))
        Column(Modifier.fillMaxWidth().testTag("slice_calendar_controls_panel")) {
            ManualCalendarControlsPanel(
                state = calendarState,
                chooseLabel = chooseLabel,
                accessLabel = accessLabel,
                unavailableLabel = unavailableLabel,
                onRequestAccess = onRequestAccess,
                onSelect = onSelect,
            )
        }
        Column(Modifier.fillMaxWidth().testTag("slice_manual_panel")) {
            ManualLogPanel(state = logState, onLogLatest = onLogLatest)
        }
        ComposeActionButton(
            label = automaticLabel,
            tag = "open_automatic",
            enabled = true,
            onClick = onOpenAutomatic,
        )
    }
}
