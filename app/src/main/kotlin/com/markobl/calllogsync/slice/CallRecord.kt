package com.markobl.calllogsync.slice

/** A source row, not a contacts lookup. Raw presentation and null identity remain distinct. */
class CallRecord
private constructor(
    namespace: String?,
    id: String?,
    @JvmField val start: Long,
    @JvmField val seconds: Long,
    direction: String?,
    @JvmField val name: String?,
    @JvmField val number: String?,
    @JvmField val identityAllowed: Boolean,
    @JvmField val type: Int,
    @JvmField val phoneAccountId: String?,
    @JvmField val numberPresentation: Int?,
    @JvmField val presentationSupplied: Boolean,
) {
    @JvmField val namespace: String
    @JvmField val id: String
    @JvmField val direction: String

    init {
        require(!namespace.isNullOrEmpty() && !id.isNullOrEmpty() && start >= 0 && seconds >= 0) {
            "Invalid call identity/time"
        }
        this.namespace = namespace
        this.id = id
        this.direction = direction ?: "Unknown"
    }

    constructor(
        namespace: String?,
        id: String?,
        start: Long,
        seconds: Long,
        direction: String?,
        name: String?,
        number: String?,
        identityAllowed: Boolean,
    ) : this(
        namespace,
        id,
        start,
        seconds,
        direction,
        name,
        number,
        identityAllowed,
        typeOf(direction),
        null,
        null,
        false,
    )

    constructor(
        namespace: String?,
        id: String?,
        start: Long,
        seconds: Long,
        type: Int,
        name: String?,
        number: String?,
        identityAllowed: Boolean,
        phoneAccountId: String?,
    ) : this(
        namespace,
        id,
        start,
        seconds,
        directionOf(type),
        name,
        number,
        identityAllowed,
        type,
        phoneAccountId,
        null,
        false,
    )

    constructor(
        namespace: String?,
        id: String?,
        start: Long,
        seconds: Long,
        type: Int,
        name: String?,
        number: String?,
        presentation: Int?,
        phoneAccountId: String?,
    ) : this(
        namespace,
        id,
        start,
        seconds,
        directionOf(type),
        name,
        number,
        presentation == 1,
        type,
        phoneAccountId,
        presentation,
        true,
    )

    fun key(): String = "${namespace.length}:$namespace${id.length}:$id"

    companion object {
        private fun typeOf(direction: String?): Int =
            when (direction) {
                "Incoming" -> 1
                "Outgoing" -> 2
                "Missed" -> 3
                else -> 0
            }

        private fun directionOf(type: Int): String =
            when (type) {
                1 -> "Incoming"
                2 -> "Outgoing"
                3 -> "Missed"
                else -> "Unknown"
            }
    }
}
