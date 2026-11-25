package com.markobl.calllogsync.slice

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.markobl.calllogsync.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RECOVERY_PAGE_SIZE = 10

/** Existing-effect checks remain separate from explicitly confirmed never-attempted insertion. */
@AndroidEntryPoint
class ReceiptRecoveryActivity : ComponentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCallTrackNavigation(CallTrackRoute.Recovery)
    }
}

@Composable
internal fun ReceiptRecoveryScreen(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    val state = remember(activity, scope) { ReceiptRecoveryState(activity, scope) }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            state.permissionsChanged()
        }
    LifecycleResumeEffect(state) {
        state.resume()
        onPauseOrDispose { state.pause() }
    }
    state.Content { permissions.launch(it) }
}

private class ReceiptRecoveryState(
    private val activity: ComponentActivity,
    private val scope: CoroutineScope,
) {
    private val worker = Dispatchers.IO.limitedParallelism(1)
    private val back = ArrayList<SqliteLedger.RecoveryCursor?>()
    private var current: SqliteLedger.RecoveryCursor? = null
    private var next: SqliteLedger.RecoveryCursor? = null
    private var emptyState by mutableStateOf(false)
    private var feedbackText by mutableStateOf("")
    private var retentionFeedback by mutableStateOf("")
    private var confirmation by mutableStateOf<Confirmation?>(null)
    private var hasCalendarReadAccess by mutableStateOf(false)
    private var cards by mutableStateOf<List<RecoveryCardUiState>>(emptyList())
    private var visibleRecords: Map<Long, Pair<DurableDelivery.Intent, String?>> = emptyMap()
    private var nextCardToken = 0L
    private var navigationState by mutableStateOf(RecoveryNavigationUiState())
    private var busy = false

    @Volatile private var resumed = false

    @Volatile private var approvalSession = Any()
    private var version = 0L
    private var opened = false

    fun permissionsChanged() {
        if (!busy) load(current)
    }

    @Composable
    fun Content(requestAccess: (Array<String>) -> Unit) {
        RecoveryOverview { requestAccess(arrayOf(Manifest.permission.READ_CALENDAR)) }
        RecoveryReceipts(requestAccess)
        RecoveryRetention()
        confirmation?.let { ConfirmationDialog(it) { confirmation = null } }
    }

    @Composable
    private fun RecoveryOverview(requestReadAccess: () -> Unit) {
        RecoveryAccessPanel(
            label = stringResource(R.string.recovery_access),
            hasCalendarAccess = hasCalendarReadAccess,
            onRequestAccess = requestReadAccess,
        )
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

    @Composable
    private fun RecoveryReceipts(requestAccess: (Array<String>) -> Unit) {
        SectionHeading(stringResource(R.string.recovery_records_heading))
        RecoveryStatusPanel(feedbackText)
        RecoveryEmptyPanel(
            visible = emptyState,
            message = stringResource(R.string.recovery_empty),
        )
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
                visibleRecords[token]?.let { confirmReapproval(it.first, it.second, requestAccess) }
            },
            onOpenEvent = { token ->
                visibleRecords[token]?.let { openEvent(it.first) }
            },
        )
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

    @Composable
    private fun RecoveryRetention() {
        RecoveryCopyPanel(
            text = stringResource(R.string.recovery_intro),
            tag = "recovery_intro_text",
        )
        SectionHeading(stringResource(R.string.recovery_retention_heading))
        RecoveryCopyPanel(
            text = stringResource(R.string.retention_policy),
            tag = "recovery_retention_copy_text",
        )
        RecoveryRetentionPanel(
            busy = navigationState.busy,
            label = stringResource(R.string.retention_run),
            onExpire = ::expireOldPayloads,
        )
        RecoveryStatusPanel(retentionFeedback, tag = "recovery_retention_feedback")
    }

    private fun load(cursor: SqliteLedger.RecoveryCursor?) {
        if (busy) return
        busy = true
        val ticket = ++version
        navigation()
        scope.launch(worker) {
            try {
                SqliteLedger(activity.applicationContext).use { ledger ->
                    ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                    val page = ledger.recoveryPage(cursor, RECOVERY_PAGE_SIZE + 1)
                    val labels = readCalendarLabels(page)
                    withContext(Dispatchers.Main.immediate) {
                        if (activity.isDestroyed || ticket != version) return@withContext
                        busy = false
                        current = cursor
                        val size = minOf(RECOVERY_PAGE_SIZE, page.size)
                        val records =
                            (0 until size).map { index ->
                                val intent = page[index].intent!!
                                intent to labels[intent.calendar]
                            }
                        val keyedRecords = records.map { nextCardToken++ to it }
                        visibleRecords = keyedRecords.toMap()
                        cards = keyedRecords.map { (token, record) ->
                            cardState(token, record.first, record.second)
                        }
                        emptyState = size == 0
                        next =
                            if (page.size > RECOVERY_PAGE_SIZE) {
                                page[RECOVERY_PAGE_SIZE - 1].cursor
                            } else {
                                null
                            }
                        navigation()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed || ticket != version) return@withContext
                    busy = false
                    setFeedback(R.string.recovery_failed)
                    navigation()
                }
            }
        }
    }

    private fun readCalendarLabels(page: List<SqliteLedger.RecoveryItem>): Map<Long, String> {
        val labels = HashMap<Long, String>()
        val sink = AndroidCalendarSink(activity.applicationContext)
        for (item in page) {
            val intent = item.intent!!
            if (!labels.containsKey(intent.calendar)) {
                var label = activity.getString(R.string.recovery_calendar_hidden)
                if (
                    activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED
                ) {
                    label =
                        try {
                            sink.calendarLabel(intent.calendar)
                                ?: activity.getString(R.string.auto_destination_unavailable)
                        } catch (ignored: RuntimeException) {
                            activity.getString(R.string.recovery_calendar_unverified)
                        }
                }
                labels[intent.calendar] = label
            }
        }
        return labels
    }

    private fun navigation() {
        hasCalendarReadAccess =
            activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        navigationState =
            RecoveryNavigationUiState(
                busy = busy,
                previousEnabled = back.isNotEmpty(),
                moreEnabled = next != null,
            )
    }

    private fun setFeedback(message: Int) {
        feedbackText = activity.getString(message)
    }

    private fun setFeedback(text: String) {
        feedbackText = text
    }

    private fun cardState(
        token: Long,
        intent: DurableDelivery.Intent,
        calendar: String?,
    ): RecoveryCardUiState {
        var source = activity.getString(R.string.recovery_source_unknown)
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
                activity.resources.getQuantityString(
                    R.plurals.recovery_source,
                    facts.seconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    activity.getString(type),
                    DateFormat.getDateTimeInstance().format(Date(facts.start)),
                    facts.seconds,
                )
        }
        return RecoveryCardUiState(
            token = token,
            detail =
                activity.getString(
                    R.string.recovery_item,
                    source,
                    calendar,
                    activity.getString(stateLabel(intent.state)),
                ),
            checkEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            suppressEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            reapproveVisible = ReadyReapproval.eligible(intent),
            openEventVisible = intent.eventId >= 0,
        )
    }

    private fun confirmSuppression(intent: DurableDelivery.Intent) {
        if (busy) return
        confirmation =
            Confirmation(
                title = activity.getString(R.string.recovery_suppress),
                message = activity.getString(R.string.recovery_suppress_confirm),
                actionLabel = activity.getString(R.string.recovery_suppress),
            ) {
                action(intent, true)
            }
    }

    private fun openEvent(intent: DurableDelivery.Intent) {
        if (intent.eventId < 0) return
        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, intent.eventId),
                ),
            )
        } catch (ignored: RuntimeException) {
            setFeedback(R.string.recovery_open_failed)
        }
    }

    private fun expireOldPayloads() {
        if (busy) return
        retentionFeedback = ""
        busy = true
        navigation()
        scope.launch(worker) {
            try {
                SqliteLedger(activity.applicationContext).use { ledger ->
                    val count = ledger.expirePayloads(50)
                    withContext(Dispatchers.Main.immediate) {
                        if (activity.isDestroyed) return@withContext
                        busy = false
                        retentionFeedback =
                            activity.resources.getQuantityString(
                                R.plurals.retention_result,
                                count,
                                count,
                            )
                        load(current)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    if (activity.isDestroyed) return@withContext
                    busy = false
                    retentionFeedback = activity.getString(R.string.recovery_failed)
                    navigation()
                }
            }
        }
    }

    private fun confirmReapproval(
        intent: DurableDelivery.Intent,
        calendar: String?,
        requestAccess: (Array<String>) -> Unit,
    ) {
        if (busy || !ReadyReapproval.eligible(intent)) return
        if (
            activity.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED ||
                activity.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            requestAccess(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
            setFeedback(R.string.reapproval_tap_again)
            return
        }
        val payload = intent.payload!!
        val preview =
            activity.getString(
                R.string.reapproval_preview,
                calendar,
                payload.title,
                payload.description,
                DateFormat.getDateTimeInstance().format(Date(payload.start)),
                DateFormat.getDateTimeInstance().format(Date(payload.end)),
            )
        confirmation =
            Confirmation(
                title = activity.getString(R.string.reapproval_open),
                message = preview,
                actionLabel = activity.getString(R.string.reapproval_confirm),
            ) {
                reapprove(intent)
            }
    }

    private fun reapprove(expected: DurableDelivery.Intent) {
        if (busy) return
        busy = true
        val approvedSession = approvalSession
        navigation()
        scope.launch(worker) {
            val outcome =
                try {
                    SqliteLedger(activity.applicationContext).use { ledger ->
                        ReadyReapproval(
                                ledger.outbox(),
                                AndroidCalendarSink(activity.applicationContext),
                            )
                            .approve(expected) { resumed && approvalSession === approvedSession }
                    }
                } catch (ignored: RuntimeException) {
                    DurableDelivery.Result.FAILED
                }
            withContext(Dispatchers.Main.immediate) {
                if (activity.isDestroyed) return@withContext
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

    fun resume() {
        resumed = true
        navigation()
        if (!opened) {
            opened = true
            load(null)
        }
    }

    fun pause() {
        confirmation = null
        resumed = false
        approvalSession = Any() // Returning cannot revive an earlier queued approval.
    }

    private fun action(intent: DurableDelivery.Intent, suppress: Boolean) {
        if (busy) return
        busy = true
        navigation()
        scope.launch(worker) {
            val outcome =
                try {
                    SqliteLedger(activity.applicationContext).use { ledger ->
                        val sink = AndroidCalendarSink(activity.applicationContext)
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
                        if (suppress) {
                            recovery.suppress(intent.key, intent.calendar)
                        } else {
                            recovery.reconcile(intent.key, intent.calendar)
                        }
                    }
                } catch (ignored: RuntimeException) {
                    ReceiptRecovery.Result.FAILED
                }
            withContext(Dispatchers.Main.immediate) {
                if (activity.isDestroyed) return@withContext
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
}
