package com.markobl.calllogsync.slice

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.markobl.calllogsync.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoricalImportActivityComposeTest {
    @get:Rule val compose = createAndroidComposeRule<HistoricalImportActivity>()

    @Test
    fun filtersRenderAcceptInputAndSurviveActivityRecreationWithoutCreatingASession() {
        compose.onNodeWithTag("import_create").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("import_confirm").assertIsNotEnabled()
        compose.onNodeWithTag("import_continue").assertIsNotEnabled()
        compose.onNodeWithText(compose.activity.getString(R.string.import_none)).assertIsDisplayed()
        assertTrue(compose.activity.findViewById<ComposeView>(R.id.import_filters) != null)
        assertTrue(compose.activity.findViewById<ComposeView>(R.id.import_calendar_picker) != null)
        assertTrue(compose.activity.findViewById<ComposeView>(R.id.import_intro_panel) != null)
        assertTrue(compose.activity.findViewById<ComposeView>(R.id.import_access_panel) != null)
        compose.onNodeWithTag("import_access_action").assertIsDisplayed()
        compose.onNodeWithTag("import_intro_text").assertIsDisplayed()
        compose
            .onNodeWithText(
                compose.activity.getString(
                    R.string.import_intro,
                    java.time.ZoneId.systemDefault().id,
                )
            )
            .assertIsDisplayed()
        compose.onNodeWithTag("import_calendar_picker").assertIsNotEnabled()
        compose.onNodeWithTag("import_incoming").assertIsOn()
        compose.onNodeWithTag("import_outgoing").assertIsOn()
        compose.onNodeWithTag("import_missed").assertIsOn()

        compose.onNodeWithTag("import_from").performTextClearance()
        compose.onNodeWithTag("import_from").performTextInput("2026-10-21")
        compose.onNodeWithTag("import_to").performTextClearance()
        compose.onNodeWithTag("import_to").performTextInput("2026-10-24")
        compose.onNodeWithTag("import_outgoing").performTouchInput { click() }.assertIsOff()
        compose.activityRule.scenario.recreate()

        compose.onNodeWithTag("import_from").assertTextEquals("2026-10-21")
        compose.onNodeWithTag("import_to").assertTextEquals("2026-10-24")
        compose.onNodeWithTag("import_incoming").assertIsOn()
        compose.onNodeWithTag("import_outgoing").assertIsOff()
        compose.onNodeWithTag("import_missed").assertIsOn()
        compose.onNodeWithTag("import_create").assertIsNotEnabled()

        SqliteImportStore(compose.activity).use { store ->
            assertTrue(store.sessions(-1, 10).isEmpty())
        }
    }
}
