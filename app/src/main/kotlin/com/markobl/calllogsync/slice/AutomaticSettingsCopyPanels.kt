package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
internal fun AutomaticSettingsIntroPanel(text: String) {
    Text(
        text = text,
        modifier =
            Modifier.fillMaxWidth()
                .semantics { testTagsAsResourceId = true }
                .testTag("auto_intro_text"),
    )
}

@Composable
internal fun AutomaticSettingsAccessPanel(label: String, onRequestAccess: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        ActionButton(
            label = label,
            tag = "auto_access_action",
            enabled = true,
            onClick = onRequestAccess,
        )
    }
}
