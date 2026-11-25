package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.markobl.calllogsync.R

/** Explicit manual action; any enabled automatic policy is controlled independently. */
class CalendarSliceActivity : ComponentActivity() {
    private val model by
        viewModels<ManualLoggingViewModel> {
            viewModelFactory {
                initializer {
                    ManualLoggingViewModel(AndroidManualLoggingDataSource(applicationContext))
                }
            }
        }

    private val calendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            model.refresh(calendarAccess())
        }
    private val callPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            model.callPermissionResult(it)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackContent(R.string.calltrack_title, showUp = false) {
            val state by model.uiState.collectAsStateWithLifecycle()
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
                onRequestAccess = ::requestCalendarAccess,
                onSelect = ::selectDestination,
                onLogLatest = ::logLatest,
                onOpenAutomatic = {
                    startActivity(
                        Intent(this@CalendarSliceActivity, AutomaticSettingsActivity::class.java),
                    )
                },
                hasCalendarAccess = access,
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        model.resume(calendarAccess())
    }

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCalendarAccess() {
        if (calendarAccess()) {
            model.refresh(calendarAccess = true)
        } else {
            calendarPermissions.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
    }

    private fun selectDestination(token: String) {
        if (calendarAccess()) model.select(token) else model.refresh(calendarAccess = false)
    }

    private fun logLatest() {
        if (!model.canLog()) return
        when {
            !calendarAccess() -> model.refresh(calendarAccess = false)

            checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED -> model.logLatest()

            else -> callPermission.launch(Manifest.permission.READ_CALL_LOG)
        }
    }
}
