package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecoveryCardsPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyMessageIsOnlyRenderedFromActivitySuppliedState() {
        var visible by mutableStateOf(false)
        compose.setContent { RecoveryEmptyPanel(visible, "No recovery records") }

        compose.onAllNodesWithTag("recovery_empty_message").assertCountEquals(0)
        compose.onAllNodesWithText("No recovery records").assertCountEquals(0)

        visible = true
        compose.onNodeWithTag("recovery_empty_message").assertIsDisplayed()
        compose.onNodeWithText("No recovery records").assertIsDisplayed()
    }

    @Test
    fun cardsRenderBoundedActionsAndInvokeOnlyExplicitCallbacks() {
        val calls = mutableListOf<String>()
        val card =
            RecoveryCardUiState(
                token = 4,
                detail =
                    "Outgoing call — 2026-10-24; actual duration: 12 seconds\nCalendar: Work\nNeeds review",
                checkEnabled = true,
                suppressEnabled = true,
                reapproveVisible = true,
                openEventVisible = true,
            )

        compose.setContent {
            RecoveryCardsPanel(
                cards = listOf(card),
                busy = false,
                checkLabel = "Check existing effect only",
                suppressLabel = "Suppress further retries",
                reapproveLabel = "Reapprove never-attempted insertion",
                openEventLabel = "Open recorded event in calendar app",
                onCheck = { calls += "check:$it" },
                onSuppress = { calls += "suppress:$it" },
                onReapprove = { calls += "reapprove:$it" },
                onOpenEvent = { calls += "open:$it" },
            )
        }

        compose.onNodeWithTag("recovery_card_4").assertIsDisplayed()
        compose.onNodeWithText(card.detail).assertIsDisplayed()
        compose.onAllNodesWithText("No recovery records").assertCountEquals(0)
        assertTrue("Rendering must not start recovery work", calls.isEmpty())

        compose.onNodeWithTag("recovery_check_4").performClick()
        compose.onNodeWithTag("recovery_suppress_4").performClick()
        compose.onNodeWithTag("recovery_reapprove_4").performClick()
        compose.onNodeWithTag("recovery_open_event_4").performClick()
        compose.runOnIdle {
            assertEquals(listOf("check:4", "suppress:4", "reapprove:4", "open:4"), calls)
        }
    }

    @Test
    fun busyAndIneligibleActionsStayDisabledOrAbsent() {
        compose.setContent {
            RecoveryCardsPanel(
                cards =
                    listOf(
                        RecoveryCardUiState(
                            token = 0,
                            detail = "Suppressed record",
                            checkEnabled = false,
                            suppressEnabled = false,
                            reapproveVisible = false,
                            openEventVisible = true,
                        )
                    ),
                busy = true,
                checkLabel = "Check existing effect only",
                suppressLabel = "Suppress further retries",
                reapproveLabel = "Reapprove never-attempted insertion",
                openEventLabel = "Open recorded event in calendar app",
                onCheck = {},
                onSuppress = {},
                onReapprove = {},
                onOpenEvent = {},
            )
        }

        compose.onNodeWithTag("recovery_check_0").assertIsNotEnabled()
        compose.onNodeWithTag("recovery_suppress_0").assertIsNotEnabled()
        compose.onNodeWithTag("recovery_open_event_0").assertIsEnabled()
        compose.onAllNodesWithTag("recovery_reapprove_0").assertCountEquals(0)
    }

    @Test
    fun managementAndPaginationRequireExplicitEnabledActions() {
        val calls = mutableListOf<String>()
        compose.setContent {
            Column {
                RecoveryRefreshPanel(
                    busy = false,
                    refreshLabel = "Refresh records",
                    onRefresh = { calls += "refresh" },
                )
                RecoveryRetentionPanel(
                    busy = false,
                    label = "Clear expired payloads",
                    onExpire = { calls += "expire" },
                )
                RecoveryNavigationPanel(
                    state = RecoveryNavigationUiState(previousEnabled = true),
                    previousLabel = "Previous records",
                    moreLabel = "More records",
                    onPrevious = { calls += "previous" },
                    onMore = { calls += "more" },
                )
            }
        }

        compose.onNodeWithTag("recovery_refresh_action").performClick()
        compose.onNodeWithTag("recovery_retention_action").performClick()
        compose.onNodeWithTag("recovery_previous_action").performClick()
        compose.onNodeWithTag("recovery_more_action").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf("refresh", "expire", "previous"), calls) }
    }

    @Test
    fun busyManagementAndPaginationActionsAreDisabled() {
        compose.setContent {
            Column {
                RecoveryRefreshPanel(busy = true, refreshLabel = "Refresh records", onRefresh = {})
                RecoveryRetentionPanel(busy = true, label = "Clear expired payloads", onExpire = {})
                RecoveryNavigationPanel(
                    state =
                        RecoveryNavigationUiState(
                            busy = true,
                            previousEnabled = true,
                            moreEnabled = true,
                        ),
                    previousLabel = "Previous records",
                    moreLabel = "More records",
                    onPrevious = {},
                    onMore = {},
                )
            }
        }

        compose.onNodeWithTag("recovery_refresh_action").assertIsNotEnabled()
        compose.onNodeWithTag("recovery_retention_action").assertIsNotEnabled()
        compose.onNodeWithTag("recovery_previous_action").assertIsNotEnabled()
        compose.onNodeWithTag("recovery_more_action").assertIsNotEnabled()
    }
}
