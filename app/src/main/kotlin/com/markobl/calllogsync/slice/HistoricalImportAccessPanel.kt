package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
internal fun HistoricalImportAccessPanel(label: String, onRequestAccess: () -> Unit) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ComposeActionButton(
            label = label,
            tag = "import_access_action",
            enabled = true,
            onClick = onRequestAccess,
        )
    }
}
