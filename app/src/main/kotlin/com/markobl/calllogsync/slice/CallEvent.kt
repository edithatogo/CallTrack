package com.markobl.calllogsync.slice

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.Objects

/** Immutable public event fields. No network, attendees, reminders or sync-only fields. */
class CallEvent(
    @JvmField val title: String?,
    @JvmField val description: String?,
    @JvmField val start: Long,
    @JvmField val end: Long,
    @JvmField val timezone: String?,
    @JvmField val marker: String?,
) {
    fun fingerprint(): String =
        hash(
            field(title) +
                field(description) +
                start +
                ":" +
                end +
                ":" +
                field(timezone) +
                field(marker)
        )

    companion object {
        @JvmStatic
        fun from(call: CallRecord?, timezone: String?): CallEvent {
            val source = call!!
            val person =
                when {
                    !source.identityAllowed && source.presentationSupplied ->
                        when (source.numberPresentation) {
                            2 -> "Private number"
                            4 -> "Payphone"
                            else -> "Unknown number"
                        }
                    !source.identityAllowed -> "Private number"
                    present(source.name) -> source.name
                    present(source.number) -> source.number
                    else -> "Unknown number"
                }
            var description =
                "Number: " +
                    (if (source.identityAllowed && present(source.number)) source.number
                    else "Unavailable") +
                    "\nActual duration: ${source.seconds} seconds"
            if (source.seconds == 0L) {
                description += "\nOne-minute visual marker; actual call duration is zero."
            }
            val end =
                Math.addExact(
                    source.start,
                    if (source.seconds == 0L) 60000L else Math.multiplyExact(source.seconds, 1000L),
                )
            return CallEvent(
                "${source.direction}: $person",
                description,
                source.start,
                end,
                Objects.requireNonNull(timezone),
                "calltrack-dev://source/" + hash(source.key()),
            )
        }

        // Java String.trim() semantics are part of the stored fingerprint contract.
        private fun present(value: String?): Boolean =
            value != null && value.trim { it <= ' ' }.isNotEmpty()

        private fun field(value: String?): String = value?.let { "${it.length}:$it" } ?: "-1:"

        private fun hash(text: String): String {
            try {
                val bytes =
                    MessageDigest.getInstance("SHA-256")
                        .digest(text.toByteArray(StandardCharsets.UTF_8))
                return buildString {
                    for (value in bytes) append(
                        String.format(Locale.ROOT, "%02x", value.toInt() and 255)
                    )
                }
            } catch (e: NoSuchAlgorithmException) {
                throw AssertionError(e)
            }
        }
    }
}
