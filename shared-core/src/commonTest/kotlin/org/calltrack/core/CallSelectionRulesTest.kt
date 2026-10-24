package org.calltrack.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallSelectionRulesTest {
    @Test
    fun allCallTypeMasksMatchNumericCallLogContract() {
        val unknownTypes = intArrayOf(0, 4, 5, 6, 7, 8, -1, Int.MAX_VALUE)
        for (mask in 0 until 8) {
            val selection =
                CallTypeSelection(
                    incoming = (mask and 1) != 0,
                    outgoing = (mask and 2) != 0,
                    missed = (mask and 4) != 0,
                )
            for (type in 1..3) {
                assertEquals((mask and (1 shl (type - 1))) != 0, selection.accepts(type))
            }
            for (type in unknownTypes) assertFalse(selection.accepts(type))
        }
    }

    @Test
    fun timeRangeIncludesStartAndExcludesEnd() {
        val range = CallTimeRange(1, Long.MAX_VALUE)

        assertFalse(range.contains(0))
        assertFalse(range.contains(Long.MIN_VALUE))
        assertTrue(range.contains(1))
        assertTrue(range.contains(Long.MAX_VALUE - 1))
        assertFalse(range.contains(Long.MAX_VALUE))
    }

    @Test
    fun invalidRangesFailWithoutArithmetic() {
        assertFailsWith<IllegalArgumentException> { CallTimeRange(-1, 1) }
        assertFailsWith<IllegalArgumentException> { CallTimeRange(1, 1) }
        assertFailsWith<IllegalArgumentException> { CallTimeRange(2, 1) }
        assertFailsWith<IllegalArgumentException> { CallTimeRange(0, Long.MIN_VALUE) }
    }
}
