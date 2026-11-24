package com.markobl.calllogsync.slice

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.markobl.calllogsync.R

internal fun ComponentActivity.setCallTrackContent(content: @Composable () -> Unit) {
    setContentView(
        ComposeView(this).apply {
            id = R.id.calltrack_root
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val colors =
                    if (isSystemInDarkTheme()) {
                        darkColorScheme(
                            primary = colorResource(R.color.purple_200),
                            onPrimary = Color.Black,
                        )
                    } else {
                        lightColorScheme(
                            primary = colorResource(R.color.purple_500),
                            onPrimary = Color.White,
                        )
                    }
                MaterialTheme(colorScheme = colors) {
                    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                            Column(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .testTag("calltrack_screen")
                                        .semantics { testTagsAsResourceId = true }
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                            ) {
                                content()
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun ActionButton(label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag).semantics {
                testTagsAsResourceId = true
                role = Role.Button
                text = AnnotatedString(label)
            },
    ) {
        Text(label, modifier = Modifier.clearAndSetSemantics {})
    }
}
