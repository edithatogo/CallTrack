package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint

private val automaticLoggingPermissions =
    arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
    )

internal data class AutomaticStatusUiState(
    val state: String,
    val counts: String,
    val feedback: String,
    val lastRun: String,
)

@Composable
internal fun AutomaticStatusLine(text: String, tag: String, announceChanges: Boolean = false) {
    if (text.isEmpty()) return
    var modifier = Modifier.testTag(tag)
    if (announceChanges) modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    Text(
        text,
        modifier = modifier,
        style =
            if (announceChanges) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodySmall
            },
    )
}

@Composable
internal fun AutomaticStatusPanel(state: AutomaticStatusUiState, showDetails: Boolean = true) {
    StatusCard {
        AutomaticStatusLine(state.state, "auto_state", announceChanges = true)
        if (showDetails) AutomaticStatusDetails(state)
    }
}

@Composable
private fun ColumnScope.AutomaticStatusDetails(state: AutomaticStatusUiState) {
    AutomaticStatusLine(state.counts, "auto_counts")
    AutomaticStatusLine(state.feedback, "auto_feedback")
    AutomaticStatusLine(state.lastRun, "auto_last")
}

/** Explicit activation controls; reopening can resume enabled policy but never enable it. */
@AndroidEntryPoint
class AutomaticSettingsActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Automatic)
    }
}

@Composable
internal fun ColumnScope.AutomaticSettingsScreen(
    activity: ComponentActivity,
    onOpenImports: () -> Unit,
    onOpenRecovery: () -> Unit,
    model: AutomaticSettingsViewModel = hiltViewModel(),
) {
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            model.permissionsChanged()
        }
    LifecycleResumeEffect(model.uiState) {
        model.setResumed(true)
        onPauseOrDispose { model.setResumed(false) }
    }
    val screen by model.uiState.collectAsStateWithLifecycle()
    val form = screen.form
    AutomaticStatusPanel(form.status, showDetails = false)
    AutomaticStatusActions(
        state = form.actions,
        pauseLabel = stringResource(R.string.auto_pause),
        refreshLabel = stringResource(R.string.auto_refresh),
        onPause = model::pauseLogging,
        onRefresh = { model.refresh() },
    )
    AutomaticSettingsIntroPanel(text = stringResource(R.string.auto_intro))
    AutomaticSettingsAccessPanel(
        label = stringResource(R.string.auto_access),
        hasRequiredAccess = form.hasRequiredAccess,
        onRequestAccess = { permissions.launch(automaticLoggingPermissions) },
    )
    AutomaticSettingsNavigationPanel(
        importLabel = stringResource(R.string.import_open),
        recoveryLabel = stringResource(R.string.recovery_open),
        onOpenImports = onOpenImports,
        onOpenRecovery = onOpenRecovery,
    )
    HorizontalDivider()
    AutomaticSettingsForm(
        state = form,
        onSelectSource = model::selectSource,
        onOpenSystemHistory = {
            activity.openAndroidHistorySettings(model::systemHistoryUnavailable)
        },
        onToggleDestination = model::toggleDestination,
        onChangeCallTypes = model::setCallTypes,
    )
    AutomaticApplyButton(
        state = form.actions,
        label = stringResource(R.string.auto_enable),
        onApply = model::requestReview,
    )
    AutomaticStatusDetails(form.status)
    (screen as? AutomaticSettingsUiState.Reviewing)?.let { reviewing ->
        val applyLabel = stringResource(R.string.auto_enable)
        ConfirmationDialog(
            title = applyLabel,
            message = reviewing.review.message,
            actionLabel = applyLabel,
            onConfirm = { model.confirmReview(reviewing.review) },
            onDismiss = model::dismissReview,
        )
    }
}

private fun ComponentActivity.openAndroidHistorySettings(onUnavailable: () -> Unit) {
    try {
        startActivity(Intent(TelecomManager.ACTION_CONFIGURE_CALL_LOG_INTEGRATION))
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    }
}

@Composable
private fun ColumnScope.AutomaticSettingsForm(
    state: AutomaticSettingsFormUiState,
    onSelectSource: (String) -> Unit,
    onOpenSystemHistory: () -> Unit,
    onToggleDestination: (String) -> Unit,
    onChangeCallTypes: (AutomaticCallTypesUiState) -> Unit,
) {
    val hasAppSources = state.sources.options.size > 1
    if (hasAppSources) {
        SectionHeading(stringResource(R.string.auto_source_heading))
        CalendarDestinationPicker(
            state = state.sources,
            tagPrefix = "auto_source",
            chooseLabel = stringResource(R.string.auto_source_phone),
            onSelect = onSelectSource,
        )
        ActionButton(
            label = stringResource(R.string.auto_system_history),
            tag = "auto_system_history",
            enabled = state.sources.enabled,
            onClick = onOpenSystemHistory,
            style = ButtonStyle.Text,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val title =
            if (hasAppSources) {
                R.string.auto_calendars_heading
            } else {
                R.string.auto_phone_calendars_heading
            }
        SectionHeading(stringResource(title))
        CalendarDestinationPicker(
            state = state.destinations,
            tagPrefix = "auto_calendar",
            chooseLabel = stringResource(R.string.auto_choose_calendars),
            onSelect = onToggleDestination,
        )
    }
    Column {
        SectionHeading(stringResource(R.string.auto_types_heading))
        AutomaticCallTypesPanel(
            state = state.callTypes,
            incomingLabel = stringResource(R.string.auto_incoming),
            outgoingLabel = stringResource(R.string.auto_outgoing),
            missedLabel = stringResource(R.string.auto_missed),
            enabled = state.callTypesEnabled,
            onStateChange = onChangeCallTypes,
        )
    }
}
