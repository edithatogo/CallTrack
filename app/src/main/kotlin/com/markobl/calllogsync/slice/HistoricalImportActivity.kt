// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint

/** Foreground bounded runs; opening/resuming never confirms historical delivery. */
@AndroidEntryPoint
class HistoricalImportActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Import)
    }
}

@Composable
internal fun HistoricalImportScreen(
    activity: ComponentActivity,
    model: HistoricalImportViewModel = hiltViewModel(),
) {
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            model.permissionsChanged()
        }
    LifecycleResumeEffect(model.uiState) {
        model.setResumed(true)
        onPauseOrDispose { model.setResumed(false) }
    }
    val state by model.uiState.collectAsStateWithLifecycle()
    val form = state.form
    HistoricalImportForm(form, model::selectOption, model::setFilters) {
        val requested =
            arrayOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            )
        if (
            requested.all { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        ) {
            model.permissionsChanged()
        } else {
            permissions.launch(requested)
        }
    }
    HistoricalImportActions(
        form = form,
        onCreate = model::startPreview,
        onSetRunning = model::setRunning,
        onConfirm = model::requestConfirmation,
        onCancel = model::requestCancellation,
    )
    HistoricalImportHistoryPanel(
        state = form.history,
        title = stringResource(R.string.import_history),
        emptyMessage = stringResource(R.string.import_empty_history),
        onOpen = model::openHistorySession,
    )
    HistoricalImportPaginationPanel(
        state = form.actions,
        newerLabel = stringResource(R.string.import_newer),
        olderLabel = stringResource(R.string.import_older),
        onNewer = { model.changeHistoryPage(newer = true) },
        onOlder = { model.changeHistoryPage(newer = false) },
    )
    val review = (state as? HistoricalImportUiState.Reviewing)?.review
    if (review != null) {
        val label = stringResource(review.actionLabel)
        ConfirmationDialog(
            title = label,
            message = review.message,
            actionLabel = label,
            onConfirm = { model.finishReview(review, confirmed = true) },
            onDismiss = { model.finishReview(review, confirmed = false) },
        )
    }
}

@Composable
private fun HistoricalImportForm(
    form: HistoricalImportFormUiState,
    onSelect: (String) -> Unit,
    onFiltersChange: (HistoricalImportFiltersUiState) -> Unit,
    onRequestAccess: () -> Unit,
) {
    HistoricalImportIntroPanel(stringResource(R.string.import_intro, form.zone))
    HistoricalImportAccessPanel(
        label = stringResource(R.string.auto_access),
        hasRequiredAccess = form.actions.hasRequiredAccess,
        onRequestAccess = onRequestAccess,
    )
    if (form.showsSourcePicker) {
        SectionHeading(stringResource(R.string.auto_source_heading))
        CalendarDestinationPicker(
            state = form.sources,
            tagPrefix = "import_source",
            chooseLabel = stringResource(R.string.import_choose_source),
            onSelect = onSelect,
        )
    }
    CalendarDestinationPicker(
        state = form.destination,
        tagPrefix = "import_calendar",
        chooseLabel = stringResource(R.string.slice_choose),
        unavailableLabel = form.destinationMessage?.let { stringResource(it) },
        onSelect = onSelect,
    )
    HistoricalImportFiltersPanel(
        state = form.filters,
        fromLabel = stringResource(R.string.import_from),
        toLabel = stringResource(R.string.import_to),
        incomingLabel = stringResource(R.string.auto_incoming),
        outgoingLabel = stringResource(R.string.auto_outgoing),
        missedLabel = stringResource(R.string.auto_missed),
        onStateChange = onFiltersChange,
    )
}

@Composable
private fun HistoricalImportActions(
    form: HistoricalImportFormUiState,
    onCreate: () -> Unit,
    onSetRunning: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val focus = LocalFocusManager.current
    HistoricalImportCreateActionPanel(
        state = form.actions,
        label = stringResource(R.string.import_create),
        onCreate = {
            focus.clearFocus()
            onCreate()
        },
    )
    HistoricalImportSummaryText(form.summary)
    if (form.actions.sessionSelected) {
        HistoricalImportRunActionsPanel(
            state = form.actions,
            continueLabel = stringResource(R.string.import_continue),
            confirmLabel = stringResource(R.string.import_confirm),
            stopLabel = stringResource(R.string.import_stop),
            cancelLabel = stringResource(R.string.import_cancel),
            onContinue = { onSetRunning(true) },
            onConfirm = onConfirm,
            onStop = { onSetRunning(false) },
            onCancel = onCancel,
        )
    }
    HistoricalImportFeedbackText(form.feedback)
}
