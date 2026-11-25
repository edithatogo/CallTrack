// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint

private val recoveryReadPermissions = arrayOf(Manifest.permission.READ_CALENDAR)
private val reapprovalPermissions =
    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

/** Existing-effect checks remain separate from explicitly confirmed never-attempted insertion. */
@AndroidEntryPoint
class ReceiptRecoveryActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Recovery)
    }
}

@Composable
internal fun ReceiptRecoveryScreen(activity: ComponentActivity) {
    val model = hiltViewModel<ReceiptRecoveryViewModel>()
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            model.permissionsChanged()
        }
    LifecycleResumeEffect(model) {
        model.setResumed(true)
        onPauseOrDispose { model.setResumed(false) }
    }
    val state by model.uiState.collectAsStateWithLifecycle()
    val page = state.page

    RecoveryAccessPanel(
        label = stringResource(R.string.recovery_access),
        hasCalendarAccess = page.hasCalendarReadAccess,
        onRequestAccess = { permissions.launch(recoveryReadPermissions) },
    )
    RecoveryRefreshPanel(
        busy = page.navigation.busy,
        refreshLabel = stringResource(R.string.recovery_refresh),
        onRefresh = model::refresh,
    )
    SectionHeading(stringResource(R.string.recovery_records_heading))
    RecoveryStatusPanel(page.feedback)
    RecoveryEmptyPanel(visible = page.empty, message = stringResource(R.string.recovery_empty))
    RecoveryCardsPanel(
        cards = page.cards,
        busy = page.navigation.busy,
        checkLabel = stringResource(R.string.recovery_check),
        suppressLabel = stringResource(R.string.recovery_suppress),
        reapproveLabel = stringResource(R.string.reapproval_open),
        openEventLabel = stringResource(R.string.recovery_open_event),
        onCheck = model::check,
        onSuppress = model::requestSuppression,
        onReapprove = { token ->
            if (model.requestReapproval(token)) permissions.launch(reapprovalPermissions)
        },
        onOpenEvent = { token ->
            model.eventId(token)?.let { activity.openRecoveryEvent(it, model::eventUnavailable) }
        },
    )
    RecoveryNavigationPanel(
        state = page.navigation,
        previousLabel = stringResource(R.string.recovery_previous),
        moreLabel = stringResource(R.string.recovery_more),
        onPrevious = model::previous,
        onMore = model::more,
    )
    RecoveryRetention(page, model::expireOldPayloads)

    (state as? ReceiptRecoveryUiState.Reviewing)?.let { reviewing ->
        val review = reviewing.review
        ConfirmationDialog(
            title = stringResource(review.title),
            message = review.message,
            actionLabel = stringResource(review.actionLabel),
            onConfirm = { model.confirmReview(review) },
            onDismiss = { model.dismissReview(review) },
        )
    }
}

@Composable
private fun RecoveryRetention(page: RecoveryPageUiState, onExpire: () -> Unit) {
    RecoveryCopyPanel(
        text = stringResource(R.string.recovery_intro),
        tag = "recovery_intro_text",
    )
    SectionHeading(stringResource(R.string.recovery_retention_heading))
    RecoveryCopyPanel(
        text = stringResource(R.string.retention_policy),
        tag = "recovery_retention_copy_text",
    )
    RecoveryRetentionPanel(
        busy = page.navigation.busy,
        label = stringResource(R.string.retention_run),
        onExpire = onExpire,
    )
    RecoveryStatusPanel(page.retentionFeedback, tag = "recovery_retention_feedback")
}

private fun ComponentActivity.openRecoveryEvent(eventId: Long, onUnavailable: () -> Unit) {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            ),
        )
    } catch (_: RuntimeException) {
        onUnavailable()
    }
}
