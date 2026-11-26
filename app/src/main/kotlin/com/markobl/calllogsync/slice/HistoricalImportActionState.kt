package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class HistoricalImportActionState(
    val createEnabled: Boolean = false,
    val continueEnabled: Boolean = false,
    val confirmEnabled: Boolean = false,
    val stopEnabled: Boolean = false,
    val cancelEnabled: Boolean = false,
    val newerEnabled: Boolean = false,
    val olderEnabled: Boolean = false,
    val hasRequiredAccess: Boolean = false,
    val sessionSelected: Boolean = false,
)

@Composable
internal fun HistoricalImportCreateActionPanel(
    state: HistoricalImportActionState,
    label: String,
    onCreate: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ActionButton(
            label = label,
            tag = "import_create",
            enabled = state.createEnabled,
            onClick = onCreate,
            style =
                if (state.confirmEnabled || state.continueEnabled) {
                    ButtonStyle.Outlined
                } else {
                    ButtonStyle.Filled
                },
        )
    }
}

@Composable
internal fun HistoricalImportRunActionsPanel(
    state: HistoricalImportActionState,
    continueLabel: String,
    confirmLabel: String,
    stopLabel: String,
    cancelLabel: String,
    onContinue: () -> Unit,
    onConfirm: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                testTagsAsResourceId = true
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                confirmLabel,
                "import_confirm",
                state.confirmEnabled,
                onConfirm,
                if (state.confirmEnabled) ButtonStyle.Filled else ButtonStyle.Outlined,
            )
            ActionButton(
                continueLabel,
                "import_continue",
                state.continueEnabled,
                onContinue,
                if (state.continueEnabled && !state.confirmEnabled) {
                    ButtonStyle.Filled
                } else {
                    ButtonStyle.Outlined
                },
            )
            ActionButton(stopLabel, "import_stop", state.stopEnabled, onStop, ButtonStyle.Outlined)
            ActionButton(
                cancelLabel,
                "import_cancel",
                state.cancelEnabled,
                onCancel,
                ButtonStyle.Text,
            )
        }
    }
}

@Composable
internal fun HistoricalImportPaginationPanel(
    state: HistoricalImportActionState,
    newerLabel: String,
    olderLabel: String,
    onNewer: () -> Unit,
    onOlder: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                testTagsAsResourceId = true
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            ActionButton(newerLabel, "import_newer", state.newerEnabled, onNewer, ButtonStyle.Text)
        }
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            ActionButton(olderLabel, "import_older", state.olderEnabled, onOlder, ButtonStyle.Text)
        }
    }
}
