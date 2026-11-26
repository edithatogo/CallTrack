// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markobl.calllogsync.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
internal sealed interface AutomaticSettingsUiState {
    val form: AutomaticSettingsFormUiState

    data class Editing(override val form: AutomaticSettingsFormUiState) : AutomaticSettingsUiState

    data class Reviewing(
        override val form: AutomaticSettingsFormUiState,
        val review: AutomaticActivationReview,
    ) : AutomaticSettingsUiState
}

@Immutable
internal data class AutomaticSettingsFormUiState(
    val status: AutomaticStatusUiState = AutomaticStatusUiState("", "", "", ""),
    val actions: AutomaticSettingsActionsUiState = AutomaticSettingsActionsUiState(),
    val sources: CalendarDestinationUiState = CalendarDestinationUiState(),
    val destinations: CalendarDestinationUiState = CalendarDestinationUiState(),
    val callTypes: AutomaticCallTypesUiState = AutomaticCallTypesUiState(),
    val callTypesEnabled: Boolean = true,
    val hasRequiredAccess: Boolean = false,
)

@Immutable internal class AutomaticActivationReview(val message: String)

@HiltViewModel
internal class AutomaticSettingsViewModel
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val worker = Dispatchers.IO.limitedParallelism(1)
    private var form = AutomaticSettingsFormUiState()
    private var approval: PendingActivation? = null
    private val state =
        MutableStateFlow<AutomaticSettingsUiState>(AutomaticSettingsUiState.Editing(form))

    private var choices = emptyList<AndroidCalendarSink.Choice>()
    private var calendarLabels = emptyMap<Long, String>()
    private var availableBindings = emptyMap<Long, String>()
    private var appSources = emptyList<AndroidCallSources.App>()
    private var source = CallRecord.PHONE
    private var draft: Map<Long, Set<String>>? = null
    private var draftBindings = emptyMap<Long, String>()
    private var busy = false
    private var resumed = false
    private var initialised = false
    private var reloadRequested = false
    private var reloadReconcile = false
    private var hasDraftCallTypes = false
    private var destinationOptionTokens = emptyList<String>()

    init {
        restoreState(savedState[STATE_DRAFT])
        savedState.setSavedStateProvider(STATE_DRAFT) { saveState() }
        updateControls()
    }

    val uiState: StateFlow<AutomaticSettingsUiState> =
        state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    @MainThread
    fun setResumed(value: Boolean) {
        resumed = value
        if (value) refresh(reconcile = true) else dismissReview()
    }

    @MainThread
    fun permissionsChanged() {
        initialised = false
        refresh()
    }

    @MainThread
    fun pauseLogging() {
        if (!resumed) return
        change { AutomaticScheduling.pause(context) }
    }

    @MainThread
    fun refresh(reconcile: Boolean = false) {
        dismissReview()
        if (busy) {
            reloadRequested = true
            reloadReconcile = reloadReconcile || reconcile
            return
        }
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            try {
                val settings = ActivationControl(AndroidActivationStore(context)).current()
                if (reconcile && settings?.enabled == true) AutomaticScheduling.reconcile(context)
                val loaded =
                    if (calendarAccess()) AndroidCalendarSink(context).choices() else emptyList()
                val preferred =
                    if (loaded.isEmpty()) -1L else AndroidSettingsStore(context).selectedCalendar()
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
                    showLoadedSettings(settings, loaded, apps, summary, workerStatus, preferred)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    busy = false
                    form =
                        form.copy(
                            status =
                                form.status.copy(
                                    state = context.getString(R.string.auto_action_failed),
                                ),
                        )
                    updateControls()
                    reloadIfRequested()
                }
            }
        }
    }

    @MainThread
    fun selectSource(token: String) {
        if (!resumed || busy || form.sources.options.none { it.token == token }) return
        source = token
        updateRoutingForm()
    }

    @MainThread
    fun setCallTypes(callTypes: AutomaticCallTypesUiState) {
        if (!resumed || busy) return
        hasDraftCallTypes = true
        form = form.copy(callTypes = callTypes)
        updateControls()
    }

    @MainThread
    fun toggleDestination(token: String) {
        if (!resumed || busy || !calendarAccess()) return
        val index = destinationOptionTokens.indexOf(token)
        val calendar = choices.getOrNull(index)?.id ?: return
        val updated = draft.orEmpty().toMutableMap()
        val existing = updated[calendar].orEmpty()
        val sources = if (source in existing) existing - source else existing + source
        if (sources.isEmpty()) updated.remove(calendar) else updated[calendar] = sources
        draft = updated
        draftBindings = availableBindings.filterKeys { it in updated }
        updateRoutingForm()
    }

    @MainThread
    fun requestReview() {
        if (!resumed || !form.actions.enableEnabled || !hasRequiredAccess()) return
        val destinations = draft.orEmpty()
        val callTypes = form.callTypes
        val directionLabels =
            context.callTypeLabels(callTypes.incoming, callTypes.outgoing, callTypes.missed)
        val message =
            listOf(
                    context.getString(R.string.auto_confirm),
                    routeSummary(destinations),
                    context.getString(R.string.auto_confirm_types, directionLabels),
                )
                .joinToString("\n\n")
        approval =
            PendingActivation(
                AutomaticActivationReview(message),
                destinations,
                draftBindings,
                callTypes,
            )
        publish()
    }

    @MainThread
    fun confirmReview(review: AutomaticActivationReview) {
        val pending = approval ?: return
        if (pending.review !== review) return
        dismissReview()
        if (!resumed || busy) return
        if (!hasRequiredAccess()) {
            permissionsChanged()
            return
        }
        change {
            check(hasRequiredAccess()) { "Automatic logging permissions required" }
            AutomaticScheduling.activate(
                context,
                pending.destinations,
                TimeZone.getDefault().id,
                pending.callTypes.incoming,
                pending.callTypes.outgoing,
                pending.callTypes.missed,
                pending.bindings,
            )
        }
    }

    @MainThread
    fun dismissReview() {
        if (approval == null) return
        approval = null
        publish()
    }

    @MainThread
    fun systemHistoryUnavailable() {
        form =
            form.copy(
                status =
                    form.status.copy(
                        feedback = context.getString(R.string.auto_system_history_unavailable),
                    ),
            )
        publish()
    }

    private fun calendarAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun callAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasRequiredAccess(): Boolean = calendarAccess() && callAccess()

    private fun updateControls() {
        val hasCalendarAccess = calendarAccess()
        val hasAccess = hasCalendarAccess && callAccess()
        val callTypes = form.callTypes
        form =
            form.copy(
                hasRequiredAccess = hasAccess,
                actions =
                    AutomaticSettingsActionsUiState(
                        enableEnabled =
                            !busy &&
                                !draft.isNullOrEmpty() &&
                                hasAccess &&
                                (callTypes.incoming || callTypes.outgoing || callTypes.missed),
                        pauseEnabled = !busy,
                        refreshEnabled = true,
                    ),
                destinations =
                    if (hasCalendarAccess) {
                        form.destinations.copy(enabled = !busy)
                    } else {
                        CalendarDestinationUiState()
                    },
                sources = form.sources.copy(enabled = !busy),
                callTypesEnabled = !busy,
            )
        publish()
    }

    private fun publish() {
        state.value =
            approval?.let { AutomaticSettingsUiState.Reviewing(form, it.review) }
                ?: AutomaticSettingsUiState.Editing(form)
    }

    private fun change(action: () -> Unit) {
        if (busy) return
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            val done =
                try {
                    action()
                    true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    false
                }
            withContext(Dispatchers.Main.immediate) {
                busy = false
                val result = if (done) R.string.auto_action_saved else R.string.auto_action_failed
                form = form.copy(status = form.status.copy(feedback = context.getString(result)))
                refresh()
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
        choices = loaded
        appSources = apps
        calendarLabels =
            calendarChoiceLabels(loaded, context.getString(R.string.slice_unnamed_calendar))
        availableBindings = loaded.associate { it.id to checkNotNull(it.binding) }
        form = form.copy(status = workStatus(settings, summary, worker))
        destinationOptionTokens = loaded.map { UUID.randomUUID().toString() }
        restoreRoutingDraft(settings, preferred)
        if (!initialised && settings != null && !hasDraftCallTypes) {
            form =
                form.copy(
                    callTypes =
                        AutomaticCallTypesUiState(
                            settings.incoming,
                            settings.outgoing,
                            settings.missed,
                        ),
                )
        }
        initialised = calendarAccess()
        updateRoutingForm()
        reloadIfRequested()
    }

    private fun workStatus(
        settings: ActivationControl.Settings?,
        summary: DeliveryCounts,
        worker: WorkStatus?,
    ): AutomaticStatusUiState {
        val status =
            when {
                settings?.enabled != true -> context.getString(R.string.auto_disabled)

                settings.destinations.values.singleOrNull() == setOf(CallRecord.PHONE) ->
                    context.getString(
                        R.string.auto_enabled,
                        calendarLabel(settings.calendar, settings.calendarBindings),
                    )

                else ->
                    context.getString(
                        R.string.auto_enabled_routes,
                        routeSummary(settings.destinations, settings.calendarBindings),
                    )
            }
        val counts =
            context.getString(
                R.string.auto_count_text,
                summary.pending,
                summary.verified,
                summary.review,
                summary.suppressed,
            )
        val lastRun =
            context.getString(
                R.string.auto_status_summary,
                context.getString(workerMessage(worker?.code?.name)),
                context.getString(R.string.auto_failure_count, worker?.failures ?: 0L),
            )
        return AutomaticStatusUiState(status, counts, form.status.feedback, lastRun)
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
            refresh(reconcile)
        }
    }

    private fun updateRoutingForm() {
        val phone = CalendarDestinationOption(CallRecord.PHONE, sourceLabel(CallRecord.PHONE))
        val options = listOf(phone) + appSources.map { it.option(context) }
        if (options.none { it.token == source }) source = CallRecord.PHONE
        form =
            form.copy(
                sources = CalendarDestinationUiState(options, source),
                destinations =
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
                    ),
            )
        updateControls()
    }

    private fun calendarLabel(id: Long, expected: Map<Long, String> = emptyMap()): String =
        calendarLabels[id]?.takeIf { id !in expected || expected[id] == availableBindings[id] }
            ?: context.getString(R.string.auto_destination_unavailable)

    private fun sourceLabel(id: String): String =
        if (id == CallRecord.PHONE) {
            context.getString(R.string.auto_source_phone)
        } else {
            appSources.firstOrNull { it.packageName == id }?.label ?: id
        }

    private fun routeSummary(
        routes: Map<Long, Set<String>>,
        expected: Map<Long, String> = emptyMap(),
    ): String =
        routes.entries.joinToString("\n") { (calendar, sources) ->
            context.getString(
                R.string.auto_route_row,
                calendarLabel(calendar, expected),
                sources.joinToString(transform = ::sourceLabel),
            )
        }

    private fun restoreState(saved: Bundle?) {
        if (saved == null) return
        source = saved.getString(STATE_SOURCE) ?: CallRecord.PHONE
        draft =
            saved.getBundle(STATE_DESTINATIONS)?.let { routes ->
                routes.keySet().associate {
                    it.toLong() to routes.getStringArrayList(it).orEmpty().toSet()
                }
            }
        draftBindings =
            saved
                .getBundle(STATE_BINDINGS)
                ?.let { stored ->
                    stored.keySet().associate { it.toLong() to checkNotNull(stored.getString(it)) }
                }
                .orEmpty()
        form =
            form.copy(
                callTypes =
                    AutomaticCallTypesUiState(
                        incoming = saved.getBoolean(STATE_INCOMING, true),
                        outgoing = saved.getBoolean(STATE_OUTGOING, true),
                        missed = saved.getBoolean(STATE_MISSED, true),
                    ),
            )
        hasDraftCallTypes = true
    }

    private fun saveState(): Bundle =
        Bundle().apply {
            putString(STATE_SOURCE, source)
            draft?.let { routes ->
                putBundle(
                    STATE_DESTINATIONS,
                    Bundle().apply {
                        for ((id, sources) in routes) {
                            putStringArrayList(id.toString(), ArrayList(sources))
                        }
                    },
                )
            }
            putBundle(
                STATE_BINDINGS,
                Bundle().apply {
                    for ((id, binding) in draftBindings) putString(id.toString(), binding)
                },
            )
            putBoolean(STATE_INCOMING, form.callTypes.incoming)
            putBoolean(STATE_OUTGOING, form.callTypes.outgoing)
            putBoolean(STATE_MISSED, form.callTypes.missed)
        }

    private data class PendingActivation(
        val review: AutomaticActivationReview,
        val destinations: Map<Long, Set<String>>,
        val bindings: Map<Long, String>,
        val callTypes: AutomaticCallTypesUiState,
    )

    private companion object {
        const val STATE_DRAFT = "automatic-draft"
        const val STATE_SOURCE = "automatic-source"
        const val STATE_DESTINATIONS = "automatic-destinations"
        const val STATE_BINDINGS = "automatic-destination-bindings"
        const val STATE_INCOMING = "automatic-call-types-incoming"
        const val STATE_OUTGOING = "automatic-call-types-outgoing"
        const val STATE_MISSED = "automatic-call-types-missed"
    }
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
