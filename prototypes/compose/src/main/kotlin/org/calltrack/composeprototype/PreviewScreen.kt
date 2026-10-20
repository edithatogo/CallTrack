package org.calltrack.composeprototype

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PreviewScreen(
    state: PreviewState,
    onAccess: () -> Unit,
    onSelect: (Long) -> Unit,
    onLog: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.intro))
            Button(onClick = onAccess, enabled = !state.busy && !state.refreshing) {
                Text(stringResource(R.string.access))
            }
            if (state.calendarAccess) {
                Column(Modifier.selectableGroup()) {
                    CalendarRow(-1, stringResource(R.string.choose), state, onSelect)
                    for (id in state.calendars) {
                        val label =
                            when (id) {
                                7L -> stringResource(R.string.fixture_personal)
                                9L -> stringResource(R.string.fixture_work)
                                else -> stringResource(R.string.fixture_other, id)
                            }
                        CalendarRow(id, label, state, onSelect)
                    }
                }
            }
            Button(onClick = onLog, enabled = state.canLog) { Text(stringResource(R.string.log)) }
            val message =
                when {
                    !state.calendarAccess -> R.string.permission
                    state.busy -> R.string.working
                    state.refreshing -> R.string.refreshing
                    state.calendars.isEmpty() -> R.string.no_calendars
                    else -> R.string.ready
                }
            Text(
                stringResource(message),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(stringResource(R.string.count, state.requests))
        }
    }
}

@Composable
private fun CalendarRow(id: Long, label: String, state: PreviewState, onSelect: (Long) -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(
                    selected = state.selected == id,
                    enabled = state.controlsEnabled,
                    role = Role.RadioButton,
                    onClick = { onSelect(id) },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = state.selected == id,
            onClick = null,
            enabled = state.controlsEnabled,
        )
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
