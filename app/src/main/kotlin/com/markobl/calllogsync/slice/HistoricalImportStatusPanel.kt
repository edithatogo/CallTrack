package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class HistoricalImportStatusUiState(val summary: String, val feedback: String)

@Composable
internal fun HistoricalImportStatusPanel(state: HistoricalImportStatusUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoricalImportSummaryText(state.summary)
        HistoricalImportFeedbackText(state.feedback)
    }
}

@Composable
internal fun HistoricalImportSummaryText(text: String) {
    BasicText(
        text = text,
        modifier =
            Modifier.fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("import_summary_text"),
    )
}

@Composable
internal fun HistoricalImportFeedbackText(text: String) {
    if (text.isNotEmpty()) {
        BasicText(
            text = text,
            modifier =
                Modifier.fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("import_feedback_text"),
        )
    }
}
