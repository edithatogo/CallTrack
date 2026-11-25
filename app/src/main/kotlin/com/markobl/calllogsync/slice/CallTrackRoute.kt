// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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

@Composable
private fun CallTrackNavigation(activity: ComponentActivity, root: CallTrackRoute) {
    val stack = rememberNavBackStack(root)
    val navigate: (CallTrackRoute) -> Unit = { route ->
        if (stack.last() != route) stack.add(route)
    }
    val popEntry: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val navigateUp: () -> Unit = {
        if (stack.size > 1) {
            popEntry()
        } else {
            activity.finish()
        }
    }
    NavDisplay(
        backStack = stack,
        onBack = popEntry,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<CallTrackRoute.Manual> {
                    CallTrackScaffold(R.string.calltrack_title, onNavigateUp = null) {
                        ManualLoggingScreen(activity) { navigate(CallTrackRoute.Automatic) }
                    }
                }
                entry<CallTrackRoute.Automatic> {
                    CallTrackScaffold(R.string.auto_title, navigateUp) {
                        AutomaticSettingsScreen(
                            activity = activity,
                            onOpenImports = { navigate(CallTrackRoute.Import) },
                            onOpenRecovery = { navigate(CallTrackRoute.Recovery) },
                        )
                    }
                }
                entry<CallTrackRoute.Import> {
                    CallTrackScaffold(R.string.import_open, navigateUp) {
                        HistoricalImportScreen(activity)
                    }
                }
                entry<CallTrackRoute.Recovery> {
                    CallTrackScaffold(R.string.recovery_title, navigateUp) {
                        ReceiptRecoveryScreen(activity)
                    }
                }
            },
    )
}
