package com.markobl.calllogsync.slice

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.markobl.calllogsync.R
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

/** Existing-effect checks remain separate from explicitly confirmed never-attempted insertion. */
class ReceiptRecoveryActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val back = ArrayList<SqliteLedger.RecoveryCursor?>()
    private var current: SqliteLedger.RecoveryCursor? = null
    private var next: SqliteLedger.RecoveryCursor? = null
    private var rows: LinearLayout? = null
    private var emptyState by mutableStateOf(false)
    private var feedback: ComposeView? = null
    private var feedbackText by mutableStateOf("")
    private var cards by mutableStateOf<List<RecoveryCardUiState>>(emptyList())
    private var visibleRecords: Map<Long, Pair<DurableDelivery.Intent, String?>> = emptyMap()
    private var nextCardToken = 0L
    private var navigationState by mutableStateOf(RecoveryNavigationUiState())
    private var busy = false
    @Volatile private var resumed = false
    @Volatile private var approvalSession = Any()
    private var version = 0L

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro =
            ComposeView(this).apply {
                id = R.id.recovery_intro_panel
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryCopyPanel(
                        text = stringResource(R.string.recovery_intro),
                        tag = "recovery_intro_text",
                    )
                }
            }
        layout.addView(intro)
        val access = button(layout, R.string.recovery_access)
        access.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 11)
        }
        val actions =
            ComposeView(this).apply {
                id = R.id.recovery_refresh
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryRefreshPanel(
                        busy = navigationState.busy,
                        refreshLabel = stringResource(R.string.recovery_refresh),
                        onRefresh = {
                            if (!busy) {
                                back.clear()
                                load(null)
                            }
                        },
                    )
                }
            }
        layout.addView(actions)
        val retentionCopy =
            ComposeView(this).apply {
                id = R.id.recovery_retention_copy
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryCopyPanel(
                        text = stringResource(R.string.retention_policy),
                        tag = "recovery_retention_copy_text",
                    )
                }
            }
        layout.addView(retentionCopy)
        val expire =
            ComposeView(this).apply {
                id = R.id.recovery_retention
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryRetentionPanel(
                        busy = navigationState.busy,
                        label = stringResource(R.string.retention_run),
                        onExpire = ::expireOldPayloads,
                    )
                }
            }
        layout.addView(expire)
        feedback =
            ComposeView(this).apply {
                id = R.id.recovery_feedback
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { RecoveryStatusPanel(feedbackText) }
            }
        layout.addView(feedback)
        rows = LinearLayout(this)
        rows!!.id = R.id.recovery_rows
        rows!!.orientation = LinearLayout.VERTICAL
        val emptyPanel =
            ComposeView(this).apply {
                id = R.id.recovery_empty
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryEmptyPanel(
                        visible = emptyState,
                        message = stringResource(R.string.recovery_empty),
                    )
                }
            }
        rows!!.addView(emptyPanel)
        val cardsPanel =
            ComposeView(this).apply {
                id = R.id.recovery_cards
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryCardsPanel(
                        cards = cards,
                        busy = navigationState.busy,
                        checkLabel = stringResource(R.string.recovery_check),
                        suppressLabel = stringResource(R.string.recovery_suppress),
                        reapproveLabel = stringResource(R.string.reapproval_open),
                        openEventLabel = stringResource(R.string.recovery_open_event),
                        onCheck = { token ->
                            visibleRecords[token]?.let { action(it.first, false) }
                        },
                        onSuppress = { token ->
                            visibleRecords[token]?.let { confirmSuppression(it.first) }
                        },
                        onReapprove = { token ->
                            visibleRecords[token]?.let { confirmReapproval(it.first, it.second) }
                        },
                        onOpenEvent = { token ->
                            visibleRecords[token]?.let { openEvent(it.first) }
                        },
                    )
                }
            }
        rows!!.addView(cardsPanel)
        layout.addView(rows)
        val navigation =
            ComposeView(this).apply {
                id = R.id.recovery_navigation
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    RecoveryNavigationPanel(
                        state = navigationState,
                        previousLabel = stringResource(R.string.recovery_previous),
                        moreLabel = stringResource(R.string.recovery_more),
                        onPrevious = {
                            if (!busy && back.isNotEmpty()) load(back.removeAt(back.size - 1))
                        },
                        onMore = {
                            if (!busy && next != null) {
                                back.add(current)
                                load(next)
                            }
                        },
                    )
                }
            }
        layout.addView(navigation)
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
        load(null)
    }

    private fun button(layout: LinearLayout, label: Int): Button {
        val result = Button(this)
        result.setText(label)
        layout.addView(result)
        return result
    }

    private fun load(cursor: SqliteLedger.RecoveryCursor?) {
        if (busy) return
        busy = true
        val ticket = ++version
        navigation()
        executor.execute {
            try {
                SqliteLedger(applicationContext).use { ledger ->
                    ledger.expirePayloads(50)
                    val page = ledger.recoveryPage(cursor, 11)
                    val labels = HashMap<Long, String>()
                    val sink = AndroidCalendarSink(applicationContext)
                    for (item in page) {
                        val intent = item.intent!!
                        if (!labels.containsKey(intent.calendar)) {
                            var label = getString(R.string.recovery_calendar_hidden)
                            if (
                                checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                                    PackageManager.PERMISSION_GRANTED
                            ) {
                                label =
                                    try {
                                        sink.calendarLabel(intent.calendar)
                                            ?: getString(R.string.auto_destination_unavailable)
                                    } catch (unavailable: RuntimeException) {
                                        getString(R.string.recovery_calendar_unverified)
                                    }
                            }
                            labels[intent.calendar] = label
                        }
                    }
                    runOnUiThread {
                        if (isDestroyed || ticket != version) return@runOnUiThread
                        busy = false
                        current = cursor
                        val size = minOf(10, page.size)
                        val records =
                            (0 until size).map { index ->
                                val intent = page[index].intent!!
                                intent to labels[intent.calendar]
                            }
                        val keyedRecords = records.map { nextCardToken++ to it }
                        visibleRecords = keyedRecords.toMap()
                        cards =
                            keyedRecords.map { (token, record) ->
                                cardState(token, record.first, record.second)
                            }
                        emptyState = size == 0
                        next = if (page.size > 10) page[9].cursor else null
                        navigation()
                    }
                }
            } catch (failed: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed || ticket != version) return@runOnUiThread
                    busy = false
                    setFeedback(R.string.recovery_failed)
                    navigation()
                }
            }
        }
    }

    private fun navigation() {
        navigationState =
            RecoveryNavigationUiState(
                busy = busy,
                previousEnabled = back.isNotEmpty(),
                moreEnabled = next != null,
            )
    }

    private fun setFeedback(message: Int) {
        feedbackText = getString(message)
    }

    private fun setFeedback(text: String) {
        feedbackText = text
    }

    private fun cardState(
        token: Long,
        intent: DurableDelivery.Intent,
        calendar: String?,
    ): RecoveryCardUiState {
        var source = getString(R.string.recovery_source_unknown)
        val facts = intent.facts
        if (facts != null) {
            val type =
                when (facts.type) {
                    1 -> R.string.auto_incoming
                    2 -> R.string.auto_outgoing
                    3 -> R.string.auto_missed
                    else -> R.string.recovery_call
                }
            source =
                getString(
                    R.string.recovery_source,
                    getString(type),
                    DateFormat.getDateTimeInstance().format(Date(facts.start)),
                    facts.seconds,
                )
        }
        return RecoveryCardUiState(
            token = token,
            detail =
                getString(
                    R.string.recovery_item,
                    source,
                    calendar,
                    getString(stateLabel(intent.state)),
                ),
            checkEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            suppressEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            reapproveVisible = ReadyReapproval.eligible(intent),
            openEventVisible = intent.eventId >= 0,
        )
    }

    private fun confirmSuppression(intent: DurableDelivery.Intent) {
        if (busy) return
        AlertDialog.Builder(this)
            .setMessage(R.string.recovery_suppress_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.recovery_suppress) { _, _ -> action(intent, true) }
            .show()
    }

    private fun openEvent(intent: DurableDelivery.Intent) {
        if (intent.eventId < 0) return
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, intent.eventId),
                )
            )
        } catch (unavailable: RuntimeException) {
            setFeedback(R.string.recovery_open_failed)
        }
    }

    private fun expireOldPayloads() {
        if (busy) return
        busy = true
        navigation()
        executor.execute {
            try {
                SqliteLedger(applicationContext).use { ledger ->
                    val count = ledger.expirePayloads(50)
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        busy = false
                        setFeedback(getString(R.string.retention_result, count))
                        load(current)
                    }
                }
            } catch (failed: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    setFeedback(R.string.recovery_failed)
                    navigation()
                }
            }
        }
    }

    private fun confirmReapproval(intent: DurableDelivery.Intent, calendar: String?) {
        if (busy || !ReadyReapproval.eligible(intent)) return
        if (
            checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                12,
            )
            setFeedback(R.string.reapproval_tap_again)
            return
        }
        val payload = intent.payload!!
        val preview =
            getString(
                R.string.reapproval_preview,
                calendar,
                payload.title,
                payload.description,
                DateFormat.getDateTimeInstance().format(Date(payload.start)),
                DateFormat.getDateTimeInstance().format(Date(payload.end)),
            )
        AlertDialog.Builder(this)
            .setTitle(R.string.reapproval_open)
            .setMessage(preview)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.reapproval_confirm) { _, _ -> reapprove(intent) }
            .show()
    }

    private fun reapprove(expected: DurableDelivery.Intent) {
        if (busy) return
        busy = true
        val approvedSession = approvalSession
        navigation()
        executor.execute {
            val outcome =
                try {
                    SqliteLedger(applicationContext).use { ledger ->
                        ReadyReapproval(ledger.outbox(), AndroidCalendarSink(applicationContext))
                            .approve(expected) { resumed && approvalSession === approvedSession }
                    }
                } catch (failed: RuntimeException) {
                    DurableDelivery.Result.FAILED
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                val label =
                    when (outcome) {
                        DurableDelivery.Result.SAVED,
                        DurableDelivery.Result.RECOVERED,
                        DurableDelivery.Result.ALREADY_LOGGED -> R.string.reapproval_verified
                        DurableDelivery.Result.PERMISSION_REQUIRED -> R.string.reapproval_tap_again
                        DurableDelivery.Result.DESTINATION_UNAVAILABLE ->
                            R.string.slice_calendar_unavailable
                        DurableDelivery.Result.SOURCE_CHANGED,
                        DurableDelivery.Result.REVIEW_REQUIRED -> R.string.reapproval_changed
                        DurableDelivery.Result.PAUSED -> R.string.reapproval_paused
                        else -> R.string.reapproval_failed
                    }
                setFeedback(label)
                load(current)
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        resumed = true
    }

    public override fun onPause() {
        resumed = false
        approvalSession = Any() // Returning cannot revive an earlier queued approval.
        super.onPause()
    }

    private fun action(intent: DurableDelivery.Intent, suppress: Boolean) {
        if (busy) return
        busy = true
        navigation()
        executor.execute {
            val outcome =
                try {
                    SqliteLedger(applicationContext).use { ledger ->
                        val sink = AndroidCalendarSink(applicationContext)
                        val recovery =
                            ReceiptRecovery(
                                ledger.outbox(),
                                object : ReceiptRecovery.ExistingEvents {
                                    override fun read(id: Long): SliceEngine.StoredEvent? =
                                        sink.read(id)

                                    override fun find(
                                        calendar: Long,
                                        marker: String?,
                                    ): List<SliceEngine.StoredEvent?> = sink.find(calendar, marker)
                                },
                            )
                        if (suppress) recovery.suppress(intent.key, intent.calendar)
                        else recovery.reconcile(intent.key, intent.calendar)
                    }
                } catch (failed: RuntimeException) {
                    ReceiptRecovery.Result.FAILED
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                setFeedback(resultLabel(outcome))
                load(current)
            }
        }
    }

    private fun stateLabel(state: DurableDelivery.State): Int =
        when (state) {
            DurableDelivery.State.READY -> R.string.recovery_ready
            DurableDelivery.State.EXPIRED_READY -> R.string.recovery_expired
            DurableDelivery.State.IN_FLIGHT -> R.string.recovery_inflight
            DurableDelivery.State.ATTEMPTED -> R.string.recovery_attempted
            DurableDelivery.State.CONFIRMED -> R.string.recovery_confirmed
            DurableDelivery.State.REVIEW -> R.string.recovery_review
            DurableDelivery.State.SUPPRESSED -> R.string.recovery_suppressed
            else -> R.string.recovery_legacy
        }

    private fun resultLabel(result: ReceiptRecovery.Result): Int =
        when (result) {
            ReceiptRecovery.Result.VERIFIED -> R.string.recovery_verified
            ReceiptRecovery.Result.NEVER_ATTEMPTED -> R.string.recovery_never_attempted
            ReceiptRecovery.Result.SUPPRESSED -> R.string.recovery_suppressed_result
            ReceiptRecovery.Result.PERMISSION_REQUIRED -> R.string.recovery_permission
            ReceiptRecovery.Result.REVIEW_REQUIRED -> R.string.recovery_review_result
            ReceiptRecovery.Result.NOT_FOUND -> R.string.recovery_not_found
            else -> R.string.recovery_failed
        }

    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (!busy) load(current)
    }

    public override fun onDestroy() {
        resumed = false
        version++
        executor.shutdown()
        super.onDestroy()
    }
}
