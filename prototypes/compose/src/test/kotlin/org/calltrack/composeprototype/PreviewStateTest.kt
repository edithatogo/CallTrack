package org.calltrack.composeprototype

import org.junit.Assert.*
import org.junit.Test

class PreviewStateTest {
    @Test
    fun cannotLogUntilPermissionAndAnExistingDestinationAreSelected() {
        assertFalse(PreviewState().canLog)
        assertFalse(PreviewState(calendarAccess = true).canLog)
        assertFalse(PreviewState(calendarAccess = true, selected = 99).canLog)
        assertTrue(PreviewState(calendarAccess = true, selected = 7).canLog)
    }

    @Test
    fun busyRefreshAndRevocationDisableActions() {
        val ready = PreviewState(calendarAccess = true, selected = 7)
        assertFalse(ready.copy(busy = true).canLog)
        assertFalse(ready.copy(refreshing = true).canLog)
        assertFalse(ready.copy(calendarAccess = false).canLog)
    }

    @Test
    fun destinationRefreshPreservesIdentityWithoutFallback() {
        val state = PreviewState(calendarAccess = true, selected = 9)
        assertEquals(9L, state.replaceCalendars(listOf(9, 7)).selected)
        assertEquals(-1L, state.replaceCalendars(listOf(7)).selected)
        assertFalse(state.replaceCalendars(emptyList()).canLog)
    }
}
