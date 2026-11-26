package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/** Presentation for manual logging. */
@Composable
internal fun ManualCalendarScreen(
    intro: String,
    calendarState: CalendarDestinationUiState,
    logState: ManualLogUiState,
    chooseLabel: String,
    accessLabel: String,
    unavailableLabel: String?,
    automaticLabel: String,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
    onLogLatest: () -> Unit,
    onOpenAutomatic: () -> Unit,
    hasCalendarAccess: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier.fillMaxWidth().semantics {
                testTagsAsResourceId = true
                testTag = "slice_screen"
            },
    ) {
        Text(
            intro,
            modifier =
                Modifier.fillMaxWidth().semantics {
                    testTagsAsResourceId = true
                    testTag = "slice_intro"
                },
        )
        Column(
            Modifier.fillMaxWidth().semantics {
                testTagsAsResourceId = true
                testTag = "slice_calendar_controls_panel"
            },
        ) {
            ManualCalendarControlsPanel(
                state = calendarState,
                chooseLabel = chooseLabel,
                accessLabel = accessLabel,
                unavailableLabel = unavailableLabel,
                onRequestAccess = onRequestAccess,
                onSelect = onSelect,
                hasCalendarAccess = hasCalendarAccess,
            )
        }
        Column(
            Modifier.fillMaxWidth().semantics {
                testTagsAsResourceId = true
                testTag = "slice_manual_panel"
            },
        ) {
            ManualLogPanel(state = logState, onLogLatest = onLogLatest)
        }
        NavigationItem(
            label = automaticLabel,
            tag = "open_automatic",
            onClick = onOpenAutomatic,
        )
    }
}
