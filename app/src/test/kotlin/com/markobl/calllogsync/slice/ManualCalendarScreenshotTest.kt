// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
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
class ManualCalendarScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun manualCalendarBeforeAccess() {
        compose.setContent {
            MaterialExpressiveTheme {
                Surface {
                    Box(Modifier.width(360.dp)) {
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
            }
        }
        compose
            .onRoot()
            .captureRoboImage(
                "manual-calendar-before-access.png",
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
