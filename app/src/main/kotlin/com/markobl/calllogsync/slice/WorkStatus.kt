package com.markobl.calllogsync.slice

/** Nonpersonal status; failures count worker attempts, not failed call rows. */
class WorkStatus(
    @JvmField val generation: Long,
    @JvmField val at: Long,
    token: String?,
    code: Code?,
    @JvmField val delivered: Int,
    @JvmField val review: Int,
    @JvmField val failures: Long,
) {
    enum class Code {
        COMPLETE,
        PAUSED,
        BACKLOG,
        PERMISSION_REQUIRED,
        DESTINATION_UNAVAILABLE,
        SOURCE_CHANGED,
        REVIEW_REQUIRED,
        INVALID_CALL,
        FAILED,
        NOTHING_PENDING,
        UNKNOWN;

        companion object {
            @JvmStatic
            fun parse(value: String?): Code =
                try {
                    valueOf(value!!)
                } catch (invalid: IllegalArgumentException) {
                    UNKNOWN
                } catch (invalid: NullPointerException) {
                    UNKNOWN
                }
        }
    }

    @JvmField val token: String
    @JvmField val code: Code

    init {
        require(
            generation >= 1 &&
                at >= 0 &&
                !token.isNullOrEmpty() &&
                code != null &&
                delivered >= 0 &&
                review >= 0 &&
                failures >= 0
        ) {
            "Invalid work status"
        }
        this.token = token
        this.code = code
    }

    companion object {
        @JvmStatic
        fun next(
            prior: WorkStatus?,
            generation: Long,
            at: Long,
            token: String?,
            code: Code?,
            delivered: Int,
            review: Int,
        ): WorkStatus {
            if (prior != null && prior.generation > generation) return prior
            var failures =
                if (prior != null && prior.generation == generation) prior.failures else 0L
            if (
                code == Code.FAILED &&
                    (prior == null || prior.generation != generation || prior.token != token) &&
                    failures < Long.MAX_VALUE
            )
                failures++
            return WorkStatus(generation, at, token, code, delivered, review, failures)
        }
    }
}
