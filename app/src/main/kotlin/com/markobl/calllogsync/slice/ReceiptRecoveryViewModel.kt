// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markobl.calllogsync.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RECOVERY_PAGE_SIZE = 10

@Immutable
internal sealed interface ReceiptRecoveryUiState {
    val page: RecoveryPageUiState

    data class Showing(override val page: RecoveryPageUiState) : ReceiptRecoveryUiState

    data class Reviewing(
        override val page: RecoveryPageUiState,
        val review: RecoveryReview,
    ) : ReceiptRecoveryUiState
}

@Immutable
internal data class RecoveryPageUiState(
    val empty: Boolean = false,
    val feedback: String = "",
    val retentionFeedback: String = "",
    val hasCalendarReadAccess: Boolean = false,
    val cards: List<RecoveryCardUiState> = emptyList(),
    val navigation: RecoveryNavigationUiState = RecoveryNavigationUiState(),
)

@Immutable
internal class RecoveryReview(
    @param:StringRes @get:StringRes val title: Int,
    val message: String,
    @param:StringRes @get:StringRes val actionLabel: Int,
)

@HiltViewModel
internal class ReceiptRecoveryViewModel
@Inject
constructor(@param:ApplicationContext private val context: Context) : ViewModel() {
    private val worker = Dispatchers.IO.limitedParallelism(1)
    private var page = RecoveryPageUiState()
    private val state =
        MutableStateFlow<ReceiptRecoveryUiState>(ReceiptRecoveryUiState.Showing(page))
    val uiState: StateFlow<ReceiptRecoveryUiState> =
        state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state.value)

    private val back = ArrayList<SqliteLedger.RecoveryCursor?>()
    private var current: SqliteLedger.RecoveryCursor? = null
    private var next: SqliteLedger.RecoveryCursor? = null
    private var visibleRecords = emptyMap<Long, RecoveryRecord>()
    private var nextCardToken = 0L
    private var pendingReview: PendingRecoveryReview? = null
    private var busy = false
    private var opened = false

    @Volatile private var approvalSession: Any? = null

    @MainThread
    fun setResumed(resumed: Boolean) {
        if (!resumed) {
            approvalSession = null
            pendingReview?.let { resolveReview(it.review, confirmed = false) }
            return
        }
        approvalSession = Any()
        updateControls()
        if (!opened) {
            opened = true
            load(null)
        }
    }

    override fun onCleared() {
        approvalSession = null
    }

    @MainThread
    fun permissionsChanged() {
        if (!busy) load(current)
    }

    @MainThread
    fun refresh() {
        if (busy || approvalSession == null) return
        back.clear()
        load(null)
    }

    @MainThread
    fun navigateHistory(forward: Boolean) {
        if (busy || approvalSession == null) return
        if (forward) {
            val cursor = next ?: return
            back.add(current)
            load(cursor)
        } else if (back.isNotEmpty()) {
            load(back.removeAt(back.lastIndex))
        }
    }

    @MainThread
    fun check(token: Long) {
        val record = activeRecord(token) ?: return
        action(record.intent, suppress = false)
    }

    @MainThread
    fun requestSuppression(token: Long) {
        val record = activeRecord(token) ?: return
        val review =
            RecoveryReview(
                title = R.string.recovery_suppress,
                message = context.getString(R.string.recovery_suppress_confirm),
                actionLabel = R.string.recovery_suppress,
            )
        showReview(token, record, review) { action(record.intent, suppress = true) }
    }

    /** Returns true when the UI must request calendar access before any approval. */
    @MainThread
    fun requestReapproval(token: Long): Boolean {
        val record = activeRecord(token) ?: return false
        val intent = record.intent
        if (!ReadyReapproval.eligible(intent)) return false
        if (!hasCalendarReadAccess() || !hasCalendarWriteAccess()) {
            page = page.copy(feedback = context.getString(R.string.reapproval_tap_again))
            publish()
            return true
        }
        val payload = checkNotNull(intent.payload)
        val review =
            RecoveryReview(
                title = R.string.reapproval_open,
                message =
                    context.getString(
                        R.string.reapproval_preview,
                        record.calendar,
                        payload.title,
                        payload.description,
                        DateFormat.getDateTimeInstance().format(Date(payload.start)),
                        DateFormat.getDateTimeInstance().format(Date(payload.end)),
                    ),
                actionLabel = R.string.reapproval_confirm,
            )
        showReview(token, record, review) { session -> reapprove(intent, session) }
        return false
    }

    @MainThread
    fun resolveReview(review: RecoveryReview, confirmed: Boolean) {
        val pending = pendingReview ?: return
        if (pending.review !== review) return
        pendingReview = null
        publish()
        if (!confirmed || busy) return
        if (
            approvalSession === pending.session && visibleRecords[pending.token] === pending.record
        ) {
            pending.confirm()
        }
    }

    @MainThread
    fun eventId(token: Long): Long? {
        if (approvalSession == null) return null
        return visibleRecords[token]?.intent?.eventId?.takeIf { it >= 0 }
    }

    @MainThread
    fun eventUnavailable() {
        page = page.copy(feedback = context.getString(R.string.recovery_open_failed))
        publish()
    }

    @MainThread
    fun expireOldPayloads() {
        if (busy || approvalSession == null) return
        page = page.copy(retentionFeedback = "")
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            try {
                SqliteLedger(context).use { ledger ->
                    val count = ledger.expirePayloads(50)
                    withContext(Dispatchers.Main.immediate) {
                        busy = false
                        page =
                            page.copy(
                                retentionFeedback =
                                    context.resources.getQuantityString(
                                        R.plurals.retention_result,
                                        count,
                                        count,
                                    ),
                            )
                        load(current)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    busy = false
                    page =
                        page.copy(retentionFeedback = context.getString(R.string.recovery_failed))
                    updateControls()
                }
            }
        }
    }

    private fun activeRecord(token: Long): RecoveryRecord? =
        if (busy || approvalSession == null) null else visibleRecords[token]

    private fun showReview(
        token: Long,
        record: RecoveryRecord,
        review: RecoveryReview,
        confirm: (Any) -> Unit,
    ) {
        val session = approvalSession ?: return
        pendingReview = PendingRecoveryReview(review, token, record, session) { confirm(session) }
        publish()
    }

    private fun reapprove(expected: DurableDelivery.Intent, approvedSession: Any) {
        if (!hasCalendarReadAccess() || !hasCalendarWriteAccess()) {
            page = page.copy(feedback = context.getString(R.string.reapproval_tap_again))
            updateControls()
            return
        }
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            val outcome =
                try {
                    SqliteLedger(context).use { ledger ->
                        ReadyReapproval(ledger.outbox(), AndroidCalendarSink(context)).approve(
                            expected,
                        ) {
                            approvalSession === approvedSession
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    DurableDelivery.Result.FAILED
                }
            withContext(Dispatchers.Main.immediate) {
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
                page = page.copy(feedback = context.getString(label))
                load(current)
            }
        }
    }

    private fun action(intent: DurableDelivery.Intent, suppress: Boolean) {
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            val outcome =
                try {
                    SqliteLedger(context).use { ledger ->
                        val sink = AndroidCalendarSink(context)
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
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    ReceiptRecovery.Result.FAILED
                }
            withContext(Dispatchers.Main.immediate) {
                busy = false
                page = page.copy(feedback = context.getString(resultLabel(outcome)))
                load(current)
            }
        }
    }

    private fun load(cursor: SqliteLedger.RecoveryCursor?) {
        if (busy) return
        busy = true
        updateControls()
        viewModelScope.launch(worker) {
            try {
                SqliteLedger(context).use { ledger ->
                    ledger.expirePayloads(RECEIPT_RETENTION_BATCH_SIZE)
                    val items = ledger.recoveryPage(cursor, RECOVERY_PAGE_SIZE + 1)
                    val labels = readCalendarLabels(items)
                    withContext(Dispatchers.Main.immediate) {
                        busy = false
                        current = cursor
                        visibleRecords =
                            items.take(RECOVERY_PAGE_SIZE).associate { item ->
                                val intent = checkNotNull(item.intent)
                                nextCardToken++ to RecoveryRecord(intent, labels[intent.calendar])
                            }
                        page =
                            page.copy(
                                cards =
                                    visibleRecords.map { (token, record) ->
                                        cardState(token, record)
                                    },
                                empty = visibleRecords.isEmpty(),
                            )
                        next =
                            items.getOrNull(RECOVERY_PAGE_SIZE)?.let {
                                items[RECOVERY_PAGE_SIZE - 1].cursor
                            }
                        updateControls()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                withContext(Dispatchers.Main.immediate) {
                    busy = false
                    page = page.copy(feedback = context.getString(R.string.recovery_failed))
                    updateControls()
                }
            }
        }
    }

    private fun readCalendarLabels(items: List<SqliteLedger.RecoveryItem>): Map<Long, String> {
        val labels = HashMap<Long, String>()
        val sink = AndroidCalendarSink(context)
        for (item in items) {
            val calendar = checkNotNull(item.intent).calendar
            labels.getOrPut(calendar) {
                if (!hasCalendarReadAccess()) {
                    context.getString(R.string.recovery_calendar_hidden)
                } else {
                    try {
                        sink.calendarLabel(calendar)
                            ?: context.getString(R.string.auto_destination_unavailable)
                    } catch (_: RuntimeException) {
                        context.getString(R.string.recovery_calendar_unverified)
                    }
                }
            }
        }
        return labels
    }

    private fun cardState(token: Long, record: RecoveryRecord): RecoveryCardUiState {
        val intent = record.intent
        val facts = intent.facts
        val source =
            if (facts == null) {
                context.getString(R.string.recovery_source_unknown)
            } else {
                val type =
                    when (facts.type) {
                        1 -> R.string.auto_incoming
                        2 -> R.string.auto_outgoing
                        3 -> R.string.auto_missed
                        else -> R.string.recovery_call
                    }
                context.resources.getQuantityString(
                    R.plurals.recovery_source,
                    facts.seconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    context.getString(type),
                    DateFormat.getDateTimeInstance().format(Date(facts.start)),
                    facts.seconds,
                )
            }
        return RecoveryCardUiState(
            token = token,
            detail =
                context.getString(
                    R.string.recovery_item,
                    source,
                    record.calendar,
                    context.getString(stateLabel(intent.state)),
                ),
            checkEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            suppressEnabled = intent.state != DurableDelivery.State.SUPPRESSED,
            reapproveVisible = ReadyReapproval.eligible(intent),
            openEventVisible = intent.eventId >= 0,
        )
    }

    private fun hasCalendarReadAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasCalendarWriteAccess(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun updateControls() {
        page =
            page.copy(
                hasCalendarReadAccess = hasCalendarReadAccess(),
                navigation =
                    RecoveryNavigationUiState(
                        busy = busy,
                        previousEnabled = back.isNotEmpty(),
                        moreEnabled = next != null,
                    ),
            )
        publish()
    }

    private fun publish() {
        val review = pendingReview?.review
        state.value =
            if (review == null) {
                ReceiptRecoveryUiState.Showing(page)
            } else {
                ReceiptRecoveryUiState.Reviewing(page, review)
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

    private data class RecoveryRecord(val intent: DurableDelivery.Intent, val calendar: String?)

    private data class PendingRecoveryReview(
        val review: RecoveryReview,
        val token: Long,
        val record: RecoveryRecord,
        val session: Any,
        val confirm: () -> Unit,
    )
}
