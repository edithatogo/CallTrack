package com.markobl.calllogsync.slice

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.markobl.calllogsync.R
import java.util.Collections
import java.util.TimeZone
import java.util.concurrent.Executors

internal data class AutomaticStatusUiState(
    val state: String,
    val counts: String,
    val feedback: String,
    val lastRun: String,
)

@androidx.compose.runtime.Composable
internal fun AutomaticStatusLine(text: String, tag: String, announceChanges: Boolean = false) {
    var modifier = Modifier.testTag(tag)
    if (announceChanges) modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    Text(text, modifier = modifier)
}

@androidx.compose.runtime.Composable
internal fun AutomaticStatusPanel(state: AutomaticStatusUiState) {
    Column {
        AutomaticStatusLine(state.state, "auto_state", announceChanges = true)
        AutomaticStatusLine(state.counts, "auto_counts")
        AutomaticStatusLine(state.feedback, "auto_feedback")
        AutomaticStatusLine(state.lastRun, "auto_last")
    }
}

/** Explicit activation controls; reopening can resume enabled policy but never enable it. */
class AutomaticSettingsActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var stateText by mutableStateOf("")
    private var countsText by mutableStateOf("")
    private var lastText by mutableStateOf("")
    private var feedbackText by mutableStateOf("")
    private var choices: List<AndroidCalendarSink.Choice> = Collections.emptyList()
    private var calendarLabels: Map<Long, String> = emptyMap()
    private var availableBindings: Map<Long, String> = emptyMap()
    private var appSources: List<AndroidCallSources.App> = emptyList()
    private var source = CallRecord.PHONE
    private var draft: Map<Long, Set<String>>? = null
    private var draftBindings: Map<Long, String> = emptyMap()
    private var busy = false
    private var active = false
    private var initialised = false
    private var reloadRequested = false
    private var reloadReconcile = false
    private var callTypesState by mutableStateOf(AutomaticCallTypesUiState())
    private var callTypesEnabled by mutableStateOf(true)
    private var destinationUiState by mutableStateOf(CalendarDestinationUiState())
    private var sourceUiState by mutableStateOf(CalendarDestinationUiState())
    private var actionsUiState by mutableStateOf(AutomaticSettingsActionsUiState())
    private var restoredCallTypes = false
    private var destinationOptionTokens: List<String> = Collections.emptyList()

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            initialised = false
            load(false)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        source = savedInstanceState?.getString(STATE_SOURCE) ?: CallRecord.PHONE
        draft =
            savedInstanceState?.getBundle(STATE_DESTINATIONS)?.let { routes ->
                routes.keySet().associate {
                    it.toLong() to routes.getStringArrayList(it).orEmpty().toSet()
                }
            }
        draftBindings =
            savedInstanceState
                ?.getBundle(STATE_BINDINGS)
                ?.let { stored ->
                    stored.keySet().associate { it.toLong() to checkNotNull(stored.getString(it)) }
                }
                .orEmpty()
        callTypesState =
            AutomaticCallTypesUiState(
                incoming = savedInstanceState?.getBoolean(STATE_INCOMING, true) ?: true,
                outgoing = savedInstanceState?.getBoolean(STATE_OUTGOING, true) ?: true,
                missed = savedInstanceState?.getBoolean(STATE_MISSED, true) ?: true,
            )
        restoredCallTypes = savedInstanceState != null
        setCallTrackContent {
            SettingsForm()
            SettingsActions()
        }
        buttons()
    }

    @Composable
    private fun SettingsForm() {
        AutomaticSettingsIntroPanel(text = getString(R.string.auto_intro))
        AutomaticSettingsAccessPanel(
            label = getString(R.string.auto_access),
            onRequestAccess = {
                permissions.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_STATE,
                    ),
                )
            },
        )
        if (sourceUiState.options.size > 1) {
            CalendarDestinationPicker(
                state = sourceUiState,
                tagPrefix = "auto_source",
                chooseLabel = getString(R.string.auto_source_phone),
                onSelect = { selected ->
                    source = selected
                    updateRoutingForm()
                },
            )
            ActionButton(
                label = getString(R.string.auto_system_history),
                tag = "auto_system_history",
                enabled = sourceUiState.enabled,
                onClick = ::openAndroidHistorySettings,
            )
        }
        CalendarDestinationPicker(
            state = destinationUiState,
            tagPrefix = "auto_calendar",
            chooseLabel = getString(R.string.slice_choose),
            onSelect = ::toggleDestination,
        )
        AutomaticCallTypesPanel(
            state = callTypesState,
            incomingLabel = getString(R.string.auto_incoming),
            outgoingLabel = getString(R.string.auto_outgoing),
            missedLabel = getString(R.string.auto_missed),
            enabled = callTypesEnabled,
            onStateChange = {
                callTypesState = it
                buttons()
            },
        )
    }

    @Composable
    private fun SettingsActions() {
        AutomaticSettingsActionsPanel(
            state = actionsUiState,
            enableLabel = getString(R.string.auto_enable),
            pauseLabel = getString(R.string.auto_pause),
            refreshLabel = getString(R.string.auto_refresh),
            onEnable = ::confirmEnable,
            onPause = { change { AutomaticScheduling.pause(applicationContext) } },
            onRefresh = { load(false) },
        )
        AutomaticSettingsNavigationPanel(
            importLabel = getString(R.string.import_open),
            recoveryLabel = getString(R.string.recovery_open),
            onOpenImports = {
                startActivity(
                    Intent(
                        this@AutomaticSettingsActivity,
                        HistoricalImportActivity::class.java,
                    ),
                )
            },
            onOpenRecovery = {
                startActivity(
                    Intent(
                        this@AutomaticSettingsActivity,
                        ReceiptRecoveryActivity::class.java,
                    ),
                )
            },
        )
        AutomaticStatusPanel(
            AutomaticStatusUiState(stateText, countsText, feedbackText, lastText),
        )
    }

    public override fun onResume() {
        super.onResume()
        load(true)
    }

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun callAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        actionsUiState =
            AutomaticSettingsActionsUiState(
                enableEnabled =
                    !busy &&
                        !draft.isNullOrEmpty() &&
                        calendarAccess() &&
                        callAccess() &&
                        (callTypesState.incoming ||
                            callTypesState.outgoing ||
                            callTypesState.missed),
                pauseEnabled = !busy,
                refreshEnabled = true,
            )
        destinationUiState = destinationUiState.copy(enabled = !busy && calendarAccess())
        sourceUiState = sourceUiState.copy(enabled = !busy)
        callTypesEnabled = !busy
    }

    private fun confirmEnable() {
        val destinations = draft.orEmpty()
        if (destinations.isEmpty() || busy) return
        val bindings = draftBindings
        val inCalls = callTypesState.incoming
        val outCalls = callTypesState.outgoing
        val missedCalls = callTypesState.missed
        AlertDialog.Builder(this)
            .setTitle(R.string.auto_enable)
            .setMessage(getString(R.string.auto_confirm) + "\n\n" + routeSummary(destinations))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.auto_enable) { _, _ ->
                change {
                    AutomaticScheduling.activate(
                        applicationContext,
                        destinations,
                        TimeZone.getDefault().id,
                        inCalls,
                        outCalls,
                        missedCalls,
                        bindings,
                    )
                }
            }
            .show()
    }

    private fun openAndroidHistorySettings() {
        try {
            startActivity(Intent(TelecomManager.ACTION_CONFIGURE_CALL_LOG_INTEGRATION))
        } catch (_: ActivityNotFoundException) {
            feedbackText = getString(R.string.auto_system_history_unavailable)
        }
    }

    private fun change(action: Runnable?) {
        if (busy) return
        busy = true
        buttons()
        executor.execute {
            val done =
                try {
                    action!!.run()
                    true
                } catch (ignored: RuntimeException) {
                    false
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                feedbackText =
                    getString(if (done) R.string.auto_action_saved else R.string.auto_action_failed)
                buttons()
                load(false)
            }
        }
    }

    private fun load(reconcile: Boolean) {
        if (busy) {
            reloadRequested = true
            reloadReconcile = reloadReconcile || reconcile
            return
        }
        busy = true
        buttons()
        executor.execute {
            try {
                val settings =
                    ActivationControl(AndroidActivationStore(applicationContext)).current()
                if (reconcile && settings != null && settings.enabled) {
                    AutomaticScheduling.reconcile(applicationContext)
                }
                val loaded =
                    if (calendarAccess()) {
                        AndroidCalendarSink(applicationContext).choices()
                    } else {
                        Collections.emptyList()
                    }
                val apps =
                    AndroidCallSources.apps(
                        applicationContext,
                        settings?.destinations.orEmpty().values.flatten().toSet(),
                    )
                val summary =
                    SqliteLedger(applicationContext).use { ledger ->
                        ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                        ledger.counts()
                    }
                val worker =
                    if (settings == null) {
                        null
                    } else {
                        AndroidWorkStatusStore(applicationContext).read(settings.generation)
                    }
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    showLoadedSettings(settings, loaded, apps, summary, worker)
                }
            } catch (ignored: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    stateText = getString(R.string.auto_action_failed)
                    buttons()
                    reloadIfRequested()
                }
            }
        }
    }

    private fun showLoadedSettings(
        settings: ActivationControl.Settings?,
        loaded: List<AndroidCalendarSink.Choice>,
        apps: List<AndroidCallSources.App>,
        summary: DeliveryCounts,
        worker: WorkStatus?,
    ) {
        busy = false
        active = settings?.enabled == true
        choices = loaded
        appSources = apps
        calendarLabels = labels(loaded)
        availableBindings = loaded.associate { it.id to checkNotNull(it.binding) }
        showWorkStatus(settings, summary, worker)
        destinationOptionTokens = loaded.map { java.util.UUID.randomUUID().toString() }
        restoreRoutingDraft(settings)
        updateRoutingForm()
        if (!initialised && settings != null && !restoredCallTypes) {
            callTypesState =
                AutomaticCallTypesUiState(
                    incoming = settings.incoming,
                    outgoing = settings.outgoing,
                    missed = settings.missed,
                )
        }
        initialised = calendarAccess()
        buttons()
        reloadIfRequested()
    }

    private fun showWorkStatus(
        settings: ActivationControl.Settings?,
        summary: DeliveryCounts,
        worker: WorkStatus?,
    ) {
        stateText =
            when {
                settings?.enabled != true -> getString(R.string.auto_disabled)

                settings.destinations.values.singleOrNull() == setOf(CallRecord.PHONE) ->
                    getString(
                        R.string.auto_enabled,
                        calendarLabel(settings.calendar, settings.calendarBindings),
                    )

                else ->
                    getString(
                        R.string.auto_enabled_routes,
                        routeSummary(settings.destinations, settings.calendarBindings),
                    )
            }
        countsText =
            getString(
                R.string.auto_count_text,
                summary.pending,
                summary.verified,
                summary.review,
                summary.suppressed,
            )
        lastText =
            getString(
                R.string.auto_status_summary,
                getString(workerMessage(worker?.code?.name)),
                getString(R.string.auto_failure_count, worker?.failures ?: 0L),
            )
    }

    private fun restoreRoutingDraft(settings: ActivationControl.Settings?) {
        if (!calendarAccess()) return
        val preferred = getSharedPreferences("slice", MODE_PRIVATE).getLong("calendar", -1)
        val fallback = mapOf(preferred to setOf(CallRecord.PHONE))
        val priorBindings =
            if (draft == null) settings?.calendarBindings.orEmpty() else draftBindings
        draft =
            (draft ?: settings?.destinations ?: fallback).filterKeys {
                it in availableBindings &&
                    (it !in priorBindings || priorBindings[it] == availableBindings[it])
            }
        draftBindings = availableBindings.filterKeys { it in draft.orEmpty() }
    }

    private fun reloadIfRequested() {
        if (reloadRequested) {
            val reconcile = reloadReconcile
            reloadRequested = false
            reloadReconcile = false
            load(reconcile)
        }
    }

    private fun toggleDestination(token: String) {
        val index = destinationOptionTokens.indexOf(token)
        if (index !in choices.indices || busy) return
        val calendar = choices[index].id
        val updated = draft.orEmpty().toMutableMap()
        val existing = updated[calendar].orEmpty()
        val sources = if (source in existing) existing - source else existing + source
        if (sources.isEmpty()) updated.remove(calendar) else updated[calendar] = sources
        draft = updated
        draftBindings = availableBindings.filterKeys { it in updated }
        updateRoutingForm()
    }

    private fun updateRoutingForm() {
        val phone = CalendarDestinationOption(CallRecord.PHONE, sourceLabel(CallRecord.PHONE))
        val options =
            listOf(phone) +
                appSources.map { app ->
                    val label =
                        getString(
                            if (app.observed) {
                                R.string.auto_source_observed
                            } else {
                                R.string.auto_source_unverified
                            },
                            app.label,
                        )
                    CalendarDestinationOption(app.packageName, label)
                }
        if (options.none { it.token == source }) source = CallRecord.PHONE
        sourceUiState = CalendarDestinationUiState(options, source, enabled = !busy)
        destinationUiState =
            CalendarDestinationUiState(
                options =
                    choices.mapIndexed { index, choice ->
                        CalendarDestinationOption(
                            destinationOptionTokens[index],
                            calendarLabel(choice.id),
                        )
                    },
                checkedTokens =
                    choices
                        .mapIndexedNotNull { index, choice ->
                            destinationOptionTokens[index].takeIf {
                                source in draft?.get(choice.id).orEmpty()
                            }
                        }
                        .toSet(),
                enabled = !busy && calendarAccess(),
            )
        buttons()
    }

    private fun calendarLabel(id: Long, expected: Map<Long, String> = emptyMap()): String =
        calendarLabels[id]?.takeIf { id !in expected || expected[id] == availableBindings[id] }
            ?: getString(R.string.auto_destination_unavailable)

    private fun labels(calendars: List<AndroidCalendarSink.Choice>): Map<Long, String> {
        val named = calendars.groupBy { it.name }
        return calendars.associate { calendar ->
            val title = calendar.name ?: getString(R.string.auto_destination_unavailable)
            val peers = named.getValue(calendar.name)
            val label =
                if (peers.size == 1) {
                    title
                } else {
                    val account =
                        calendar.accountName ?: calendar.accountType ?: calendar.id.toString()
                    if (peers.count { (it.accountName ?: it.accountType) == account } > 1) {
                        "$title ($account, ${calendar.id})"
                    } else {
                        "$title ($account)"
                    }
                }
            calendar.id to label
        }
    }

    private fun sourceLabel(id: String): String =
        if (id == CallRecord.PHONE) {
            getString(R.string.auto_source_phone)
        } else {
            appSources.firstOrNull { it.packageName == id }?.label ?: id
        }

    private fun routeSummary(
        routes: Map<Long, Set<String>>,
        expected: Map<Long, String> = emptyMap(),
    ): String =
        routes.entries.joinToString("\n") { (calendar, sources) ->
            getString(
                R.string.auto_route_row,
                calendarLabel(calendar, expected),
                sources.joinToString(transform = ::sourceLabel),
            )
        }

    private fun workerMessage(code: String?): Int =
        when (code) {
            null -> R.string.auto_not_run

            "COMPLETE" -> R.string.auto_complete

            "PAUSED" -> R.string.auto_paused

            "BACKLOG" -> R.string.auto_backlog

            "PERMISSION_REQUIRED" -> R.string.slice_permission_needed

            "DESTINATION_UNAVAILABLE" -> R.string.slice_calendar_unavailable

            "SOURCE_CHANGED",
            "REVIEW_REQUIRED" -> R.string.slice_review

            "INVALID_CALL" -> R.string.slice_invalid

            "FAILED" -> R.string.auto_run_failed

            else -> R.string.auto_unknown
        }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SOURCE, source)
        draft?.let { saved ->
            outState.putBundle(
                STATE_DESTINATIONS,
                Bundle().apply {
                    for ((id, sources) in saved) {
                        putStringArrayList(
                            id.toString(),
                            ArrayList(sources),
                        )
                    }
                },
            )
        }
        outState.putBundle(
            STATE_BINDINGS,
            Bundle().apply {
                for ((id, binding) in draftBindings) putString(id.toString(), binding)
            },
        )
        outState.putBoolean(STATE_INCOMING, callTypesState.incoming)
        outState.putBoolean(STATE_OUTGOING, callTypesState.outgoing)
        outState.putBoolean(STATE_MISSED, callTypesState.missed)
        super.onSaveInstanceState(outState)
    }

    public override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val STATE_SOURCE = "automatic-source"
        const val STATE_DESTINATIONS = "automatic-destinations"
        const val STATE_BINDINGS = "automatic-destination-bindings"
        const val STATE_INCOMING = "automatic-call-types-incoming"
        const val STATE_OUTGOING = "automatic-call-types-outgoing"
        const val STATE_MISSED = "automatic-call-types-missed"
    }
}
