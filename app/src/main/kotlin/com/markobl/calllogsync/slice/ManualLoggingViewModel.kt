// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markobl.calllogsync.R
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
internal sealed interface ManualLoggingUiState {
    val destinations: CalendarDestinationUiState

    @get:StringRes val status: Int

    data object PermissionRequired : ManualLoggingUiState {
        override val destinations = CalendarDestinationUiState()
        override val status = R.string.slice_permission_needed
    }

    data class Available(
        override val destinations: CalendarDestinationUiState = CalendarDestinationUiState(),
        @StringRes override val status: Int = 0,
    ) : ManualLoggingUiState
}

internal class ManualLoggingViewModel(private val data: ManualLoggingDataSource) : ViewModel() {
    private val state =
        MutableStateFlow<ManualLoggingUiState>(ManualLoggingUiState.PermissionRequired)
    val uiState =
        state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS),
            state.value,
        )

    private val operations = Mutex()
    private var targets = emptyMap<String, Long>()
    private var reload: Job? = null
    private var actionInProgress = false
    private var reloadAfterAction = false

    @MainThread
    fun resume(calendarAccess: Boolean) {
        viewModelScope.launch { providerResult(Unit) { data.maintain() } }
        refresh(calendarAccess)
    }

    @MainThread
    fun refresh(calendarAccess: Boolean) {
        when {
            !calendarAccess -> {
                reload?.cancel()
                targets = emptyMap()
                reloadAfterAction = false
                state.value = ManualLoggingUiState.PermissionRequired
            }

            actionInProgress ->
                reloadAfterAction = state.value is ManualLoggingUiState.PermissionRequired

            reload?.isActive != true -> loadCalendars()
        }
    }

    @MainThread
    fun select(token: String) {
        val calendar = targets[token] ?: return
        perform { before ->
            if (
                providerResult(false) {
                    data.select(calendar)
                    true
                }
            ) {
                before.copy(
                    destinations = before.destinations.copy(selectedToken = token),
                    status = R.string.slice_calendar_selected,
                )
            } else {
                before.copy(status = R.string.slice_calendar_unavailable)
            }
        }
    }

    @MainThread
    fun canLog(): Boolean =
        state.value.destinations.let { it.enabled && it.selectedToken in targets }

    @MainThread
    fun callPermissionResult(granted: Boolean) {
        val available = state.value as? ManualLoggingUiState.Available ?: return
        state.value =
            available.copy(
                status =
                    if (granted) R.string.slice_tap_again else R.string.slice_permission_needed,
            )
    }

    @MainThread
    fun logLatest() {
        val calendar = targets[state.value.destinations.selectedToken] ?: return
        perform(workingStatus = R.string.slice_working) { before ->
            val result = providerResult(SliceEngine.Result.FAILED) { data.logLatest(calendar) }
            before.copy(status = manualLoggingMessage(result))
        }
    }

    private fun loadCalendars() {
        val available =
            (state.value as? ManualLoggingUiState.Available) ?: ManualLoggingUiState.Available()
        state.value = available.copy(destinations = available.destinations.copy(enabled = false))
        reload = viewModelScope.launch {
            val snapshot = providerResult(null) { data.calendars() }
            state.value = presentCalendars(snapshot, previousStatus = state.value.status)
        }
    }

    private fun presentCalendars(
        calendars: ManualCalendars?,
        @StringRes previousStatus: Int,
    ): ManualLoggingUiState.Available {
        val byToken =
            calendars?.labels.orEmpty().entries.associateBy { UUID.randomUUID().toString() }
        targets = byToken.mapValues { it.value.key }
        val options = byToken.map { (token, calendar) ->
            CalendarDestinationOption(token, calendar.value)
        }
        val selected = targets.entries.firstOrNull { it.value == calendars?.saved }?.key
        val status =
            when {
                calendars == null -> R.string.slice_calendar_unavailable
                options.isEmpty() -> R.string.slice_no_calendars
                previousStatus == R.string.slice_tap_again -> previousStatus
                selected != null -> R.string.slice_calendar_selected
                else -> R.string.slice_ready
            }
        return ManualLoggingUiState.Available(
            destinations =
                CalendarDestinationUiState(options, selected, enabled = calendars != null),
            status = status,
        )
    }

    private fun perform(
        @StringRes workingStatus: Int? = null,
        action: suspend (ManualLoggingUiState.Available) -> ManualLoggingUiState.Available,
    ) {
        val before = state.value as? ManualLoggingUiState.Available ?: return
        if (!before.destinations.enabled) return
        actionInProgress = true
        state.value =
            before.copy(
                destinations = before.destinations.copy(enabled = false),
                status = workingStatus ?: before.status,
            )
        viewModelScope.launch {
            try {
                val outcome = action(before)
                if (state.value is ManualLoggingUiState.Available) state.value = outcome
            } finally {
                actionInProgress = false
                if (reloadAfterAction) {
                    reloadAfterAction = false
                    refresh(calendarAccess = true)
                }
            }
        }
    }

    private suspend fun <T> providerResult(failure: T, operation: suspend () -> T): T =
        operations.withLock {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                failure
            } catch (_: RuntimeException) {
                failure
            }
        }
}

private const val SUBSCRIPTION_GRACE_MILLIS = 5_000L

@StringRes
private fun manualLoggingMessage(result: SliceEngine.Result): Int =
    when (result) {
        SliceEngine.Result.SAVED -> R.string.slice_saved
        SliceEngine.Result.ALREADY_LOGGED -> R.string.slice_already
        SliceEngine.Result.RECOVERED -> R.string.slice_recovered
        SliceEngine.Result.NO_CALL -> R.string.slice_no_call
        SliceEngine.Result.PERMISSION_REQUIRED -> R.string.slice_permission_needed
        SliceEngine.Result.DESTINATION_UNAVAILABLE -> R.string.slice_calendar_unavailable
        SliceEngine.Result.REVIEW_REQUIRED -> R.string.slice_review
        SliceEngine.Result.INVALID_CALL -> R.string.slice_invalid
        else -> R.string.slice_failed
    }
