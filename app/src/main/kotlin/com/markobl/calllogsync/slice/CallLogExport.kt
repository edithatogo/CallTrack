package com.markobl.calllogsync.slice

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Prototype-only, redacted JSON export for deliberate cross-platform preview. It does not activate
 * foreign receipts or authorize calendar delivery.
 */
object CallLogExport {
    const val SCHEMA_VERSION = 1
    const val MAX_CALLS = 10_000
    const val MAX_BYTES = 5 * 1024 * 1024

    private const val ID_DOMAIN = "org.calltrack.export.v1\u0000"
    private const val UUID_BYTES = 16
    private const val VERSION_BYTE = 6
    private const val VARIANT_BYTE = 8
    private const val VERSION_DATA_MASK = 0x0f
    private const val VARIANT_DATA_MASK = 0x3f
    private const val UUID_VERSION_8 = 0x80
    private const val RFC_VARIANT = 0x80

    /** The optional ID parameter exists for deterministic golden-contract tests. */
    @JvmStatic
    @JvmOverloads
    fun encode(calls: Collection<CallRecord?>?, exportId: UUID = UUID.randomUUID()): String {
        requireNotNull(calls) { "Calls unavailable" }
        require(calls.size <= MAX_CALLS) { "Too many calls" }

        val identities = HashSet<UUID>()
        val records =
            calls
                .map { call ->
                    val source = requireNotNull(call) { "Null call" }
                    val sourceId = source.id.toLongOrNull()
                    require(sourceId != null && sourceId >= 0 && sourceId.toString() == source.id) {
                        "Invalid source identity"
                    }
                    val direction =
                        when (source.direction) {
                            "Incoming" -> "incoming"
                            "Outgoing" -> "outgoing"
                            "Missed" -> "missed"
                            else -> throw IllegalArgumentException("Unsupported call type")
                        }
                    val durationMilliseconds = Math.multiplyExact(source.seconds, 1_000L)
                    Math.addExact(source.start, durationMilliseconds)
                    val id = portableId(source)
                    require(identities.add(id)) { "Duplicate source identity" }
                    ExportRecord(id, direction, source.start, source.seconds)
                }
                .sortedWith(
                    compareBy<ExportRecord> { it.startedAtMilliseconds }
                        .thenBy { it.id.toString() },
                )

        val callsJson =
            records.joinToString(",", prefix = "[", postfix = "]") { record ->
                "{\"id\":\"${record.id}\",\"direction\":\"${record.direction}\"," +
                    "\"started_at_ms\":${record.startedAtMilliseconds}," +
                    "\"duration_seconds\":${record.durationSeconds}}"
            }
        val payload =
            "{\"schema_version\":$SCHEMA_VERSION," +
                "\"export_id\":\"$exportId\"," +
                "\"record_count\":${records.size}," +
                "\"calls\":$callsJson}"
        val checksum = sha256(payload.toByteArray(StandardCharsets.UTF_8))
        val document = payload.dropLast(1) + ",\"content_sha256\":\"$checksum\"}"
        require(document.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "Export exceeds byte limit"
        }
        return document
    }

    private fun portableId(call: CallRecord): UUID {
        val input = (ID_DOMAIN + call.key()).toByteArray(StandardCharsets.UTF_8)
        val first128 = MessageDigest.getInstance("SHA-256").digest(input).copyOf(UUID_BYTES)
        // UUIDv8 with RFC variant bits; SHA-256 gives deterministic, opaque source IDs.
        first128[VERSION_BYTE] =
            ((first128[VERSION_BYTE].toInt() and VERSION_DATA_MASK) or UUID_VERSION_8).toByte()
        first128[VARIANT_BYTE] =
            ((first128[VARIANT_BYTE].toInt() and VARIANT_DATA_MASK) or RFC_VARIANT).toByte()
        val buffer = ByteBuffer.wrap(first128)
        return UUID(buffer.long, buffer.long)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

    private data class ExportRecord(
        val id: UUID,
        val direction: String,
        val startedAtMilliseconds: Long,
        val durationSeconds: Long,
    )
}
