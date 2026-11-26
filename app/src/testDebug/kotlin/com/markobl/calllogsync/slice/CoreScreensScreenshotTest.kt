// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.markobl.calllogsync.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [37],
    qualifiers = "en-rUS-w360dp-h800dp-notnight-mdpi",
    application = Application::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoreScreensScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun manualCalendarBeforeAccess() {
        capture("manual-calendar-before-access.png") {
            ManualCalendarScreen(
                intro = "Log a call to your calendar",
                calendarState = CalendarDestinationUiState(),
                logState =
                    ManualLogUiState(
                        "Log latest call",
                        "Choose a calendar first",
                        false,
                    ),
                chooseLabel = "Choose calendar",
                accessLabel = "Allow calendar access",
                unavailableLabel = null,
                automaticLabel = "Automatic settings",
                onRequestAccess = {},
                onSelect = {},
                onLogLatest = {},
                onOpenAutomatic = {},
            )
        }
    }

    @Test
    fun automaticSettingsNavigation() {
        capture("automatic-settings-navigation.png") {
            Column { SettingsNavigationPane(CallTrackRoute.Automatic, onSelect = {}) }
        }
    }

    @Test
    fun historicalImportFilters() {
        capture("historical-import-filters.png") {
            HistoricalImportFiltersPanel(
                state = HistoricalImportFiltersUiState("2026-01-01", "2026-02-01", missed = false),
                fromLabel = stringResource(R.string.import_from),
                toLabel = stringResource(R.string.import_to),
                incomingLabel = stringResource(R.string.auto_incoming),
                outgoingLabel = stringResource(R.string.auto_outgoing),
                missedLabel = stringResource(R.string.auto_missed),
                onStateChange = {},
            )
        }
    }

    @Test
    fun recoveryReceipt() {
        val card =
            RecoveryCardUiState(
                token = 1L,
                detail = "Calendar entry awaiting review",
                checkEnabled = true,
                suppressEnabled = true,
                reapproveVisible = false,
                openEventVisible = true,
            )
        capture("recovery-receipt.png") {
            RecoveryCardsPanel(
                page = RecoveryPageUiState(cards = listOf(card)),
                busy = false,
                checkLabel = stringResource(R.string.recovery_check),
                suppressLabel = stringResource(R.string.recovery_suppress),
                reapproveLabel = stringResource(R.string.reapproval_open),
                openEventLabel = stringResource(R.string.recovery_open_event),
                onCheck = {},
                onSuppress = {},
                onReapprove = {},
                onOpenEvent = {},
            )
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun capture(filename: String, content: @Composable () -> Unit) {
        compose.setContent {
            MaterialExpressiveTheme { Surface { Box(Modifier.width(360.dp)) { content() } } }
        }
        compose
            .onRoot()
            .captureRoboImage(
                filename,
                roborazziOptions =
                    RoborazziOptions(
                        compareOptions =
                            RoborazziOptions.CompareOptions(
                                imageComparator = SimpleImageComparator(maxDistance = 0f),
                            ),
                    ),
            )
    }
}
