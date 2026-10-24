package com.markobl.calllogsync.slice

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLogExportTest {
    private val exportId = UUID.fromString("00000000-0000-4000-8000-000000000001")

    @Test
    fun emitsTheSwiftPreviewGoldenWithoutContactOrAccountData() {
        val actual = CallLogExport.encode(sampleCalls(), exportId)
        val expected =
            checkNotNull(javaClass.getResourceAsStream("/call-log-v1.json")).bufferedReader().use {
                it.readText().trim()
            }

        assertEquals(expected, actual)
        assertFalse(actual.contains("Alice Example"))
        assertFalse(actual.contains("+15551234567"))
        assertFalse(actual.contains("private-account"))
        assertFalse(actual.contains("source-install-1"))
        assertFalse(actual.contains("receipts"))
    }

    @Test
    fun exportsStableOpaqueIdsAndNewExportIdsAcrossRuns() {
        val calls = sampleCalls()
        val first = CallLogExport.encode(calls, exportId)
        val secondExport = UUID.fromString("00000000-0000-4000-8000-000000000003")
        val second = CallLogExport.encode(calls, secondExport)

        val recordBodies = Regex("\\{\\\"id\\\":.*?\\}")
        assertEquals(
            recordBodies.findAll(first).map { it.value }.toList(),
            recordBodies.findAll(second).map { it.value }.toList(),
        )
        assertTrue(first.contains(exportId.toString()))
        assertTrue(second.contains(secondExport.toString()))
    }

    @Test
    fun rejectsUnknownDirectionsAndMalformedSourceIds() {
        val unknown = CallRecord("source-install-1", "19", 1, 0, 99, null, null, 1, null)
        val malformedId = CallRecord("source-install-1", "019", 1, 0, 1, null, null, 1, null)

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            CallLogExport.encode(listOf(unknown), exportId)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            CallLogExport.encode(listOf(malformedId), exportId)
        }
    }

    @Test
    fun rejectsTimeOverflowAndOversizedCollections() {
        val overflow =
            CallRecord("source-install-1", "19", Long.MAX_VALUE, 1, 1, null, null, 1, null)
        org.junit.Assert.assertThrows(ArithmeticException::class.java) {
            CallLogExport.encode(listOf(overflow), exportId)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            CallLogExport.encode(List(CallLogExport.MAX_CALLS + 1) { sampleCalls()[0] }, exportId)
        }
    }

    private fun sampleCalls(): List<CallRecord> =
        listOf(
            CallRecord(
                "source-install-1",
                "17",
                1_700_000_000_123L,
                45,
                1,
                "Alice Example",
                "+15551234567",
                1,
                "private-account",
            ),
            CallRecord(
                "source-install-1",
                "18",
                1_699_999_999_000L,
                0,
                3,
                "Alice Example",
                "+15551234567",
                1,
                "private-account",
            ),
        )
}
