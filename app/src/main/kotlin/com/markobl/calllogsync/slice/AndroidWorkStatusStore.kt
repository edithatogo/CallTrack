package com.markobl.calllogsync.slice

import android.content.Context
import com.markobl.calllogsync.settings.ExecutionStatus

/** Generation-checked atomic status. Publishing a result grants no delivery authority. */
class AndroidWorkStatusStore internal constructor(private val storage: AndroidSettingsStore) {
    constructor(context: Context?) : this(AndroidSettingsStore(checkNotNull(context)))

    fun read(generation: Long): WorkStatus? =
        storage.read().let { state ->
            state.execution
                .takeIf { state.hasExecution() && it.generation == generation }
                ?.toStatus()
        }

    fun publish(
        generation: Long,
        at: Long,
        token: String?,
        code: WorkStatus.Code?,
        delivered: Int,
        review: Int,
    ): Boolean {
        storage.update { state ->
            if (!state.hasAutomatic() || state.automatic.generation != generation) {
                return@update state
            }
            val prior = state.execution.takeIf { state.hasExecution() }?.toStatus()
            val next = WorkStatus.next(prior, generation, at, token, code, delivered, review)
            state.toBuilder().setExecution(next.toProto()).build()
        }
        return true
    }

    private fun ExecutionStatus.toStatus() =
        WorkStatus(generation, at, token, WorkStatus.Code.parse(code), delivered, review, failures)
}

internal fun WorkStatus.toProto(): ExecutionStatus =
    ExecutionStatus.newBuilder()
        .setGeneration(generation)
        .setAt(at)
        .setToken(token)
        .setCode(code.name)
        .setDelivered(delivered)
        .setReview(review)
        .setFailures(failures)
        .build()
