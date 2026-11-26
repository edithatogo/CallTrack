package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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

/** The normal app entry for manual logging and the other call management screens. */
@AndroidEntryPoint
class CalendarSliceActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Manual)
    }
}

@Composable
internal fun ManualLoggingScreen(
    activity: ComponentActivity,
    onOpenAutomatic: () -> Unit,
    model: ManualLoggingViewModel = hiltViewModel(),
) {
    val calendarPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            model.refresh(activity.hasCalendarAccess())
        }
    val callPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
            model::callPermissionResult,
        )
    LifecycleResumeEffect(model.uiState) {
        model.resume(activity.hasCalendarAccess())
        onPauseOrDispose {}
    }
    val state by model.uiState.collectAsStateWithLifecycle()
    ManualLoggingForm(
        state = state,
        onRequestAccess = {
            if (activity.hasCalendarAccess()) {
                model.refresh(calendarAccess = true)
            } else {
                calendarPermissions.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                )
            }
        },
        onSelect = { token ->
            if (activity.hasCalendarAccess()) {
                model.select(token)
            } else {
                model.refresh(calendarAccess = false)
            }
        },
        onLogLatest = {
            activity.logLatest(model) { callPermission.launch(Manifest.permission.READ_CALL_LOG) }
        },
        onOpenAutomatic = onOpenAutomatic,
    )
}

@Composable
private fun ManualLoggingForm(
    state: ManualLoggingUiState,
    onRequestAccess: () -> Unit,
    onSelect: (String) -> Unit,
    onLogLatest: () -> Unit,
    onOpenAutomatic: () -> Unit,
) {
    val access = state is ManualLoggingUiState.Available
    val destinations = state.destinations
    val accessAction =
        if (access) R.string.slice_calendar_refresh else R.string.slice_calendar_access
    ManualCalendarScreen(
        intro = stringResource(R.string.slice_intro),
        calendarState = destinations,
        logState =
            ManualLogUiState(
                logLabel = stringResource(R.string.slice_log_latest),
                status = if (state.status == 0) "" else stringResource(state.status),
                actionEnabled = destinations.enabled && destinations.selectedToken != null,
            ),
        chooseLabel = stringResource(R.string.slice_choose),
        accessLabel = stringResource(accessAction),
        unavailableLabel = null,
        automaticLabel = stringResource(R.string.auto_open),
        onRequestAccess = onRequestAccess,
        onSelect = onSelect,
        onLogLatest = onLogLatest,
        onOpenAutomatic = onOpenAutomatic,
        hasCalendarAccess = access,
    )
}

private fun Context.hasCalendarAccess(): Boolean =
    checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

private fun Context.logLatest(model: ManualLoggingViewModel, requestCallPermission: () -> Unit) {
    if (!model.canLog()) return
    when {
        !hasCalendarAccess() -> model.refresh(calendarAccess = false)

        checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED -> model.logLatest()

        else -> requestCallPermission()
    }
}
