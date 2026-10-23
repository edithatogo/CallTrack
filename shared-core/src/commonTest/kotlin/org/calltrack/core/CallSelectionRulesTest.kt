package org.calltrack.core

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallSelectionRulesTest {
    @Test
    fun callTypesAreIndependentAndUnknownTypesStayExcluded() {
        val selection = CallTypeSelection(incoming = true, outgoing = false, missed = true)

        assertTrue(selection.accepts(1))
        assertFalse(selection.accepts(2))
        assertTrue(selection.accepts(3))
        assertFalse(selection.accepts(0))
        assertFalse(selection.accepts(4))
        assertFalse(selection.accepts(Int.MAX_VALUE))
    }

    @Test
    fun timeRangeIncludesStartAndExcludesEnd() {
        val range = CallTimeRange(1, Long.MAX_VALUE)

        assertFalse(range.contains(0))
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
