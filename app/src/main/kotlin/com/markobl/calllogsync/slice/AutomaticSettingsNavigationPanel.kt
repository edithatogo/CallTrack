package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

@Composable
internal fun AutomaticSettingsNavigationPanel(
    importLabel: String,
    recoveryLabel: String,
    onOpenImports: () -> Unit,
    onOpenRecovery: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComposeActionButton(
            label = importLabel,
            tag = "open_imports",
            enabled = true,
            onClick = onOpenImports,
        )
        ComposeActionButton(
            label = recoveryLabel,
            tag = "open_recovery",
            enabled = true,
            onClick = onOpenRecovery,
        )
    }
}
