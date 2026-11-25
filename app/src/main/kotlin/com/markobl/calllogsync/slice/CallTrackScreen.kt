package com.markobl.calllogsync.slice

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V2
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass
import com.markobl.calllogsync.R

private val FormMaxWidth = 640.dp
private val ExpandedFormMaxWidth = 720.dp
private val MinimumTargetSize = 48.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ComponentActivity.setCallTrackContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    setContentView(
        ComposeView(this).apply {
            id = R.id.calltrack_root
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val dark = isSystemInDarkTheme()
                val configuration = LocalConfiguration.current
                val colors =
                    remember(dark, configuration) {
                        if (dark) {
                            dynamicDarkColorScheme(context)
                        } else {
                            dynamicLightColorScheme(context)
                        }
                    }
                MaterialExpressiveTheme(colorScheme = colors, content = content)
            }
        },
    )
}

@Composable
internal fun CallTrackScaffold(
    @StringRes title: Int,
    onNavigateUp: (() -> Unit)?,
    screenTag: String = "calltrack_screen",
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val windowSizeClass by
        remember(windowInfo) {
            derivedStateOf(structuralEqualityPolicy()) {
                val size = windowInfo.containerDpSize
                BREAKPOINTS_V2.computeWindowSizeClass(size.width.value, size.height.value)
            }
        }
    val roomy =
        windowSizeClass.isAtLeastBreakpoint(
            WIDTH_DP_EXPANDED_LOWER_BOUND,
            HEIGHT_DP_MEDIUM_LOWER_BOUND,
        )
    val formWidth = if (roomy) ExpandedFormMaxWidth else FormMaxWidth
    val formGutter =
        if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) 24.dp else 16.dp
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { CallTrackAppBar(title, onNavigateUp) },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier.widthIn(max = formWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .testTag(screenTag)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = formGutter, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallTrackAppBar(@StringRes title: Int, onNavigateUp: (() -> Unit)?) {
    TopAppBar(
        title = {
            Text(
                stringResource(title),
                modifier = Modifier.semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onNavigateUp != null) {
                val backLabel = stringResource(R.string.navigate_up)
                IconButton(
                    onClick = onNavigateUp,
                    modifier =
                        Modifier.sizeIn(
                                minWidth = MinimumTargetSize,
                                minHeight = MinimumTargetSize,
                            )
                            .testTag("navigate_up")
                            .semantics {
                                contentDescription = backLabel
                                role = Role.Button
                            },
                ) {
                    Icon(
                        painterResource(R.drawable.material_arrow_back),
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

internal enum class ButtonStyle {
    Filled,
    Tonal,
    Outlined,
    Text,
}

@Composable
internal fun ActionButton(
    label: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    style: ButtonStyle = ButtonStyle.Filled,
) {
    val target =
        Modifier.sizeIn(minWidth = MinimumTargetSize, minHeight = MinimumTargetSize)
            .testTag(tag)
            .semantics {
                testTagsAsResourceId = true
                role = Role.Button
                text = AnnotatedString(label)
            }
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Text(label, modifier = Modifier.clearAndSetSemantics {})
    }

    when (style) {
        ButtonStyle.Filled -> Button(onClick, target, enabled, content = content)
        ButtonStyle.Tonal -> FilledTonalButton(onClick, target, enabled, content = content)
        ButtonStyle.Outlined -> OutlinedButton(onClick, target, enabled, content = content)
        ButtonStyle.Text -> TextButton(onClick, target, enabled, content = content)
    }
}

@Composable
internal fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
internal fun StatusCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
internal fun NavigationItem(label: String, tag: String, onClick: () -> Unit) {
    ListItem(
        trailingContent = {
            Icon(painterResource(R.drawable.material_chevron_right), contentDescription = null)
        },
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = MinimumTargetSize)
                .testTag(tag)
                .semantics {
                    testTagsAsResourceId = true
                    text = AnnotatedString(label)
                }
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(label, modifier = Modifier.clearAndSetSemantics {})
    }
}
