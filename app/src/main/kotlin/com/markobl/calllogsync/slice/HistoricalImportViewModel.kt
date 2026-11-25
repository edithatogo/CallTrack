// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markobl.calllogsync.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_LOOKBACK_DAYS = 7L
private const val IMPORT_STEP_SIZE = 20
private const val DRAFT_KEY = "historicalImportDraft"

@Immutable
internal sealed interface HistoricalImportUiState {
    val form: HistoricalImportFormUiState

    data class Editing(override val form: HistoricalImportFormUiState) : HistoricalImportUiState

    data class Reviewing(
        override val form: HistoricalImportFormUiState,
        val review: HistoricalImportReview,
    ) : HistoricalImportUiState
}

@Immutable
internal data class HistoricalImportFormUiState(
    val zone: String,
    val filters: HistoricalImportFiltersUiState,
    val summary: String,
    val destination: CalendarDestinationUiState = CalendarDestinationUiState(),
    @get:StringRes val destinationMessage: Int? = null,
    val actions: HistoricalImportActionState = HistoricalImportActionState(),
    val feedback: String = "",
    val history: HistoricalImportHistoryUiState = HistoricalImportHistoryUiState(),
)

@Immutable
internal class HistoricalImportReview(@get:StringRes val actionLabel: Int, val message: String)

