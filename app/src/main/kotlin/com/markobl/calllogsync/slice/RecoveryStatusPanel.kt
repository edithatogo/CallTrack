package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
internal fun RecoveryStatusPanel(feedback: String) {
    if (feedback.isNotEmpty()) {
        BasicText(
            text = feedback,
            modifier =
                Modifier.fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("recovery_feedback_text"),
        )
    }
}
