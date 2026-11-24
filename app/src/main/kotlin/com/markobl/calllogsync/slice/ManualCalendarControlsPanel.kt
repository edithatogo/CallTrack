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
    unavailableLabel: String,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActionButton(
            label = accessLabel,
            tag = "slice_calendar_access",
            enabled = true,
            onClick = onRequestAccess,
        )
        CalendarDestinationPicker(
            state = state,
            chooseLabel = chooseLabel,
            tagPrefix = "slice_calendar",
            unavailableLabel = unavailableLabel,
            onSelect = onSelect,
        )
    }
}
