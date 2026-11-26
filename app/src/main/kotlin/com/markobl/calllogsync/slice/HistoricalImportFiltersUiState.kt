package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.hintText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class HistoricalImportFiltersUiState(
    val fromDate: String = "",
    val toDate: String = "",
    val incoming: Boolean = true,
    val outgoing: Boolean = true,
    val missed: Boolean = true,
)

@Composable
internal fun HistoricalImportFiltersPanel(
    state: HistoricalImportFiltersUiState,
    fromLabel: String,
    toLabel: String,
    incomingLabel: String,
    outgoingLabel: String,
    missedLabel: String,
    onStateChange: (HistoricalImportFiltersUiState) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImportDateField(fromLabel, "import_from", state.fromDate) {
            onStateChange(state.copy(fromDate = it))
        }
        ImportDateField(toLabel, "import_to", state.toDate) {
            onStateChange(state.copy(toDate = it))
        }
        CallTypeOption(
            "import_incoming",
            incomingLabel,
            state.incoming,
            onCheckedChange = { onStateChange(state.copy(incoming = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        CallTypeOption(
            "import_outgoing",
            outgoingLabel,
            state.outgoing,
            onCheckedChange = { onStateChange(state.copy(outgoing = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        CallTypeOption(
            "import_missed",
            missedLabel,
            state.missed,
            onCheckedChange = { onStateChange(state.copy(missed = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ImportDateField(
    label: String,
    tag: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag(tag).semantics { hintText = label },
        singleLine = true,
        label = { Text(label, Modifier.semantics { hideFromAccessibility() }) },
        placeholder = { Text("YYYY-MM-DD") },
    )
}
