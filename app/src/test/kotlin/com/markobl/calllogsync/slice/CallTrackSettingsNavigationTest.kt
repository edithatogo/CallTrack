// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.navigation3.runtime.NavKey
import com.markobl.calllogsync.slice.CallTrackRoute.Automatic
import com.markobl.calllogsync.slice.CallTrackRoute.Import
import com.markobl.calllogsync.slice.CallTrackRoute.Manual
import com.markobl.calllogsync.slice.CallTrackRoute.Recovery
import org.junit.Assert.assertEquals
import org.junit.Test

class CallTrackSettingsNavigationTest {
    @Test
    fun staleSettingsActionsCannotReplaceTheManualOrStandaloneRoot() {
        for (root in listOf(Manual, Import, Recovery)) {
            val stack = mutableListOf<NavKey>(root)
            stack.selectSettingsSection(Import)
            stack.selectSettingsSection(Recovery)
            stack.selectSettingsSection(Automatic)
            assertEquals(listOf(root), stack)
        }
    }

    @Test
    fun settingsActionsReplaceOnlyTheCurrentDetailAndReturnToSettings() {
        val stack = mutableListOf<NavKey>(Manual, Automatic)
        stack.selectSettingsSection(Import)
        assertEquals(listOf(Manual, Automatic, Import), stack)

        stack.selectSettingsSection(Recovery)
        stack.selectSettingsSection(Recovery)
        assertEquals(listOf(Manual, Automatic, Recovery), stack)

        stack.selectSettingsSection(Automatic)
        assertEquals(listOf(Manual, Automatic), stack)
    }
}
