package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.markobl.calllogsync.R

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
        NavigationItem(
            label = importLabel,
            tag = "open_imports",
            onClick = onOpenImports,
        )
        NavigationItem(
            label = recoveryLabel,
            tag = "open_recovery",
            onClick = onOpenRecovery,
        )
    }
}

@Composable
internal fun ColumnScope.SettingsNavigationPane(
    currentSection: NavKey,
    onSelect: (CallTrackRoute) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(stringResource(R.string.auto_title)) },
        selected = currentSection == CallTrackRoute.Automatic,
        onClick = { onSelect(CallTrackRoute.Automatic) },
        modifier = Modifier.testTag("section_automatic"),
    )
    NavigationDrawerItem(
        label = { Text(stringResource(R.string.import_open)) },
        selected = currentSection == CallTrackRoute.Import,
        onClick = { onSelect(CallTrackRoute.Import) },
        modifier = Modifier.testTag("section_imports"),
    )
    NavigationDrawerItem(
        label = { Text(stringResource(R.string.recovery_open)) },
        selected = currentSection == CallTrackRoute.Recovery,
        onClick = { onSelect(CallTrackRoute.Recovery) },
        modifier = Modifier.testTag("section_recovery"),
    )
}
