package com.markobl.calllogsync.slice

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markobl.calllogsync.R

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
        ImportTypeFilter("import_incoming", incomingLabel, state.incoming) {
            onStateChange(state.copy(incoming = it))
        }
        ImportTypeFilter("import_outgoing", outgoingLabel, state.outgoing) {
            onStateChange(state.copy(outgoing = it))
        }
        ImportTypeFilter("import_missed", missedLabel, state.missed) {
            onStateChange(state.copy(missed = it))
        }
    }
}

@Composable
private fun ImportDateField(
    label: String,
    tag: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(text = label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.fillMaxWidth().semantics { contentDescription = label }.testTag(tag),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default,
            textStyle = TextStyle(fontSize = 16.sp),
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .border(1.dp, colorResource(R.color.purple_500))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (value.isEmpty()) {
                        BasicText("YYYY-MM-DD", style = TextStyle(fontSize = 16.sp))
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun ImportTypeFilter(
    tag: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .testTag(tag)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(24.dp).border(1.dp, colorResource(R.color.purple_500)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked)
                BasicText("✓", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
        BasicText(text = label)
    }
}
