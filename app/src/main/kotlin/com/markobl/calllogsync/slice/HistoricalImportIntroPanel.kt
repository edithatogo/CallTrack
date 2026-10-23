package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
internal fun HistoricalImportIntroPanel(text: String) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        BasicText(text = text, modifier = Modifier.testTag("import_intro_text"))
    }
}
