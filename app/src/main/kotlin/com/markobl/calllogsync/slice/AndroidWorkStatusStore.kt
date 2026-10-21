package com.markobl.calllogsync.slice

import android.content.Context
import android.content.SharedPreferences
import java.util.Collections
import java.util.WeakHashMap

/** Generation-checked atomic status; publication never grants delivery authority. */
class AndroidWorkStatusStore(private val preferences: SharedPreferences?) {
    constructor(
        context: Context?
    ) : this(context!!.applicationContext.getSharedPreferences("slice", Context.MODE_PRIVATE))

    fun read(generation: Long): WorkStatus? =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            if (uncertain.contains(preferences))
                throw IllegalStateException("Execution status commit uncertain")
            val stored = stored()
            if (stored != null && stored.generation == generation) stored else null
        }

    fun publish(
        generation: Long,
        at: Long,
        token: String?,
        code: WorkStatus.Code?,
        delivered: Int,
        review: Int,
    ): Boolean =
        synchronized(DurableDelivery.DELIVERY_LOCK) {
            val active = AndroidActivationStore(preferences).get()
            if (active == null || active.generation != generation) return true
            val prior = stored()
            val next = WorkStatus.next(prior, generation, at, token, code, delivered, review)
            if (next === prior) return true
            uncertain.add(preferences)
            val committed =
                preferences!!
                    .edit()
                    .putLong("automaticStatusGeneration", next.generation)
                    .putLong("automaticStatusAt", next.at)
                    .putString("automaticStatusToken", next.token)
                    .putString("automaticStatusCode", next.code.name)
                    .putInt("automaticStatusDelivered", next.delivered)
                    .putInt("automaticStatusReview", next.review)
                    .putLong("automaticStatusFailures", next.failures)
                    .commit()
            if (committed) uncertain.remove(preferences)
            committed
        }

    private fun stored(): WorkStatus? {
        val generation = preferences!!.getLong("automaticStatusGeneration", -1)
        if (generation < 1) return null
        return WorkStatus(
            generation,
            preferences.getLong("automaticStatusAt", 0),
            preferences.getString("automaticStatusToken", "legacy"),
            WorkStatus.Code.parse(preferences.getString("automaticStatusCode", null)),
            preferences.getInt("automaticStatusDelivered", 0),
            preferences.getInt("automaticStatusReview", 0),
            preferences.getLong("automaticStatusFailures", 0),
        )
    }

    companion object {
        // Android mutates its cache before disk IO; all access uses DELIVERY_LOCK.
        private val uncertain =
            Collections.newSetFromMap(WeakHashMap<SharedPreferences?, Boolean>())
    }
}
