// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.markobl.calllogsync.R
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CallTrackRoute : NavKey {
    @Serializable data object Manual : CallTrackRoute

    @Serializable data object Automatic : CallTrackRoute

    @Serializable data object Import : CallTrackRoute

    @Serializable data object Recovery : CallTrackRoute
}

internal fun ComponentActivity.setCallTrackNavigation(root: CallTrackRoute) {
    setCallTrackContent { CallTrackNavigation(this, root) }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun CallTrackNavigation(activity: ComponentActivity, root: CallTrackRoute) {
    val stack = rememberNavBackStack(root)
    val popEntry: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val navigateUp: () -> Unit = {
        if (stack.size > 1) {
            popEntry()
        } else {
            activity.finish()
        }
    }
    val openSettingsSection: (CallTrackRoute) -> Unit = { section ->
        if (stack.last() != section) {
            when {
                section == CallTrackRoute.Automatic -> popEntry()
                stack.last() == CallTrackRoute.Automatic -> stack.add(section)
                else -> stack[stack.lastIndex] = section
            }
        }
    }
    NavDisplay(
        backStack = stack,
        onBack = popEntry,
        sceneStrategies = rememberSettingsScenes(),
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<CallTrackRoute.Manual> {
                    CallTrackScaffold(R.string.calltrack_title, onNavigateUp = null) {
                        ManualLoggingScreen(activity) {
                            if (stack.last() == CallTrackRoute.Manual) {
                                stack.add(CallTrackRoute.Automatic)
                            }
                        }
                    }
                }
                entry<CallTrackRoute.Automatic>(metadata = ListDetailSceneStrategy.listPane()) {
                    SettingsEntry(activity, stack.last(), navigateUp, openSettingsSection)
                }
                entry<CallTrackRoute.Import>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsDetail(R.string.import_open, navigateUp) {
                        HistoricalImportScreen(activity)
                    }
                }
                entry<CallTrackRoute.Recovery>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsDetail(R.string.recovery_title, navigateUp) {
                        ReceiptRecoveryScreen(activity)
                    }
                }
            },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun rememberSettingsScenes(): List<SceneStrategy<NavKey>> {
    val window = currentWindowAdaptiveInfoV2()
    val directive =
        remember(window) {
            with(calculatePaneScaffoldDirective(window)) {
                PaneScaffoldDirective(
                    maxHorizontalPartitions = maxHorizontalPartitions,
                    horizontalPartitionSpacerSize = horizontalPartitionSpacerSize,
                    maxVerticalPartitions = maxVerticalPartitions,
                    verticalPartitionSpacerSize = verticalPartitionSpacerSize,
                    defaultPanePreferredWidth = defaultPanePreferredWidth,
                    defaultPanePreferredHeight = defaultPanePreferredHeight,
                    excludedBounds = excludedBounds,
                    // Opening a screen must not focus a date field and raise the keyboard.
                    shouldAutoFocusCurrentDestination = false,
                )
            }
        }
    val listDetail =
        rememberListDetailSceneStrategy<NavKey>(
            backNavigationBehavior = BackNavigationBehavior.PopLatest,
            directive = directive,
        )
    return remember(listDetail) {
        val settingsScene =
            SceneStrategy<NavKey> { entries ->
                // Use the wide scene only for a real pair, leaving standalone routes full width.
                with(listDetail) { calculateScene(entries) }?.takeIf { it.entries.size == 2 }
            }
        listOf(settingsScene)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsEntry(
    activity: ComponentActivity,
    currentSection: NavKey,
    navigateUp: () -> Unit,
    openSettingsSection: (CallTrackRoute) -> Unit,
) {
    if (LocalListDetailSceneScope.current != null) {
        CallTrackScaffold(
            R.string.settings_title,
            onNavigateUp = null,
            screenTag = "settings_navigation",
        ) {
            SettingsNavigationPane(currentSection, openSettingsSection)
        }
    } else {
        CallTrackScaffold(R.string.auto_title, navigateUp) {
            AutomaticSettingsScreen(
                activity = activity,
                onOpenImports = { openSettingsSection(CallTrackRoute.Import) },
                onOpenRecovery = { openSettingsSection(CallTrackRoute.Recovery) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsDetail(
    @StringRes title: Int,
    navigateUp: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val back = navigateUp.takeIf { LocalListDetailSceneScope.current == null }
    CallTrackScaffold(title, onNavigateUp = back, content = content)
}
