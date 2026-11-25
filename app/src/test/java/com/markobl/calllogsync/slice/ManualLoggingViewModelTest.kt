// SPDX-License-Identifier: Apache-2.0
package com.markobl.calllogsync.slice

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.markobl.calllogsync.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualLoggingViewModelTest {
    @Test
    fun permissionRevocationDiscardsTheInFlightCalendarRead() = runTest {
        val calendars = CompletableDeferred<ManualCalendars>()
        val readStarted = CompletableDeferred<Unit>()
        val data = Fixture {
            readStarted.complete(Unit)
            calendars.await()
        }
        withModel(data) { model ->
            model.uiState.test {
                assertEquals(ManualLoggingUiState.PermissionRequired, awaitItem())
                model.refresh(calendarAccess = true)
                assertFalse(awaitItem().destinations.enabled)
                readStarted.await()
                model.refresh(calendarAccess = false)
                assertEquals(ManualLoggingUiState.PermissionRequired, awaitItem())
                calendars.complete(savedCalendar)
                runCurrent()
                expectNoEvents()
                assertFalse(model.canLog())
            }
        }
    }

    @Test
    fun grantingCallPermissionNeverLogsAndResumeDoesNotRestartAnExplicitLog() = runTest {
        val data = Fixture { savedCalendar }
        withModel(data) { model ->
            model.uiState.test {
                assertEquals(ManualLoggingUiState.PermissionRequired, awaitItem())
                model.refresh(calendarAccess = true)
                runCurrent()
                val ready = expectMostRecentItem()
                assertTrue(model.canLog())
                model.callPermissionResult(granted = true)
                assertEquals(R.string.slice_tap_again, awaitItem().status)
                assertTrue(data.writes.isEmpty())

                model.logLatest()
                assertEquals(R.string.slice_working, awaitItem().status)
                model.resume(calendarAccess = true)
                model.logLatest()
                runCurrent()
                assertEquals(listOf(CALENDAR), data.writes)
                assertFalse(model.canLog())

                data.outcome.complete(SliceEngine.Result.SAVED)
                val result = awaitItem()
                assertEquals(R.string.slice_saved, result.status)
                assertEquals(ready.destinations.selectedToken, result.destinations.selectedToken)
                assertTrue(model.canLog())
                assertEquals(listOf(CALENDAR), data.writes)
            }
        }
    }

    private suspend fun TestScope.withModel(
        data: ManualLoggingDataSource,
        verify: suspend (ManualLoggingViewModel) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val factory = viewModelFactory { initializer { ManualLoggingViewModel(data) } }
        try {
            verify(ViewModelProvider.create(store, factory)[ManualLoggingViewModel::class])
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private class Fixture(private val load: suspend () -> ManualCalendars) :
        ManualLoggingDataSource {
        val writes = mutableListOf<Long>()
        val outcome = CompletableDeferred<SliceEngine.Result>()

        override suspend fun calendars() = load()

        override suspend fun select(calendar: Long) = Unit

        override suspend fun logLatest(calendar: Long): SliceEngine.Result {
            writes.add(calendar)
            return outcome.await()
        }

        override suspend fun maintain() = Unit
    }

    private companion object {
        const val CALENDAR = 41L
        val savedCalendar = ManualCalendars(mapOf(CALENDAR to "Fixture calendar"), CALENDAR)
    }
}
