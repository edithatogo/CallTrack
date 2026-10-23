package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class HistoricalImportHistoryItem(val token: String, val label: String)

internal data class HistoricalImportHistoryUiState(
    val items: List<HistoricalImportHistoryItem> = emptyList()
)

@Composable
internal fun HistoricalImportHistoryPanel(
    state: HistoricalImportHistoryUiState,
    title: String,
    emptyMessage: String,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(title, modifier = Modifier.testTag("import_history_title"))
        if (state.items.isEmpty()) {
            BasicText(emptyMessage, modifier = Modifier.testTag("import_history_empty"))
        } else {
            state.items.forEachIndexed { index, item ->
                ComposeActionButton(
                    label = item.label,
                    tag = "import_history_item_$index",
                    enabled = true,
                    onClick = { onOpen(item.token) },
                )
            }
        }
    }
}
