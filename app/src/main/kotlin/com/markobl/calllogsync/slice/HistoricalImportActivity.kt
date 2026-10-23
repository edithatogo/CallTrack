package com.markobl.calllogsync.slice

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.markobl.calllogsync.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.HashMap
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors

/** Foreground bounded runs; opening/resuming never confirms historical delivery. */
class HistoricalImportActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false
    private var busy = false
    private var currentId: String? = null
    private var zone: String? = null
    private var session: HistoricalImport.Session? = null
    private var destinationState by mutableStateOf(HistoricalImportDestinationUiState())
    private var filters by mutableStateOf(HistoricalImportFiltersUiState())
    private var actionState by mutableStateOf(HistoricalImportActionState())
    private var summary: ComposeView? = null
    private var feedback: ComposeView? = null
    private var summaryText by mutableStateOf("")
    private var feedbackText by mutableStateOf("")
    private var historyState by mutableStateOf(HistoricalImportHistoryUiState())
    private val historyTargets = HashMap<String, String>()
    private val destinationTargets = HashMap<String, Long>()
    private var selectedDestinationToken: String? = null
    private var selected = -1L
    private var historyCursor = -1L
    private var renderVersion = 0L
    private val historyBack = ArrayList<Long>()
    private var nextHistoryCursor = -1L

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        zone =
            if (saved == null) ZoneId.systemDefault().id
            else saved.getString("zone", ZoneId.systemDefault().id)
        currentId = saved?.getString("session")
        filters =
            HistoricalImportFiltersUiState(
                fromDate =
                    if (saved == null) LocalDate.now(ZoneId.of(zone)).minusDays(7).toString()
                    else saved.getString("from") ?: "",
                toDate =
                    if (saved == null) LocalDate.now(ZoneId.of(zone)).plusDays(1).toString()
                    else saved.getString("to") ?: "",
                incoming = saved == null || saved.getBoolean("incoming"),
                outgoing = saved == null || saved.getBoolean("outgoing"),
                missed = saved == null || saved.getBoolean("missed"),
            )
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro = TextView(this)
        intro.text = getString(R.string.import_intro, zone)
        layout.addView(intro)
        val accessPanel =
            ComposeView(this).apply {
                id = R.id.import_access_panel
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    HistoricalImportAccessPanel(
                        label = stringResource(R.string.auto_access),
                        onRequestAccess = {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR,
                                ),
                                9,
                            )
                        },
                    )
                }
            }
        layout.addView(accessPanel)
        val destinationPicker =
            ComposeView(this).apply {
                id = R.id.import_calendar_picker
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    HistoricalImportDestinationPanel(
                        state = destinationState,
                        chooseLabel = stringResource(R.string.slice_choose),
                        unavailableLabel = stringResource(R.string.auto_destination_unavailable),
                        onSelect = ::selectDestination,
                    )
                }
            }
        layout.addView(destinationPicker)
        val filtersPanel =
            ComposeView(this).apply {
                id = R.id.import_filters
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
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
            }
        layout.addView(filtersPanel)
        val createActions =
            ComposeView(this).apply {
                id = R.id.import_actions
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    HistoricalImportCreateActionPanel(
                        state = actionState,
                        label = stringResource(R.string.import_create),
                        onCreate = ::startPreview,
                    )
                }
            }
        layout.addView(createActions)
        summaryText = getString(R.string.import_none)
        summary =
            ComposeView(this).apply {
                id = R.id.import_summary
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { HistoricalImportSummaryText(summaryText) }
            }
        layout.addView(summary)
        val runActions =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
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
            }
        layout.addView(runActions)
        feedback =
            ComposeView(this).apply {
                id = R.id.import_feedback
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { HistoricalImportFeedbackText(feedbackText) }
            }
        layout.addView(feedback)
        val historyPanel =
            ComposeView(this).apply {
                id = R.id.import_history
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    HistoricalImportHistoryPanel(
                        state = historyState,
                        title = stringResource(R.string.import_history),
                        emptyMessage = stringResource(R.string.import_empty_history),
                        onOpen = ::openHistorySession,
                    )
                }
            }
        layout.addView(historyPanel)
        val historyNavigation =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
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
            }
        layout.addView(historyNavigation)
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
        buttons()
        loadCalendars(if (saved == null) -1 else saved.getLong("calendar", -1))
        loadHistory(-1)
        if (currentId != null) loadSession(currentId)
    }

    private fun setFeedback(message: Int?) {
        feedbackText = message?.let(::getString) ?: ""
    }

    private fun access(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED && calendarAccess()

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        val value = session
        val hasCallTypes = filters.incoming || filters.outgoing || filters.missed
        actionState =
            HistoricalImportActionState(
                createEnabled = !busy && selected >= 0 && access() && hasCallTypes,
                continueEnabled =
                    !busy &&
                        value != null &&
                        (value.phase == HistoricalImport.Phase.BUILDING ||
                            value.phase == HistoricalImport.Phase.CONFIRMED),
                confirmEnabled =
                    !busy && value != null && value.phase == HistoricalImport.Phase.PREVIEW,
                stopEnabled = running,
                cancelEnabled =
                    value != null &&
                        value.phase != HistoricalImport.Phase.COMPLETE &&
                        value.phase != HistoricalImport.Phase.CANCELLED,
                newerEnabled = !busy && historyBack.isNotEmpty(),
                olderEnabled = !busy && nextHistoryCursor > 0,
            )
        destinationState = destinationState.copy(enabled = !busy && calendarAccess())
    }

    private fun loadCalendars(preferred: Long) {
        executor.execute {
            try {
                val loaded =
                    if (calendarAccess()) AndroidCalendarSink(applicationContext).choices()
                    else Collections.emptyList()
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    destinationTargets.clear()
                    val options =
                        loaded.map { choice ->
                            val token = UUID.randomUUID().toString()
                            destinationTargets[token] = choice.id
                            HistoricalImportDestinationOption(
                                token,
                                choice.name ?: getString(R.string.auto_destination_unavailable),
                            )
                        }
                    selectedDestinationToken =
                        options.firstOrNull { destinationTargets[it.token] == preferred }?.token
                    selected = selectedDestinationToken?.let(destinationTargets::get) ?: -1L
                    destinationState =
                        HistoricalImportDestinationUiState(
                            options = options,
                            selectedToken = selectedDestinationToken,
                            enabled = !busy && calendarAccess(),
                        )
                    if (session != null) render()
                    buttons()
                }
            } catch (failed: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun startPreview() {
        if (busy || selected < 0) return
        val range =
            try {
                ImportRange.parse(filters.fromDate, filters.toDate, zone)
            } catch (invalid: RuntimeException) {
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
        )
            AlertDialog.Builder(this)
                .setMessage(R.string.import_another)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_create) { _, _ -> start.run() }
                .show()
        else start.run()
    }

    private fun selectDestination(token: String) {
        val id = destinationTargets[token] ?: return
        if (destinationState.options.none { it.token == token }) return
        selectedDestinationToken = token
        selected = id
        destinationState = destinationState.copy(selectedToken = token)
        buttons()
    }

    private fun confirmImport() {
        val value = session
        if (busy || value == null || value.phase != HistoricalImport.Phase.PREVIEW) return
        val id = currentId
        AlertDialog.Builder(this)
            .setTitle(R.string.import_confirm)
            .setMessage(
                getString(
                    R.string.import_confirm_text,
                    value.count,
                    destinationName(value),
                    dates(value),
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.import_confirm) { _, _ ->
                running = true
                perform({ importer, _ -> importer.confirm(id) }, true)
            }
            .show()
    }

    private fun cancelImport() {
        if (session == null) return
        val id = currentId
        AlertDialog.Builder(this)
            .setMessage(R.string.import_cancel_text)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.import_cancel) { _, _ ->
                running = false
                perform({ importer, _ -> importer.cancel(id) }, false)
            }
            .show()
    }

    private fun step() {
        if (!running || busy || currentId == null) return
        val id = currentId
        perform(
            { importer, store ->
                val latest = store.get(id) ?: throw IllegalArgumentException()
                when (latest.phase) {
                    HistoricalImport.Phase.BUILDING -> importer.preview(id, 20)
                    HistoricalImport.Phase.CONFIRMED -> importer.deliver(id, 20) { running }
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
        executor.execute {
            try {
                SqliteImportStore(applicationContext).use { store ->
                    SqliteLedger(applicationContext).use { ledger ->
                        ledger.expirePayloads(50)
                        val importer =
                            HistoricalImport(
                                store,
                                AndroidCallLogSource(applicationContext),
                                ledger.outbox(),
                                DurableDelivery(
                                    ledger.outbox(),
                                    AndroidCalendarSink(applicationContext),
                                ),
                            )
                        val result = action.run(importer, store)
                        runOnUiThread {
                            if (isDestroyed || ticket != renderVersion) return@runOnUiThread
                            busy = false
                            val previous = session
                            if (
                                currentId == null ||
                                    currentId != result.id ||
                                    previous == null ||
                                    result.revision >= previous.revision
                            ) {
                                currentId = result.id
                                session = result
                                render()
                            }
                            if (result.stopped != null) {
                                running = false
                                setFeedback(R.string.import_blocked)
                            } else setFeedback(null)
                            buttons()
                            if (
                                follow &&
                                    running &&
                                    (result.phase == HistoricalImport.Phase.BUILDING ||
                                        result.phase == HistoricalImport.Phase.CONFIRMED)
                            )
                                summary!!.post { step() }
                            else {
                                running = false
                                buttons()
                                loadHistory(historyCursor)
                            }
                        }
                    }
                }
            } catch (failed: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed || ticket != renderVersion) return@runOnUiThread
                    running = false
                    busy = false
                    setFeedback(R.string.import_failed)
                    buttons()
                }
            }
        }
    }

    private fun loadSession(id: String?) {
        if (busy) return
        running = false
        currentId = id
        session = null
        perform({ _, store -> store.get(id) ?: throw IllegalArgumentException() }, false)
    }

    private fun dates(s: HistoricalImport.Session): String {
        val z = TimeZone.getTimeZone(s.timezone).toZoneId()
        return "${Instant.ofEpochMilli(s.from).atZone(z).toLocalDate()} → ${Instant.ofEpochMilli(s.to).atZone(z).toLocalDate()} (${s.timezone})"
    }

    private fun destinationName(value: HistoricalImport.Session): String? {
        destinationState.options
            .firstOrNull { destinationTargets[it.token] == value.calendar }
            ?.let {
                return it.label
            }
        return getString(R.string.auto_destination_unavailable)
    }

    private fun typeNames(value: HistoricalImport.Session): String {
        val names = ArrayList<String>()
        if (value.incoming) names.add(getString(R.string.auto_incoming))
        if (value.outgoing) names.add(getString(R.string.auto_outgoing))
        if (value.missed) names.add(getString(R.string.auto_missed))
        return android.text.TextUtils.join(", ", names)
    }

    private fun render() {
        val value = session!!
        summaryText =
            getString(
                R.string.import_progress,
                dates(value),
                getString(phaseLabel(value.phase)),
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
        executor.execute {
            try {
                SqliteImportStore(applicationContext).use { store ->
                    val records = store.sessions(before, 11)
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
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
                                        getString(
                                            R.string.import_history_item,
                                            dates(item),
                                            getString(phaseLabel(item.phase)),
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
            } catch (failed: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun openHistorySession(token: String) {
        val id = historyTargets[token] ?: return
        loadSession(id)
    }

    private fun showFailure() {
        runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            setFeedback(R.string.import_failed)
            buttons()
        }
    }

    public override fun onPause() {
        running = false
        super.onPause()
    }

    public override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("zone", zone)
        out.putString("session", currentId)
        out.putString("from", filters.fromDate)
        out.putString("to", filters.toDate)
        out.putLong("calendar", selected)
        out.putBoolean("incoming", filters.incoming)
        out.putBoolean("outgoing", filters.outgoing)
        out.putBoolean("missed", filters.missed)
    }

    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        loadCalendars(selected)
        buttons()
    }

    public override fun onDestroy() {
        running = false
        destinationTargets.clear()
        executor.shutdown()
        super.onDestroy()
    }
}
