package com.markobl.calllogsync.slice

import java.util.Collections

/** Frozen snapshot/anchors; progress advances only after handling a row. */
class PagedCalls private constructor() {
    interface Source {
        fun namespace(): String?

        fun upper(): CallRecord?

        fun at(id: Long): CallRecord?

        fun page(afterExclusive: Long, upperInclusive: Long, limit: Int): List<CallRecord?>?
    }

    class SourceChanged :
        IllegalStateException("Source changed or paging contract unavailable; review required")

    class Checkpoint(
        namespace: String?,
        @JvmField val upperId: Long,
        @JvmField val upperBinding: String?,
        @JvmField val lastId: Long,
        @JvmField val lastBinding: String?,
    ) {
        @JvmField val namespace: String

        init {
            require(
                !namespace.isNullOrEmpty() &&
                    upperId >= -1 &&
                    lastId >= -1 &&
                    lastId <= upperId &&
                    (upperId == -1L) == (upperBinding == null) &&
                    (lastId == -1L) == (lastBinding == null)
            ) {
                "Invalid source checkpoint"
            }
            this.namespace = namespace
        }

        fun advance(call: CallRecord?): Checkpoint {
            val id = id(call)
            if (namespace != call!!.namespace || id <= lastId || id > upperId) throw SourceChanged()
            return Checkpoint(namespace, upperId, upperBinding, id, binding(call))
        }
    }

    companion object {
        @JvmStatic
        fun capture(source: Source?): Checkpoint {
            val namespace = source!!.namespace()
            val upper = source.upper() ?: return Checkpoint(namespace, -1, null, -1, null)
            if (namespace != upper.namespace) throw SourceChanged()
            return Checkpoint(namespace, id(upper), binding(upper), -1, null)
        }

        @JvmStatic
        fun extend(source: Source?, completed: Checkpoint?): Checkpoint {
            val prior = completed!!
            require(prior.lastId == prior.upperId) { "Scan is incomplete" }
            validate(source, prior)
            val captured = capture(source)
            validate(source, prior)
            if (prior.namespace != captured.namespace || captured.upperId < prior.upperId)
                throw SourceChanged()
            if (captured.upperId == prior.upperId && captured.upperBinding != prior.upperBinding)
                throw SourceChanged()
            return Checkpoint(
                prior.namespace,
                captured.upperId,
                captured.upperBinding,
                prior.lastId,
                prior.lastBinding,
            )
        }

        @JvmStatic
        fun equal(left: Checkpoint?, right: Checkpoint?): Boolean =
            left != null &&
                right != null &&
                left.namespace == right.namespace &&
                left.upperId == right.upperId &&
                left.lastId == right.lastId &&
                left.upperBinding == right.upperBinding &&
                left.lastBinding == right.lastBinding

        @JvmStatic
        fun read(source: Source?, checkpoint: Checkpoint?, limit: Int): List<CallRecord> {
            require(limit in 1..500) { "Invalid page limit" }
            validate(source, checkpoint)
            val saved = checkpoint!!
            if (saved.lastId == saved.upperId) return Collections.emptyList()
            val rows = source!!.page(saved.lastId, saved.upperId, limit)
            if (rows.isNullOrEmpty() || rows.size > limit) throw SourceChanged()
            var previous = saved.lastId
            for (call in rows) {
                val id = id(call)
                if (saved.namespace != call!!.namespace || id <= previous || id > saved.upperId)
                    throw SourceChanged()
                previous = id
            }
            validate(source, saved)
            // Every row passed id(); retain Java copy-after-revalidation ordering.
            @Suppress("UNCHECKED_CAST") val copy = ArrayList(rows) as ArrayList<CallRecord>
            return Collections.unmodifiableList(copy)
        }

        @JvmStatic
        fun validate(source: Source?, checkpoint: Checkpoint?) {
            val saved = checkpoint!!
            if (saved.namespace != source!!.namespace()) throw SourceChanged()
            if (
                saved.upperId >= 0 &&
                    !same(
                        source.at(saved.upperId),
                        saved.upperId,
                        saved.upperBinding,
                        saved.namespace,
                    )
            )
                throw SourceChanged()
            if (
                saved.lastId >= 0 &&
                    !same(source.at(saved.lastId), saved.lastId, saved.lastBinding, saved.namespace)
            )
                throw SourceChanged()
        }

        private fun same(
            call: CallRecord?,
            expectedId: Long,
            expectedBinding: String?,
            namespace: String,
        ): Boolean =
            call != null &&
                id(call) == expectedId &&
                namespace == call.namespace &&
                expectedBinding!!.equals(binding(call))

        @JvmStatic
        fun binding(call: CallRecord?): String =
            CallIdentity.signature(call) +
                "|presentation-v1|" +
                call!!.presentationSupplied +
                "|" +
                call.numberPresentation

        @JvmStatic
        fun id(call: CallRecord?): Long {
            if (call == null) throw SourceChanged()
            try {
                val id = java.lang.Long.parseLong(call.id)
                if (id < 0 || id.toString() != call.id) throw SourceChanged()
                return id
            } catch (invalid: NumberFormatException) {
                throw SourceChanged()
            }
        }
    }
}
