package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

internal data class RecoveryCardUiState(
    val token: Long,
    val detail: String,
    val checkEnabled: Boolean,
    val suppressEnabled: Boolean,
    val reapproveVisible: Boolean,
    val openEventVisible: Boolean,
)

internal data class RecoveryNavigationUiState(
    val busy: Boolean = false,
    val previousEnabled: Boolean = false,
    val moreEnabled: Boolean = false,
)

@Composable
internal fun RecoveryRefreshPanel(busy: Boolean, refreshLabel: String, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ComposeActionButton(
            label = refreshLabel,
            tag = "recovery_refresh_action",
            enabled = !busy,
            onClick = onRefresh,
        )
    }
}

@Composable
internal fun RecoveryRetentionPanel(busy: Boolean, label: String, onExpire: () -> Unit) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ComposeActionButton(
            label = label,
            tag = "recovery_retention_action",
            enabled = !busy,
            onClick = onExpire,
        )
    }
}

@Composable
internal fun RecoveryCardsPanel(
    cards: List<RecoveryCardUiState>,
    busy: Boolean,
    checkLabel: String,
    suppressLabel: String,
    reapproveLabel: String,
    openEventLabel: String,
    onCheck: (Long) -> Unit,
    onSuppress: (Long) -> Unit,
    onReapprove: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cards.forEach { card ->
            Column(
                modifier = Modifier.fillMaxWidth().testTag("recovery_card_${card.token}"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(
                    text = card.detail,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("recovery_detail_${card.token}"),
                )
                ComposeActionButton(
                    label = checkLabel,
                    tag = "recovery_check_${card.token}",
                    enabled = !busy && card.checkEnabled,
                    onClick = { onCheck(card.token) },
                )
                ComposeActionButton(
                    label = suppressLabel,
                    tag = "recovery_suppress_${card.token}",
                    enabled = !busy && card.suppressEnabled,
                    onClick = { onSuppress(card.token) },
                )
                if (card.reapproveVisible) {
                    ComposeActionButton(
                        label = reapproveLabel,
                        tag = "recovery_reapprove_${card.token}",
                        enabled = !busy,
                        onClick = { onReapprove(card.token) },
                    )
                }
                if (card.openEventVisible) {
                    ComposeActionButton(
                        label = openEventLabel,
                        tag = "recovery_open_event_${card.token}",
                        enabled = true,
                        onClick = { onOpenEvent(card.token) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecoveryNavigationPanel(
    state: RecoveryNavigationUiState,
    previousLabel: String,
    moreLabel: String,
    onPrevious: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                testTagsAsResourceId = true
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f)) {
            ComposeActionButton(
                previousLabel,
                "recovery_previous_action",
                enabled = !state.busy && state.previousEnabled,
                onClick = onPrevious,
            )
        }
        Box(Modifier.weight(1f)) {
            ComposeActionButton(
                moreLabel,
                "recovery_more_action",
                enabled = !state.busy && state.moreEnabled,
                onClick = onMore,
            )
        }
    }
}