@HiltViewModel
internal class HistoricalImportViewModel
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val worker = Dispatchers.IO.limitedParallelism(1)

    @Volatile private var running = false
    private var resumed = false
    private var busy = false
    private var currentId: String? = null
    private var session: HistoricalImport.Session? = null
    private var pendingReview: PendingReview? = null
    private var calendarRefresh: Job? = null
    private var calendarsReady = false
    private val historyTargets = HashMap<String, String>()
    private val destinationTargets = HashMap<String, Long>()
    private var selected = -1L
    private var historyCursor = -1L
    private var renderVersion = 0L
    private val historyBack = ArrayList<Long>()
    private var nextHistoryCursor = -1L
    private var opened = false
    private var form: HistoricalImportFormUiState
    private val state: MutableStateFlow<HistoricalImportUiState>

    init {
        val saved = savedState.get<Bundle>(DRAFT_KEY)
        val zone = saved?.getString("zone") ?: ZoneId.systemDefault().id
        val today = LocalDate.now(ZoneId.of(zone))
        val filters =
            HistoricalImportFiltersUiState(
                fromDate =
                    if (saved == null) {
                        today.minusDays(DEFAULT_LOOKBACK_DAYS).toString()
                    } else {
                        saved.getString("from").orEmpty()
                    },
                toDate =
                    if (saved == null) {
                        today.plusDays(1).toString()
                    } else {
                        saved.getString("to").orEmpty()
                    },
                incoming = saved == null || saved.getBoolean("incoming"),
                outgoing = saved == null || saved.getBoolean("outgoing"),
                missed = saved == null || saved.getBoolean("missed"),
            )
        currentId = saved?.getString("session")
        selected = saved?.getLong("calendar", -1) ?: -1
        form = HistoricalImportFormUiState(zone, filters, context.getString(R.string.import_none))
        state = MutableStateFlow(HistoricalImportUiState.Editing(form))
        savedState.setSavedStateProvider(DRAFT_KEY) { saveDraft() }
        updateControls()
    }

    val uiState: StateFlow<HistoricalImportUiState> =
        state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    @MainThread
    fun setResumed(value: Boolean) {
        resumed = value
        if (!value) {
            running = false
            pendingReview = null
            calendarsReady = false
            calendarRefresh?.cancel()
            calendarRefresh = null
        } else {
            loadCalendars()
            if (!opened) {
                opened = true
                loadHistory(-1)
                if (currentId != null) loadSession(currentId)
            }
        }
        updateControls()
    }

    @MainThread
    fun permissionsChanged() {
        pendingReview = null
        if (resumed) loadCalendars()
        updateControls()
    }

    @MainThread
    fun setFilters(filters: HistoricalImportFiltersUiState) {
        if (!resumed) return
        form = form.copy(filters = filters)
        updateControls()
    }

    @MainThread
    fun selectDestination(token: String) {
        if (!resumed || !form.destination.enabled || !calendarAccess()) return
        selected = destinationTargets[token] ?: return
        form =
            form.copy(
                destination = form.destination.copy(selectedToken = token),
                destinationMessage = null,
            )
        updateControls()
    }

    @MainThread
    fun startPreview() {
        if (!resumed || !form.actions.createEnabled) return
        val filters = form.filters
        val range =
            try {
                ImportRange.parse(filters.fromDate, filters.toDate, form.zone)
            } catch (_: RuntimeException) {
                setFeedback(R.string.import_invalid_range)
                publish()
                return
            }
        val calendar = selected
        val start = {
            runImport { importer, _ ->
                importer.start(
                    calendar,
                    range.timezone,
                    range.from,
                    range.to,
                    filters.incoming,
                    filters.outgoing,
                    filters.missed,
                )
            }
        }
        val phase = session?.phase
        if (
            phase != null &&
                phase != HistoricalImport.Phase.COMPLETE &&
                phase != HistoricalImport.Phase.CANCELLED
        ) {
            review(R.string.import_create, context.getString(R.string.import_another), start)
        } else {
            start()
        }
    }

    @MainThread
    fun requestConfirmation() {
        val value = session ?: return
        if (!resumed || !form.actions.confirmEnabled) return
        review(
            R.string.import_confirm,
            context.resources.getQuantityString(
                R.plurals.import_confirm_text,
                value.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                value.count,
                destinationName(value),
                dates(value),
            ),
        ) {
            if (currentId == value.id) {
                runImport { importer, _ -> importer.confirm(value.id) }
            }
        }
    }

    @MainThread
    fun requestCancellation() {
        val id = currentId ?: return
        if (!resumed || !form.actions.cancelEnabled) return
        review(R.string.import_cancel, context.getString(R.string.import_cancel_text)) {
            if (currentId == id) {
                running = false
                perform({ importer, _ -> importer.cancel(id) }, false)
            }
        }
    }

    @MainThread
    fun finishReview(review: HistoricalImportReview, confirmed: Boolean) {
        val pending = pendingReview ?: return
        if (pending.review !== review) return
        pendingReview = null
        publish()
        if (confirmed && resumed) pending.confirm()
    }

    @MainThread
    fun setRunning(value: Boolean) {
        if (!resumed) return
        if (value && form.actions.continueEnabled) {
            running = true
            step()
        } else if (!value && running) {
            running = false
            setFeedback(R.string.import_stopping)
            updateControls()
        }
    }

    @MainThread
    fun openHistorySession(token: String) {
        if (!resumed) return
        val id = historyTargets[token] ?: return
        loadSession(id)
    }

    @MainThread
    fun changeHistoryPage(newer: Boolean) {
        if (!resumed || busy) return
        when {
            newer && historyBack.isNotEmpty() ->
                loadHistory(historyBack.removeAt(historyBack.lastIndex))

            !newer && nextHistoryCursor > 0 -> {
                historyBack.add(historyCursor)
                loadHistory(nextHistoryCursor)
            }
        }
    }

    override fun onCleared() {
        running = false
    }

    private fun review(@StringRes actionLabel: Int, message: String, confirm: () -> Unit) {
        pendingReview = PendingReview(HistoricalImportReview(actionLabel, message), confirm)
        publish()
    }

    private class PendingReview(val review: HistoricalImportReview, val confirm: () -> Unit)

    private fun runImport(action: Action) {
        if (busy || !access()) {
            updateControls()
            return
        }
        running = true
        perform(action, true)
    }

    private fun setFeedback(message: Int?) {
        form = form.copy(feedback = message?.let(context::getString).orEmpty())
    }

    private fun access(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED && calendarAccess()

    private fun calendarAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun updateControls() {
        val phase = session?.phase
        val permitted = access()
        val canReviewDestination = calendarsReady && !busy
        val filters = form.filters
        val hasCallTypes = filters.incoming || filters.outgoing || filters.missed
        form =
            form.copy(
                actions =
                    HistoricalImportActionState(
                        createEnabled =
                            canReviewDestination && selected >= 0 && permitted && hasCallTypes,
                        continueEnabled = !busy && continuing(phase),
                        confirmEnabled =
                            canReviewDestination && phase == HistoricalImport.Phase.PREVIEW,
                        stopEnabled = running,
                        cancelEnabled =
                            phase != null &&
                                phase != HistoricalImport.Phase.COMPLETE &&
                                phase != HistoricalImport.Phase.CANCELLED,
                        newerEnabled = !busy && historyBack.isNotEmpty(),
                        olderEnabled = !busy && nextHistoryCursor > 0,
                        hasRequiredAccess = permitted,
                        sessionSelected = phase != null,
                    ),
                destination =
                    form.destination.copy(enabled = canReviewDestination && calendarAccess()),
            )
        publish()
    }

    private fun publish() {
        state.value =
            pendingReview?.let { HistoricalImportUiState.Reviewing(form, it.review) }
                ?: HistoricalImportUiState.Editing(form)
    }

    private fun loadCalendars() {
        calendarsReady = false
        calendarRefresh?.cancel()
        calendarRefresh =
            viewModelScope.launch(worker) {
                try {
                    val loaded =
                        if (calendarAccess()) AndroidCalendarSink(context).choices()
                        else emptyList()
                    withContext(Dispatchers.Main.immediate) {
                        val preferred = selected
                        destinationTargets.clear()
                        val labels =
                            calendarChoiceLabels(
                                loaded,
                                context.getString(R.string.slice_unnamed_calendar),
                            )
                        val options = loaded.map { choice ->
                            val token = UUID.randomUUID().toString()
                            destinationTargets[token] = choice.id
                            CalendarDestinationOption(token, labels.getValue(choice.id))
                        }
                        val token =
                            options.firstOrNull { destinationTargets[it.token] == preferred }?.token
                        selected = token?.let(destinationTargets::get) ?: -1L
                        val message =
                            when {
                                !calendarAccess() -> null
                                preferred >= 0 && selected < 0 ->
                                    R.string.auto_destination_unavailable
                                options.isEmpty() -> R.string.slice_no_calendars
                                else -> null
                            }
                        form =
                            form.copy(
                                destination =
                                    CalendarDestinationUiState(
                                        options = options,
                                        selectedToken = token,
                                    ),
                                destinationMessage = message,
                            )
                        if (session != null) renderSession()
                        calendarRefresh = null
                        calendarsReady = true
                        updateControls()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    withContext(Dispatchers.Main.immediate) {
                        calendarRefresh = null
                        showFailure()
                    }
                }
            }
    }

    private fun step() {
        if (!running || busy || currentId == null) return
        val id = currentId
        perform(
            { importer, store ->
                val latest =
                    store.get(id) ?: throw IllegalArgumentException("Import session unavailable")
                when (latest.phase) {
                    HistoricalImport.Phase.BUILDING -> importer.preview(id, IMPORT_STEP_SIZE)

                    HistoricalImport.Phase.CONFIRMED ->
                        importer.deliver(id, IMPORT_STEP_SIZE) { running }

                    else -> latest
                }
            },
            true,
        )
    }

    private fun interface Action {
        fun run(importer: HistoricalImport, store: SqliteImportStore): HistoricalImport.Session
    }

    private fun perform(action: Action, follow: Boolean) {
        val ticket = ++renderVersion
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            try {
                SqliteImportStore(context).use { store ->
                    SqliteLedger(context).use { ledger ->
                        ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                        val importer =
                            HistoricalImport(
                                store,
                                AndroidCallLogSource(context),
                                ledger.outbox(),
                                DurableDelivery(ledger.outbox(), AndroidCalendarSink(context)),
                            )
                        val result = action.run(importer, store)
                        withContext(Dispatchers.Main.immediate) {
                            if (ticket != renderVersion) return@withContext
                            showImportResult(result, follow)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    if (ticket != renderVersion) return@withContext
                    running = false
                    busy = false
                    setFeedback(R.string.import_failed)
                    updateControls()
                }
            }
        }
    }

    private fun showImportResult(result: HistoricalImport.Session, follow: Boolean) {
        busy = false
        val previous = session
        if (currentId != result.id || previous == null || result.revision >= previous.revision) {
            currentId = result.id
            session = result
            renderSession()
        }
        if (result.stopped != null) {
            running = false
            setFeedback(R.string.import_blocked)
        } else {
            setFeedback(null)
        }
        updateControls()
        if (follow && running && continuing(result.phase)) {
            viewModelScope.launch { if (running) step() }
        } else {
            running = false
            updateControls()
            loadHistory(historyCursor)
        }
    }

    private fun continuing(phase: HistoricalImport.Phase?): Boolean =
        phase == HistoricalImport.Phase.BUILDING || phase == HistoricalImport.Phase.CONFIRMED

    private fun loadSession(id: String?) {
        if (busy) return
        running = false
        currentId = id
        session = null
        perform(
            { _, store ->
                store.get(id) ?: throw IllegalArgumentException("Import session unavailable")
            },
            false,
        )
    }

    private fun dates(session: HistoricalImport.Session): String {
        val timezone = session.timezone
        val zone = TimeZone.getTimeZone(timezone).toZoneId()
        val from = Instant.ofEpochMilli(session.from).atZone(zone).toLocalDate()
        val to = Instant.ofEpochMilli(session.to).atZone(zone).toLocalDate()
        return "$from → $to ($timezone)"
    }

    private fun destinationName(session: HistoricalImport.Session): String =
        form.destination.options
            .firstOrNull { destinationTargets[it.token] == session.calendar }
            ?.label ?: context.getString(R.string.auto_destination_unavailable)

    private fun renderSession() {
        val value = session ?: return
        form =
            form.copy(
                summary =
                    context.getString(
                        R.string.import_progress,
                        dates(value),
                        context.getString(phaseLabel(value.phase)),
                        destinationName(value),
                        context.callTypeLabels(value.incoming, value.outgoing, value.missed),
                        value.count,
                        value.saved,
                        value.review,
                    ),
            )
    }

    private fun phaseLabel(phase: HistoricalImport.Phase): Int =
        when (phase) {
            HistoricalImport.Phase.BUILDING -> R.string.import_building
            HistoricalImport.Phase.PREVIEW -> R.string.import_preview
            HistoricalImport.Phase.CONFIRMED -> R.string.import_confirmed
            HistoricalImport.Phase.COMPLETE -> R.string.import_complete
            HistoricalImport.Phase.CANCELLED -> R.string.import_cancelled
            HistoricalImport.Phase.REVIEW -> R.string.import_review
        }

    private fun loadHistory(before: Long) {
        viewModelScope.launch(worker) {
            try {
                SqliteImportStore(context).use { store ->
                    val records = store.sessions(before, 11)
                    withContext(Dispatchers.Main.immediate) {
                        historyCursor = before
                        val size = minOf(10, records.size)
                        historyTargets.clear()
                        val items =
                            (0 until size).map { index ->
                                val item = checkNotNull(records[index].session)
                                val token = UUID.randomUUID().toString()
                                historyTargets[token] = item.id
                                HistoricalImportHistoryItem(
                                    token = token,
                                    label =
                                        context.getString(
                                            R.string.import_history_item,
                                            dates(item),
                                            context.getString(phaseLabel(item.phase)),
                                        ),
                                )
                            }
                        form = form.copy(history = HistoricalImportHistoryUiState(items))
                        nextHistoryCursor =
                            if (records.size > size) records[size - 1].cursor else -1
                        updateControls()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                withContext(Dispatchers.Main.immediate) { showFailure() }
            }
        }
    }

    private fun showFailure() {
        setFeedback(R.string.import_failed)
        updateControls()
    }

    private fun saveDraft(): Bundle =
        Bundle().apply {
            putString("zone", form.zone)
            putString("session", currentId)
            putString("from", form.filters.fromDate)
            putString("to", form.filters.toDate)
            putLong("calendar", selected)
            putBoolean("incoming", form.filters.incoming)
            putBoolean("outgoing", form.filters.outgoing)
            putBoolean("missed", form.filters.missed)
        }
}
