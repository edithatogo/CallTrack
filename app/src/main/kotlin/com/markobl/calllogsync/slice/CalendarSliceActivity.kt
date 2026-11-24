package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.markobl.calllogsync.R
import java.util.HashMap
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

/** Explicit manual action; any enabled automatic policy is controlled independently. */
class CalendarSliceActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var panelState by
        mutableStateOf(ManualLogUiState(logLabel = "", status = "", actionEnabled = false))
    private var destinationState by mutableStateOf(CalendarDestinationUiState())
    private val destinationTargets = HashMap<String, Long>()
    private var selected = -1L
    private var busy = false
    private var refreshing = false
    private var refreshGeneration = 0

    private val calendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (calendarAccess()) refresh() else setStatus(R.string.slice_permission_needed)
        }
    private val callPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            setStatus(R.string.slice_tap_again)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        panelState = panelState.copy(logLabel = getString(R.string.slice_log_latest))
        setCallTrackContent {
            ManualCalendarScreen(
                intro = getString(R.string.slice_intro),
                calendarState = destinationState,
                logState = panelState,
                chooseLabel = getString(R.string.slice_choose),
                accessLabel = getString(R.string.slice_calendar_access),
                unavailableLabel = getString(R.string.slice_no_calendars),
                automaticLabel = getString(R.string.auto_open),
                onRequestAccess = ::requestCalendarAccess,
                onSelect = ::selectDestination,
                onLogLatest = ::logLatest,
                onOpenAutomatic = {
                    startActivity(
                        Intent(
                            this@CalendarSliceActivity,
                            AutomaticSettingsActivity::class.java,
                        ),
                    )
                },
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        executor.execute {
            try {
                AutomaticScheduling.reconcile(applicationContext)
            } catch (ignored: RuntimeException) {
                /* Settings exposes unavailable policy. */
            }
            try {
                SqliteLedger(applicationContext).use {
                    it.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                }
            } catch (ignored: RuntimeException) {
                /* Recovery exposes storage errors. */
            }
        }
        if (calendarAccess()) {
            refresh()
        } else {
            selected = -1
            destinationState = destinationState.copy(selectedToken = null, enabled = false)
            setStatus(R.string.slice_permission_needed)
            updateButton()
        }
    }

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCalendarAccess() {
        if (calendarAccess()) {
            refresh()
        } else {
            calendarPermissions.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
    }

    private fun selectDestination(token: String) {
        val target = destinationTargets[token] ?: return
        if (!calendarAccess() || busy || refreshing) return
        selected = target
        getSharedPreferences("slice", MODE_PRIVATE).edit().putLong("calendar", selected).apply()
        destinationState = destinationState.copy(selectedToken = token)
        updateButton()
    }

    private fun updateButton() {
        val enabled = !busy && !refreshing && calendarAccess()
        panelState = panelState.copy(actionEnabled = enabled && selected >= 0)
        destinationState = destinationState.copy(enabled = enabled)
    }

    private fun setStatus(message: Int) {
        panelState = panelState.copy(status = getString(message))
    }

    private fun logLatest() {
        if (selected < 0 || busy) return
        if (!calendarAccess()) {
            setStatus(R.string.slice_permission_needed)
            updateButton()
            return
        }
        if (
            checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            callPermission.launch(Manifest.permission.READ_CALL_LOG)
            return
        }
        val destination = selected
        busy = true
        updateButton()
        setStatus(R.string.slice_working)
        executor.execute {
            val outcome =
                try {
                    SqliteLedger(applicationContext).use { ledger ->
                        ManualDelivery(
                                AndroidCallLogSource(applicationContext),
                                DurableDelivery(
                                    ledger.outbox(),
                                    AndroidCalendarSink(applicationContext),
                                ),
                                TimeZone.getDefault().id,
                            )
                            .run(destination)
                    }
                } catch (ignored: RuntimeException) {
                    SliceEngine.Result.FAILED
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                setStatus(message(outcome))
                updateButton()
            }
        }
    }

    private fun refresh() {
        val saved = getSharedPreferences("slice", MODE_PRIVATE).getLong("calendar", -1)
        val generation = ++refreshGeneration
        refreshing = true
        updateButton()
        executor.execute {
            try {
                val loaded = AndroidCalendarSink(applicationContext).choices()
                runOnUiThread {
                    if (isDestroyed || generation != refreshGeneration) return@runOnUiThread
                    refreshing = false
                    destinationTargets.clear()
                    val options = ArrayList<CalendarDestinationOption>()
                    var selectedToken: String? = null
                    for (choice in loaded) {
                        val token = UUID.randomUUID().toString()
                        destinationTargets[token] = choice.id
                        options.add(
                            CalendarDestinationOption(
                                token,
                                choice.name?.takeIf(String::isNotBlank)
                                    ?: getString(R.string.slice_unnamed_calendar),
                            ),
                        )
                        if (choice.id == saved) selectedToken = token
                    }
                    selected = selectedToken?.let(destinationTargets::get) ?: -1
                    destinationState =
                        CalendarDestinationUiState(
                            options = options,
                            selectedToken = selectedToken,
                            enabled = false,
                        )
                    if (selected < 0) {
                        getSharedPreferences("slice", MODE_PRIVATE)
                            .edit()
                            .putLong("calendar", -1)
                            .apply()
                    }
                    setStatus(
                        if (loaded.isEmpty()) R.string.slice_no_calendars else R.string.slice_ready,
                    )
                    updateButton()
                }
            } catch (ignored: RuntimeException) {
                runOnUiThread {
                    if (!isDestroyed && generation == refreshGeneration) {
                        refreshing = false
                        selected = -1
                        destinationTargets.clear()
                        destinationState = CalendarDestinationUiState()
                        setStatus(R.string.slice_calendar_unavailable)
                        updateButton()
                    }
                }
            }
        }
    }

    private fun message(result: SliceEngine.Result): Int =
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

    public override fun onDestroy() {
        ++refreshGeneration
        executor.shutdown()
        super.onDestroy()
    }
}
