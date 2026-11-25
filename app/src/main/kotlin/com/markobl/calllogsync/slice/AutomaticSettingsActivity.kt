package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
private fun AutomaticStatusDetails(state: AutomaticStatusUiState) {
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
internal fun AutomaticSettingsScreen(
    activity: ComponentActivity,
    onOpenImports: () -> Unit,
    onOpenRecovery: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state =
        rememberSaveable(
            saver =
                Saver(
                    save = { it.saveState() },
                    restore = { AutomaticSettingsState(activity, scope, it) },
                ),
        ) {
            AutomaticSettingsState(activity, scope, null)
        }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            state.permissionsChanged()
        }
    LifecycleResumeEffect(state) {
        state.resume()
        onPauseOrDispose { state.pause() }
    }
    state.Content(
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
        onOpenImports = onOpenImports,
        onOpenRecovery = onOpenRecovery,
    )
}

private class AutomaticSettingsState(
    private val activity: ComponentActivity,
    private val scope: CoroutineScope,
    saved: Bundle?,
) {
    private val worker = Dispatchers.IO.limitedParallelism(1)
    private var stateText by mutableStateOf("")
    private var countsText by mutableStateOf("")
    private var lastText by mutableStateOf("")
    private var feedbackText by mutableStateOf("")
    private var confirmation by mutableStateOf<Confirmation?>(null)
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
    private var hasRequiredAccess by mutableStateOf(false)
    private var restoredCallTypes = false
    private var destinationOptionTokens: List<String> = Collections.emptyList()

    init {
        source = saved?.getString(STATE_SOURCE) ?: CallRecord.PHONE
        draft =
            saved?.getBundle(STATE_DESTINATIONS)?.let { routes ->
                routes.keySet().associate {
                    it.toLong() to routes.getStringArrayList(it).orEmpty().toSet()
                }
            }
        draftBindings =
            saved
                ?.getBundle(STATE_BINDINGS)
                ?.let { stored ->
                    stored.keySet().associate { it.toLong() to checkNotNull(stored.getString(it)) }
                }
                .orEmpty()
        callTypesState =
            AutomaticCallTypesUiState(
                incoming = saved?.getBoolean(STATE_INCOMING, true) ?: true,
                outgoing = saved?.getBoolean(STATE_OUTGOING, true) ?: true,
                missed = saved?.getBoolean(STATE_MISSED, true) ?: true,
            )
        restoredCallTypes = saved != null
        buttons()
    }

    @Composable
    fun Content(
        onRequestAccess: () -> Unit,
        onOpenImports: () -> Unit,
        onOpenRecovery: () -> Unit,
    ) {
        val status = AutomaticStatusUiState(stateText, countsText, feedbackText, lastText)
        AutomaticStatusPanel(status, showDetails = false)
        AutomaticStatusActions(
            state = actionsUiState,
            pauseLabel = activity.getString(R.string.auto_pause),
            refreshLabel = activity.getString(R.string.auto_refresh),
            onPause = { change { AutomaticScheduling.pause(activity.applicationContext) } },
            onRefresh = { load(false) },
        )
        SettingsForm(onRequestAccess)
        SettingsActions(onOpenImports, onOpenRecovery)
        AutomaticStatusDetails(status)
        confirmation?.let { ConfirmationDialog(it) { confirmation = null } }
    }

    @Composable
    private fun SettingsForm(onRequestAccess: () -> Unit) {
        AutomaticSettingsIntroPanel(text = activity.getString(R.string.auto_intro))
        AutomaticSettingsAccessPanel(
            label = activity.getString(R.string.auto_access),
            hasRequiredAccess = hasRequiredAccess,
            onRequestAccess = onRequestAccess,
        )
        if (sourceUiState.options.size > 1) {
            SectionHeading(stringResource(R.string.auto_source_heading))
            CalendarDestinationPicker(
                state = sourceUiState,
                tagPrefix = "auto_source",
                chooseLabel = activity.getString(R.string.auto_source_phone),
                onSelect = { selected ->
                    source = selected
                    updateRoutingForm()
                },
            )
            ActionButton(
                label = activity.getString(R.string.auto_system_history),
                tag = "auto_system_history",
                enabled = sourceUiState.enabled,
                onClick = ::openAndroidHistorySettings,
                style = ButtonStyle.Text,
            )
        }
        CalendarSelection()
        Column {
            SectionHeading(stringResource(R.string.auto_types_heading))
            AutomaticCallTypesPanel(
                state = callTypesState,
                incomingLabel = activity.getString(R.string.auto_incoming),
                outgoingLabel = activity.getString(R.string.auto_outgoing),
                missedLabel = activity.getString(R.string.auto_missed),
                enabled = callTypesEnabled,
                onStateChange = {
                    callTypesState = it
                    buttons()
                },
            )
        }
    }

    @Composable
    private fun CalendarSelection() {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeading(
                stringResource(
                    if (sourceUiState.options.size > 1) {
                        R.string.auto_calendars_heading
                    } else {
                        R.string.auto_phone_calendars_heading
                    },
                ),
            )
            CalendarDestinationPicker(
                state = destinationUiState,
                tagPrefix = "auto_calendar",
                chooseLabel = activity.getString(R.string.auto_choose_calendars),
                onSelect = ::toggleDestination,
            )
        }
    }

    @Composable
    private fun SettingsActions(onOpenImports: () -> Unit, onOpenRecovery: () -> Unit) {
        AutomaticApplyButton(
            state = actionsUiState,
            label = activity.getString(R.string.auto_enable),
            onApply = ::confirmEnable,
        )
        HorizontalDivider()
        AutomaticSettingsNavigationPanel(
            importLabel = activity.getString(R.string.import_open),
            recoveryLabel = activity.getString(R.string.recovery_open),
            onOpenImports = onOpenImports,
            onOpenRecovery = onOpenRecovery,
        )
    }

    fun resume() {
        load(true)
    }

    fun pause() {
        confirmation = null
    }

    fun permissionsChanged() {
        initialised = false
        load(false)
    }

    private fun calendarAccess(): Boolean =
        activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun callAccess(): Boolean =
        activity.checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        hasRequiredAccess = calendarAccess() && callAccess()
        actionsUiState =
            AutomaticSettingsActionsUiState(
                enableEnabled =
                    !busy &&
                        !draft.isNullOrEmpty() &&
                        hasRequiredAccess &&
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
        val directionLabels = activity.callTypeLabels(inCalls, outCalls, missedCalls)
        confirmation =
            Confirmation(
                title = activity.getString(R.string.auto_enable),
                message =
                    listOf(
                            activity.getString(R.string.auto_confirm),
                            routeSummary(destinations),
                            activity.getString(R.string.auto_confirm_types, directionLabels),
                        )
                        .joinToString("\n\n"),
                actionLabel = activity.getString(R.string.auto_enable),
            ) {
                change {
                    AutomaticScheduling.activate(
                        activity.applicationContext,
                        destinations,
                        TimeZone.getDefault().id,
                        inCalls,
                        outCalls,
                        missedCalls,
                        bindings,
                    )
                }
            }
    }

    private fun openAndroidHistorySettings() {
        try {
            activity.startActivity(Intent(TelecomManager.ACTION_CONFIGURE_CALL_LOG_INTEGRATION))
        } catch (_: ActivityNotFoundException) {
            feedbackText = activity.getString(R.string.auto_system_history_unavailable)
        }
    }

    private fun change(action: Runnable?) {
        if (busy) return
        busy = true
        buttons()
        scope.launch(worker) {
            val done =
                try {
                    action!!.run()
                    true
                } catch (ignored: RuntimeException) {
                    false
                }
            withContext(Dispatchers.Main.immediate) {
                if (activity.isDestroyed) return@withContext
                busy = false
                feedbackText =
                    activity.getString(
                        if (done) R.string.auto_action_saved else R.string.auto_action_failed,
                    )
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
        scope.launch(worker) {
            try {
                val context = activity.applicationContext
                val settings = ActivationControl(AndroidActivationStore(context)).current()
                if (reconcile && settings != null && settings.enabled) {
                    AutomaticScheduling.reconcile(context)
                }
                val loaded =
                    if (calendarAccess()) {
                        AndroidCalendarSink(context).choices()
                    } else {
                        Collections.emptyList()
                    }
                val preferred =
                    if (loaded.isEmpty()) {
                        -1L
                    } else {
                        AndroidSettingsStore(context).selectedCalendar()
                    }
                val apps =
                    AndroidCallSources.apps(
                        context,
                        settings?.destinations.orEmpty().values.flatten().toSet(),
                    )
                val summary =
                    SqliteLedger(context).use { ledger ->
                        ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                        ledger.counts()
                    }
                val workerStatus = settings?.let {
                    AndroidWorkStatusStore(context).read(it.generation)
                }
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed) return@withContext
                    showLoadedSettings(settings, loaded, apps, summary, workerStatus, preferred)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed) return@withContext
                    busy = false
                    stateText = activity.getString(R.string.auto_action_failed)
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
        preferred: Long,
    ) {
        busy = false
        active = settings?.enabled == true
        choices = loaded
        appSources = apps
        calendarLabels =
            calendarChoiceLabels(loaded, activity.getString(R.string.slice_unnamed_calendar))
        availableBindings = loaded.associate { it.id to checkNotNull(it.binding) }
        showWorkStatus(settings, summary, worker)
        destinationOptionTokens = loaded.map { java.util.UUID.randomUUID().toString() }
        restoreRoutingDraft(settings, preferred)
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
                settings?.enabled != true -> activity.getString(R.string.auto_disabled)

                settings.destinations.values.singleOrNull() == setOf(CallRecord.PHONE) ->
                    activity.getString(
                        R.string.auto_enabled,
                        calendarLabel(settings.calendar, settings.calendarBindings),
                    )

                else ->
                    activity.getString(
                        R.string.auto_enabled_routes,
                        routeSummary(settings.destinations, settings.calendarBindings),
                    )
            }
        countsText =
            activity.getString(
                R.string.auto_count_text,
                summary.pending,
                summary.verified,
                summary.review,
                summary.suppressed,
            )
        lastText =
            activity.getString(
                R.string.auto_status_summary,
                activity.getString(workerMessage(worker?.code?.name)),
                activity.getString(R.string.auto_failure_count, worker?.failures ?: 0L),
            )
    }

    private fun restoreRoutingDraft(settings: ActivationControl.Settings?, preferred: Long) {
        if (!calendarAccess()) return
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
                        activity.getString(
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
            ?: activity.getString(R.string.auto_destination_unavailable)

    private fun sourceLabel(id: String): String =
        if (id == CallRecord.PHONE) {
            activity.getString(R.string.auto_source_phone)
        } else {
            appSources.firstOrNull { it.packageName == id }?.label ?: id
        }

    private fun routeSummary(
        routes: Map<Long, Set<String>>,
        expected: Map<Long, String> = emptyMap(),
    ): String =
        routes.entries.joinToString("\n") { (calendar, sources) ->
            activity.getString(
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

    fun saveState(): Bundle {
        val outState = Bundle()
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
        return outState
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
