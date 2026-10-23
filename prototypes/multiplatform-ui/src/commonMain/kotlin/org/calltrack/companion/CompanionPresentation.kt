package org.calltrack.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Immutable presentation-only state; no provider, account, or durable receipt data. */
data class CompanionUiState(
    val destinationSelected: Boolean = false,
    val previewVisible: Boolean = false,
)

enum class CompanionUiAction {
    SELECT_SAMPLE_DESTINATION,
    CLEAR_DESTINATION,
    REQUEST_PREVIEW,
}

/** Pure transition function so selection and preview policy can be tested without Compose. */
fun reduceCompanionUi(state: CompanionUiState, action: CompanionUiAction): CompanionUiState =
    when (action) {
        CompanionUiAction.SELECT_SAMPLE_DESTINATION ->
            state.copy(destinationSelected = true, previewVisible = false)
        CompanionUiAction.CLEAR_DESTINATION -> CompanionUiState()
        CompanionUiAction.REQUEST_PREVIEW ->
            if (state.destinationSelected) state.copy(previewVisible = true) else state
    }

/** Shared rendering only. All actions are explicit callbacks; this composable performs no IO. */
@Composable
fun CompanionScreen(state: CompanionUiState, onAction: (CompanionUiAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("CallTrack companion experiment", style = MaterialTheme.typography.h5)
        Text("Sample data only. No calendar connection or file import.")
        Text("Automatic cellular capture: unavailable in this experiment.")
        Text(
            if (state.destinationSelected) "Destination: sample calendar"
            else "Choose a destination"
        )
        Button(
            onClick = {
                onAction(
                    if (state.destinationSelected) CompanionUiAction.CLEAR_DESTINATION
                    else CompanionUiAction.SELECT_SAMPLE_DESTINATION
                )
            }
        ) {
            Text(if (state.destinationSelected) "Clear selection" else "Use sample calendar")
        }
        Button(
            onClick = { onAction(CompanionUiAction.REQUEST_PREVIEW) },
            enabled = state.destinationSelected,
        ) {
            Text("Preview sample")
        }
        if (state.previewVisible) Text("2 synthetic entries ready for review; nothing saved.")
    }
}
