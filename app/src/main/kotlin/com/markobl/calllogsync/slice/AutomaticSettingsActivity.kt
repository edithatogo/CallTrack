package com.markobl.calllogsync.slice

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
    BasicText(text, modifier = modifier)
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
    private var statePanel: ComposeView? = null
    private var countsPanel: ComposeView? = null
    private var feedbackPanel: ComposeView? = null
    private var lastPanel: ComposeView? = null
    private var stateText = ""
    private var countsText = ""
    private var lastText = ""
    private var feedbackText = ""
    private var choices: List<AndroidCalendarSink.Choice> = Collections.emptyList()
    private var selected = -1L
    private var busy = false
    private var active = false
    private var initialised = false
    private var reloadRequested = false
    private var reloadReconcile = false
    private var callTypesState by mutableStateOf(AutomaticCallTypesUiState())
    private var callTypesEnabled by mutableStateOf(true)
    private var destinationUiState by mutableStateOf(AutomaticDestinationUiState())
    private var actionsUiState by mutableStateOf(AutomaticSettingsActionsUiState())
    private var restoredCallTypes = false
    private var destinationOptionTokens: List<String> = Collections.emptyList()

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        callTypesState =
            AutomaticCallTypesUiState(
                incoming = saved?.getBoolean(STATE_INCOMING, true) ?: true,
                outgoing = saved?.getBoolean(STATE_OUTGOING, true) ?: true,
                missed = saved?.getBoolean(STATE_MISSED, true) ?: true,
            )
        restoredCallTypes = saved != null
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro =
            ComposeView(this).apply {
                id = R.id.auto_intro_panel
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { AutomaticSettingsIntroPanel(text = getString(R.string.auto_intro)) }
            }
        layout.addView(intro)
        val access =
            ComposeView(this).apply {
                id = R.id.auto_permissions
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AutomaticSettingsAccessPanel(
                        label = getString(R.string.auto_access),
                        onRequestAccess = {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_PHONE_STATE,
                                ),
                                7,
                            )
                        },
                    )
                }
            }
        layout.addView(access)
        val destinationPanel =
            ComposeView(this).apply {
                id = R.id.auto_calendar_picker
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AutomaticDestinationPanel(
                        state = destinationUiState,
                        chooseLabel = getString(R.string.slice_choose),
                        onSelect = ::selectDestination,
                    )
                }
            }
        layout.addView(destinationPanel)
        val callTypes =
            ComposeView(this).apply {
                id = R.id.auto_call_types
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
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
            }
        layout.addView(callTypes)
        val actionsPanel =
            ComposeView(this).apply {
                id = R.id.auto_actions
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AutomaticSettingsActionsPanel(
                        state = actionsUiState,
                        enableLabel = getString(R.string.auto_enable),
                        pauseLabel = getString(R.string.auto_pause),
                        refreshLabel = getString(R.string.auto_refresh),
                        onEnable = ::confirmEnable,
                        onPause = { change { AutomaticScheduling.pause(applicationContext) } },
                        onRefresh = { load(false) },
                    )
                }
            }
        layout.addView(actionsPanel)
        statePanel = statusView(layout, R.id.auto_state)
        countsPanel = statusView(layout, R.id.auto_counts)
        feedbackPanel = statusView(layout, R.id.auto_feedback)
        lastPanel = statusView(layout, R.id.auto_last)
        val imports = Button(this)
        imports.id = R.id.open_imports
        imports.setText(R.string.import_open)
        imports.setOnClickListener {
            startActivity(Intent(this, HistoricalImportActivity::class.java))
        }
        layout.addView(imports)
        val recovery = Button(this)
        recovery.id = R.id.open_recovery
        recovery.setText(R.string.recovery_open)
        recovery.setOnClickListener {
            startActivity(Intent(this, ReceiptRecoveryActivity::class.java))
        }
        layout.addView(recovery)
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
        renderStatus()
        buttons()
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
                        selected >= 0 &&
                        calendarAccess() &&
                        callAccess() &&
                        (callTypesState.incoming ||
                            callTypesState.outgoing ||
                            callTypesState.missed),
                pauseEnabled = !busy,
                refreshEnabled = true,
            )
        destinationUiState = destinationUiState.copy(enabled = !busy && calendarAccess())
        callTypesEnabled = !busy
    }

    private fun confirmEnable() {
        if (selected < 0 || busy) return
        val calendar = selected
        val inCalls = callTypesState.incoming
        val outCalls = callTypesState.outgoing
        val missedCalls = callTypesState.missed
        AlertDialog.Builder(this)
            .setTitle(R.string.auto_enable)
            .setMessage(R.string.auto_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.auto_enable) { _, _ ->
                change {
                    kotlin.check(AndroidCalendarSink(applicationContext).writable(calendar)) {
                        "Destination unavailable"
                    }
                    AutomaticScheduling.activate(
                        applicationContext,
                        calendar,
                        TimeZone.getDefault().id,
                        inCalls,
                        outCalls,
                        missedCalls,
                    )
                }
            }
            .show()
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
                } catch (failed: RuntimeException) {
                    false
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                feedbackText =
                    getString(if (done) R.string.auto_action_saved else R.string.auto_action_failed)
                renderStatus()
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
                if (reconcile && settings != null && settings.enabled)
                    AutomaticScheduling.reconcile(applicationContext)
                val loaded =
                    if (calendarAccess()) AndroidCalendarSink(applicationContext).choices()
                    else Collections.emptyList()
                val summary =
                    SqliteLedger(applicationContext).use { ledger ->
                        ledger.expirePayloads(50)
                        ledger.counts()
                    }
                val worker =
                    if (settings == null) null
                    else AndroidWorkStatusStore(applicationContext).read(settings.generation)
                val code = worker?.code?.name
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    active = settings != null && settings.enabled
                    var activeName: String? = getString(R.string.auto_destination_unavailable)
                    if (settings != null)
                        for (choice in loaded) if (choice.id == settings.calendar)
                            activeName = choice.name
                    stateText =
                        if (active) getString(R.string.auto_enabled, activeName)
                        else getString(R.string.auto_disabled)
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
                            getString(workerMessage(code)),
                            getString(R.string.auto_failure_count, worker?.failures ?: 0L),
                        )
                    choices = loaded
                    destinationOptionTokens = loaded.map { java.util.UUID.randomUUID().toString() }
                    renderStatus()
                    val preferred =
                        if (initialised) selected
                        else
                            settings?.calendar
                                ?: getSharedPreferences("slice", MODE_PRIVATE)
                                    .getLong("calendar", -1)
                    val preferredIndex = loaded.indexOfFirst { it.id == preferred }
                    selected = if (preferredIndex < 0) -1 else loaded[preferredIndex].id
                    destinationUiState =
                        AutomaticDestinationUiState(
                            options =
                                loaded.mapIndexed { index, choice ->
                                    AutomaticDestinationOption(
                                        destinationOptionTokens[index],
                                        choice.name
                                            ?: getString(R.string.auto_destination_unavailable),
                                    )
                                },
                            selectedToken =
                                if (preferredIndex < 0) null
                                else destinationOptionTokens[preferredIndex],
                            enabled = !busy && calendarAccess(),
                        )
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
            } catch (unavailable: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    stateText = getString(R.string.auto_action_failed)
                    renderStatus()
                    buttons()
                    reloadIfRequested()
                }
            }
        }
    }

    private fun reloadIfRequested() {
        if (reloadRequested) {
            val reconcile = reloadReconcile
            reloadRequested = false
            reloadReconcile = false
            load(reconcile)
        }
    }

    private fun selectDestination(token: String) {
        val index = destinationOptionTokens.indexOf(token)
        if (index !in choices.indices || busy) return
        selected = choices[index].id
        destinationUiState = destinationUiState.copy(selectedToken = token)
        buttons()
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

    private fun statusView(parent: LinearLayout, id: Int): ComposeView =
        ComposeView(this).apply {
            this.id = id
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            parent.addView(this)
        }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_INCOMING, callTypesState.incoming)
        outState.putBoolean(STATE_OUTGOING, callTypesState.outgoing)
        outState.putBoolean(STATE_MISSED, callTypesState.missed)
        super.onSaveInstanceState(outState)
    }

    private fun renderStatus() {
        statePanel?.setContent { AutomaticStatusLine(stateText, "auto_state", true) }
        countsPanel?.setContent { AutomaticStatusLine(countsText, "auto_counts") }
        feedbackPanel?.setContent { AutomaticStatusLine(feedbackText, "auto_feedback") }
        lastPanel?.setContent { AutomaticStatusLine(lastText, "auto_last") }
    }

    @Deprecated("Required to preserve the existing permission callback contract")
    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        initialised = false
        load(false)
    }

    public override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val STATE_INCOMING = "automatic-call-types-incoming"
        const val STATE_OUTGOING = "automatic-call-types-outgoing"
        const val STATE_MISSED = "automatic-call-types-missed"
    }
}
