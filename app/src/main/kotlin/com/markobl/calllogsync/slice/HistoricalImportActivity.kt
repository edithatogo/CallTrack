package com.markobl.calllogsync.slice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.HashMap
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_LOOKBACK_DAYS = 7L
private const val IMPORT_STEP_SIZE = 20

/** Foreground bounded runs; opening/resuming never confirms historical delivery. */
@AndroidEntryPoint
class HistoricalImportActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Import)
    }
}

@Composable
internal fun HistoricalImportScreen(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val state =
        rememberSaveable(
            saver =
                Saver(
                    save = { it.saveState() },
                    restore = { HistoricalImportState(activity, scope, it) },
                ),
        ) {
            HistoricalImportState(activity, scope, null)
        }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            state.permissionsChanged()
        }
    LifecycleResumeEffect(state) {
        state.resume()
        onPauseOrDispose { state.pause() }
    }
    state.Content {
        permissions.launch(
            arrayOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
            ),
        )
    }
}

private class HistoricalImportState(
    private val activity: ComponentActivity,
    private val scope: CoroutineScope,
    saved: Bundle?,
) {
    private val worker = Dispatchers.IO.limitedParallelism(1)

    @Volatile private var running = false
    private var busy = false
    private var currentId: String? = null
    private var zone: String? = null
    private var session: HistoricalImport.Session? = null
    private var destinationState by mutableStateOf(CalendarDestinationUiState())
    private var destinationMessage by mutableStateOf<Int?>(null)
    private var filters by mutableStateOf(HistoricalImportFiltersUiState())
    private var actionState by mutableStateOf(HistoricalImportActionState())
    private var summaryText by mutableStateOf("")
    private var feedbackText by mutableStateOf("")
    private var confirmation by mutableStateOf<Confirmation?>(null)
    private var historyState by mutableStateOf(HistoricalImportHistoryUiState())
    private val historyTargets = HashMap<String, String>()
    private val destinationTargets = HashMap<String, Long>()
    private var selectedDestinationToken: String? = null
    private var selected = -1L
    private var historyCursor = -1L
    private var renderVersion = 0L
    private val historyBack = ArrayList<Long>()
    private var nextHistoryCursor = -1L
    private var opened = false

    init {
        zone =
            if (saved == null) {
                ZoneId.systemDefault().id
            } else {
                saved.getString("zone", ZoneId.systemDefault().id)
            }
        currentId = saved?.getString("session")
        filters =
            HistoricalImportFiltersUiState(
                fromDate =
                    if (saved == null) {
                        LocalDate.now(ZoneId.of(zone)).minusDays(DEFAULT_LOOKBACK_DAYS).toString()
                    } else {
                        saved.getString("from") ?: ""
                    },
                toDate =
                    if (saved == null) {
                        LocalDate.now(ZoneId.of(zone)).plusDays(1).toString()
                    } else {
                        saved.getString("to") ?: ""
                    },
                incoming = saved == null || saved.getBoolean("incoming"),
                outgoing = saved == null || saved.getBoolean("outgoing"),
                missed = saved == null || saved.getBoolean("missed"),
            )
        summaryText = activity.getString(R.string.import_none)
        selected = saved?.getLong("calendar", -1) ?: -1
        buttons()
    }

    fun resume() {
        if (!opened) {
            opened = true
            loadCalendars(selected)
            loadHistory(-1)
            if (currentId != null) loadSession(currentId)
        } else {
            buttons()
        }
    }

    fun permissionsChanged() {
        loadCalendars(selected)
        buttons()
    }

    @Composable
    fun Content(onRequestAccess: () -> Unit) {
        ImportForm(onRequestAccess)
        ImportActions()
        ImportHistory()
        confirmation?.let { ConfirmationDialog(it) { confirmation = null } }
    }

    @Composable
    private fun ImportForm(onRequestAccess: () -> Unit) {
        HistoricalImportIntroPanel(activity.getString(R.string.import_intro, zone))
        HistoricalImportAccessPanel(
            label = stringResource(R.string.auto_access),
            hasRequiredAccess = actionState.hasRequiredAccess,
            onRequestAccess = onRequestAccess,
        )
        CalendarDestinationPicker(
            state = destinationState,
            tagPrefix = "import_calendar",
            chooseLabel = stringResource(R.string.slice_choose),
            unavailableLabel = destinationMessage?.let { stringResource(it) },
            onSelect = ::selectDestination,
        )
        HistoricalImportFiltersPanel(
            state = filters,
            fromLabel = stringResource(R.string.import_from),
            toLabel = stringResource(R.string.import_to),
            incomingLabel = stringResource(R.string.auto_incoming),
            outgoingLabel = stringResource(R.string.auto_outgoing),
            missedLabel = stringResource(R.string.auto_missed),
            onStateChange = {
                filters = it
                buttons()
            },
        )
    }

    @Composable
    private fun ImportActions() {
        val focus = LocalFocusManager.current
        HistoricalImportCreateActionPanel(
            state = actionState,
            label = stringResource(R.string.import_create),
            onCreate = {
                focus.clearFocus()
                startPreview()
            },
        )
        HistoricalImportSummaryText(summaryText)
        if (actionState.sessionSelected) {
            HistoricalImportRunActionsPanel(
                state = actionState,
                continueLabel = stringResource(R.string.import_continue),
                confirmLabel = stringResource(R.string.import_confirm),
                stopLabel = stringResource(R.string.import_stop),
                cancelLabel = stringResource(R.string.import_cancel),
                onContinue = {
                    running = true
                    step()
                },
                onConfirm = ::confirmImport,
                onStop = {
                    running = false
                    setFeedback(R.string.import_stopping)
                    buttons()
                },
                onCancel = ::cancelImport,
            )
        }
        HistoricalImportFeedbackText(feedbackText)
    }

    @Composable
    private fun ImportHistory() {
        HistoricalImportHistoryPanel(
            state = historyState,
            title = stringResource(R.string.import_history),
            emptyMessage = stringResource(R.string.import_empty_history),
            onOpen = ::openHistorySession,
        )
        HistoricalImportPaginationPanel(
            state = actionState,
            newerLabel = stringResource(R.string.import_newer),
            olderLabel = stringResource(R.string.import_older),
            onNewer = {
                if (!busy && historyBack.isNotEmpty()) {
                    loadHistory(historyBack.removeAt(historyBack.size - 1))
                }
            },
            onOlder = {
                if (!busy && nextHistoryCursor > 0) {
                    historyBack.add(historyCursor)
                    loadHistory(nextHistoryCursor)
                }
            },
        )
    }

    private fun setFeedback(message: Int?) {
        feedbackText = message?.let(activity::getString) ?: ""
    }

    private fun access(): Boolean =
        activity.checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED && calendarAccess()

    private fun calendarAccess(): Boolean =
        activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        val phase = session?.phase
        val canContinue = continuing(phase)
        val permitted = access()
        val hasCallTypes = filters.incoming || filters.outgoing || filters.missed
        actionState =
            HistoricalImportActionState(
                createEnabled = !busy && selected >= 0 && permitted && hasCallTypes,
                continueEnabled = !busy && canContinue,
                confirmEnabled = !busy && phase == HistoricalImport.Phase.PREVIEW,
                stopEnabled = running,
                cancelEnabled =
                    phase != null &&
                        phase != HistoricalImport.Phase.COMPLETE &&
                        phase != HistoricalImport.Phase.CANCELLED,
                newerEnabled = !busy && historyBack.isNotEmpty(),
                olderEnabled = !busy && nextHistoryCursor > 0,
                hasRequiredAccess = permitted,
                sessionSelected = phase != null,
            )
        destinationState = destinationState.copy(enabled = !busy && calendarAccess())
    }

    private fun loadCalendars(preferred: Long) {
        scope.launch(worker) {
            try {
                val loaded =
                    if (calendarAccess()) {
                        AndroidCalendarSink(activity.applicationContext).choices()
                    } else {
                        Collections.emptyList()
                    }
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed) return@withContext
                    destinationTargets.clear()
                    val labels =
                        calendarChoiceLabels(
                            loaded,
                            activity.getString(R.string.slice_unnamed_calendar),
                        )
                    val options = loaded.map { choice ->
                        val token = UUID.randomUUID().toString()
                        destinationTargets[token] = choice.id
                        CalendarDestinationOption(
                            token,
                            labels.getValue(choice.id),
                        )
                    }
                    selectedDestinationToken =
                        options.firstOrNull { destinationTargets[it.token] == preferred }?.token
                    selected = selectedDestinationToken?.let(destinationTargets::get) ?: -1L
                    destinationMessage =
                        when {
                            !calendarAccess() -> null
                            preferred >= 0 && selected < 0 -> R.string.auto_destination_unavailable
                            options.isEmpty() -> R.string.slice_no_calendars
                            else -> null
                        }
                    destinationState =
                        CalendarDestinationUiState(
                            options = options,
                            selectedToken = selectedDestinationToken,
                            enabled = !busy && calendarAccess(),
                        )
                    if (session != null) render()
                    buttons()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun startPreview() {
        if (busy || selected < 0) return
        val range =
            try {
                ImportRange.parse(filters.fromDate, filters.toDate, zone)
            } catch (ignored: RuntimeException) {
                setFeedback(R.string.import_invalid_range)
                return
            }
        val calendar = selected
        val inCalls = filters.incoming
        val outCalls = filters.outgoing
        val missedCalls = filters.missed
        val start = Runnable {
            running = true
            perform(
                { importer, _ ->
                    importer.start(
                        calendar,
                        range.timezone,
                        range.from,
                        range.to,
                        inCalls,
                        outCalls,
                        missedCalls,
                    )
                },
                true,
            )
        }
        val value = session
        if (
            value != null &&
                value.phase != HistoricalImport.Phase.COMPLETE &&
                value.phase != HistoricalImport.Phase.CANCELLED
        ) {
            confirmation =
                Confirmation(
                    title = activity.getString(R.string.import_create),
                    message = activity.getString(R.string.import_another),
                    actionLabel = activity.getString(R.string.import_create),
                    onConfirm = start::run,
                )
        } else {
            start.run()
        }
    }

    private fun selectDestination(token: String) {
        val id = destinationTargets[token] ?: return
        if (destinationState.options.none { it.token == token }) return
        selectedDestinationToken = token
        selected = id
        destinationMessage = null
        destinationState = destinationState.copy(selectedToken = token)
        buttons()
    }

    private fun confirmImport() {
        val value = session
        if (busy || value == null || value.phase != HistoricalImport.Phase.PREVIEW) return
        val id = currentId
        confirmation =
            Confirmation(
                title = activity.getString(R.string.import_confirm),
                message =
                    activity.resources.getQuantityString(
                        R.plurals.import_confirm_text,
                        value.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        value.count,
                        destinationName(value),
                        dates(value),
                    ),
                actionLabel = activity.getString(R.string.import_confirm),
            ) {
                running = true
                perform({ importer, _ -> importer.confirm(id) }, true)
            }
    }

    private fun cancelImport() {
        if (session == null) return
        val id = currentId
        confirmation =
            Confirmation(
                title = activity.getString(R.string.import_cancel),
                message = activity.getString(R.string.import_cancel_text),
                actionLabel = activity.getString(R.string.import_cancel),
            ) {
                running = false
                perform({ importer, _ -> importer.cancel(id) }, false)
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
        buttons()
        scope.launch(worker) {
            try {
                SqliteImportStore(activity.applicationContext).use { store ->
                    SqliteLedger(activity.applicationContext).use { ledger ->
                        ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                        val importer =
                            HistoricalImport(
                                store,
                                AndroidCallLogSource(activity.applicationContext),
                                ledger.outbox(),
                                DurableDelivery(
                                    ledger.outbox(),
                                    AndroidCalendarSink(activity.applicationContext),
                                ),
                            )
                        val result = action.run(importer, store)
                        withContext(Dispatchers.Main.immediate) {
                            if (activity.isDestroyed || ticket != renderVersion) return@withContext
                            showImportResult(result, follow)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed || ticket != renderVersion) return@withContext
                    running = false
                    busy = false
                    setFeedback(R.string.import_failed)
                    buttons()
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
            render()
        }
        if (result.stopped != null) {
            running = false
            setFeedback(R.string.import_blocked)
        } else {
            setFeedback(null)
        }
        buttons()
        if (follow && running && continuing(result.phase)) {
            scope.launch { if (running) step() }
        } else {
            running = false
            buttons()
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

    private fun dates(s: HistoricalImport.Session): String {
        val z = TimeZone.getTimeZone(s.timezone).toZoneId()
        return "${Instant.ofEpochMilli(
            s.from,
        ).atZone(
            z,
        ).toLocalDate()} → ${Instant.ofEpochMilli(s.to).atZone(z).toLocalDate()} (${s.timezone})"
    }

    private fun destinationName(value: HistoricalImport.Session): String? {
        destinationState.options
            .firstOrNull { destinationTargets[it.token] == value.calendar }
            ?.let {
                return it.label
            }
        return activity.getString(R.string.auto_destination_unavailable)
    }

    private fun typeNames(value: HistoricalImport.Session): String =
        activity.callTypeLabels(value.incoming, value.outgoing, value.missed)

    private fun render() {
        val value = session!!
        summaryText =
            activity.getString(
                R.string.import_progress,
                dates(value),
                activity.getString(phaseLabel(value.phase)),
                destinationName(value),
                typeNames(value),
                value.count,
                value.saved,
                value.review,
            )
    }

    private fun phaseLabel(phase: HistoricalImport.Phase): Int =
        when (phase) {
            HistoricalImport.Phase.BUILDING -> R.string.import_building
            HistoricalImport.Phase.PREVIEW -> R.string.import_preview
            HistoricalImport.Phase.CONFIRMED -> R.string.import_confirmed
            HistoricalImport.Phase.COMPLETE -> R.string.import_complete
            HistoricalImport.Phase.CANCELLED -> R.string.import_cancelled
            else -> R.string.import_review
        }

    private fun loadHistory(before: Long) {
        scope.launch(worker) {
            try {
                SqliteImportStore(activity.applicationContext).use { store ->
                    val records = store.sessions(before, 11)
                    withContext(Dispatchers.Main.immediate) {
                        if (activity.isDestroyed) return@withContext
                        historyCursor = before
                        val size = minOf(10, records.size)
                        historyTargets.clear()
                        val items =
                            (0 until size).map { index ->
                                val item = records[index].session!!
                                val token = UUID.randomUUID().toString()
                                historyTargets[token] = item.id
                                HistoricalImportHistoryItem(
                                    token = token,
                                    label =
                                        activity.getString(
                                            R.string.import_history_item,
                                            dates(item),
                                            activity.getString(phaseLabel(item.phase)),
                                        ),
                                )
                            }
                        historyState = HistoricalImportHistoryUiState(items)
                        if (size > 0) {
                            val lastVisible = records[size - 1]
                            nextHistoryCursor = if (records.size > size) lastVisible.cursor else -1
                        } else {
                            nextHistoryCursor = -1
                        }
                        buttons()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun openHistorySession(token: String) {
        val id = historyTargets[token] ?: return
        loadSession(id)
    }

    private suspend fun showFailure() {
        withContext(Dispatchers.Main.immediate) {
            if (activity.isDestroyed) return@withContext
            setFeedback(R.string.import_failed)
            buttons()
        }
    }

    fun pause() {
        running = false
        confirmation = null
        buttons()
    }

    fun saveState(): Bundle =
        Bundle().apply {
            putString("zone", zone)
            putString("session", currentId)
            putString("from", filters.fromDate)
            putString("to", filters.toDate)
            putLong("calendar", selected)
            putBoolean("incoming", filters.incoming)
            putBoolean("outgoing", filters.outgoing)
            putBoolean("missed", filters.missed)
        }
}
