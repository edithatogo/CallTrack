package com.markobl.calllogsync.slice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

@Composable
internal fun RecoveryEmptyPanel(visible: Boolean, message: String) {
    Column(modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        if (visible) {
            StatusCard {
                Text(message, modifier = Modifier.testTag("recovery_empty_message"))
            }
        }
    }
}

@Composable
internal fun RecoveryCopyPanel(text: String, tag: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun RecoveryAccessPanel(
    label: String,
    hasCalendarAccess: Boolean = false,
    onRequestAccess: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ActionButton(
            label = label,
            tag = "recovery_access_action",
            enabled = true,
            onClick = onRequestAccess,
            style = if (hasCalendarAccess) ButtonStyle.Text else ButtonStyle.Filled,
        )
    }
}

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
        ActionButton(
            label = refreshLabel,
            tag = "recovery_refresh_action",
            enabled = !busy,
            onClick = onRefresh,
            style = ButtonStyle.Text,
        )
    }
}

@Composable
internal fun RecoveryRetentionPanel(busy: Boolean, label: String, onExpire: () -> Unit) {
    Column(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }) {
        ActionButton(
            label = label,
            tag = "recovery_retention_action",
            enabled = !busy,
            onClick = onExpire,
            style = ButtonStyle.Text,
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
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().testTag("recovery_card_${card.token}"),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = card.detail,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("recovery_detail_${card.token}"),
                    )
                    ActionButton(
                        label = checkLabel,
                        tag = "recovery_check_${card.token}",
                        enabled = !busy && card.checkEnabled,
                        onClick = { onCheck(card.token) },
                        style = ButtonStyle.Tonal,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            label = suppressLabel,
                            tag = "recovery_suppress_${card.token}",
                            enabled = !busy && card.suppressEnabled,
                            onClick = { onSuppress(card.token) },
                            style = ButtonStyle.Text,
                        )
                        if (card.reapproveVisible) {
                            ActionButton(
                                label = reapproveLabel,
                                tag = "recovery_reapprove_${card.token}",
                                enabled = !busy,
                                onClick = { onReapprove(card.token) },
                                style = ButtonStyle.Outlined,
                            )
                        }
                        if (card.openEventVisible) {
                            ActionButton(
                                label = openEventLabel,
                                tag = "recovery_open_event_${card.token}",
                                enabled = true,
                                onClick = { onOpenEvent(card.token) },
                                style = ButtonStyle.Text,
                            )
                        }
                    }
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
    if (!state.previousEnabled && !state.moreEnabled) return
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics {
                testTagsAsResourceId = true
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f)) {
            ActionButton(
                previousLabel,
                "recovery_previous_action",
                enabled = !state.busy && state.previousEnabled,
                onClick = onPrevious,
                style = ButtonStyle.Text,
            )
        }
        Box(Modifier.weight(1f)) {
            ActionButton(
                moreLabel,
                "recovery_more_action",
                enabled = !state.busy && state.moreEnabled,
                onClick = onMore,
                style = ButtonStyle.Text,
            )
        }
    }
}
