package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ManualCalendarControlsPanel(
    state: CalendarDestinationUiState,
    chooseLabel: String,
    accessLabel: String,
    unavailableLabel: String?,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
    hasCalendarAccess: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActionButton(
            label = accessLabel,
            tag = "slice_calendar_access",
            enabled = true,
            onClick = onRequestAccess,
            style = if (hasCalendarAccess) ButtonStyle.Text else ButtonStyle.Filled,
        )
        CalendarDestinationPicker(
            state = state,
            chooseLabel = chooseLabel,
            tagPrefix = "slice_calendar",
            unavailableLabel = unavailableLabel?.takeIf { state.options.isEmpty() },
            onSelect = onSelect,
        )
    }
}
