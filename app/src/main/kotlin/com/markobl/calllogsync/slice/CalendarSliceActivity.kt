package com.markobl.calllogsync.slice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.markobl.calllogsync.R
import java.util.TimeZone
import java.util.concurrent.Executors

/** Explicit manual action; any enabled automatic policy is controlled independently. */
class CalendarSliceActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var picker: Spinner? = null
    private var panelState by
        mutableStateOf(ManualLogUiState(logLabel = "", status = "", actionEnabled = false))
    private var choices: List<AndroidCalendarSink.Choice> = ArrayList()
    private var selected = -1L
    private var busy = false
    private var refreshing = false
    private var refreshGeneration = 0

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro = TextView(this)
        intro.setText(R.string.slice_intro)
        layout.addView(intro)
        val permission = Button(this)
        permission.setText(R.string.slice_calendar_access)
        layout.addView(permission)
        permission.setOnClickListener {
            if (calendarAccess()) refresh()
            else
                requestPermissions(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    1,
                )
        }
        picker = Spinner(this)
        picker!!.id = R.id.slice_calendar_picker
        picker!!.contentDescription = getString(R.string.slice_choose)
        layout.addView(picker)
        picker!!.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selected =
                        if (position > 0 && position <= choices.size) choices[position - 1].id
                        else -1
                    getSharedPreferences("slice", MODE_PRIVATE)
                        .edit()
                        .putLong("calendar", selected)
                        .apply()
                    updateButton()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selected = -1
                    updateButton()
                }
            }
        val manualPanel =
            ComposeView(this).apply {
                id = R.id.slice_manual_panel
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { ManualLogPanel(state = panelState, onLogLatest = ::logLatest) }
            }
        layout.addView(manualPanel)
        panelState = panelState.copy(logLabel = getString(R.string.slice_log_latest))
        val automatic = Button(this)
        automatic.id = R.id.open_automatic
        automatic.setText(R.string.auto_open)
        automatic.setOnClickListener {
            startActivity(Intent(this, AutomaticSettingsActivity::class.java))
        }
        layout.addView(automatic)
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
    }

    public override fun onResume() {
        super.onResume()
        executor.execute {
            try {
                AutomaticScheduling.reconcile(applicationContext)
            } catch (unavailable: RuntimeException) {
                /* Settings exposes unavailable policy. */
            }
            try {
                SqliteLedger(applicationContext).use { it.expirePayloads(50) }
            } catch (unavailable: RuntimeException) {
                /* Recovery exposes storage errors. */
            }
        }
        if (calendarAccess()) refresh()
        else {
            selected = -1
            setStatus(R.string.slice_permission_needed)
            updateButton()
        }
    }

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun updateButton() {
        panelState =
            panelState.copy(
                actionEnabled = !busy && !refreshing && selected >= 0 && calendarAccess()
            )
        picker?.isEnabled = !busy && !refreshing && calendarAccess()
    }

    private fun setStatus(message: Int) {
        panelState = panelState.copy(status = getString(message))
    }

    private fun logLatest() {
        if (selected < 0 || busy) return
        if (!calendarAccess()) {
            setStatus(R.string.slice_permission_needed)
            updateButton()
            return
        }
        if (
            checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), 2)
            return
        }
        val destination = selected
        busy = true
        updateButton()
        setStatus(R.string.slice_working)
        executor.execute {
            val outcome =
                try {
                    SqliteLedger(applicationContext).use { ledger ->
                        ManualDelivery(
                                AndroidCallLogSource(applicationContext),
                                DurableDelivery(
                                    ledger.outbox(),
                                    AndroidCalendarSink(applicationContext),
                                ),
                                TimeZone.getDefault().id,
                            )
                            .run(destination)
                    }
                } catch (failed: RuntimeException) {
                    SliceEngine.Result.FAILED
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                setStatus(message(outcome))
                updateButton()
            }
        }
    }

    private fun refresh() {
        val saved = getSharedPreferences("slice", MODE_PRIVATE).getLong("calendar", -1)
        val generation = ++refreshGeneration
        refreshing = true
        updateButton()
        executor.execute {
            try {
                val loaded = AndroidCalendarSink(applicationContext).choices()
                runOnUiThread {
                    if (isDestroyed || generation != refreshGeneration) return@runOnUiThread
                    refreshing = false
                    choices = loaded
                    val labels = ArrayList<String?>()
                    labels.add(getString(R.string.slice_choose))
                    var position = 0
                    for (i in loaded.indices) {
                        labels.add(loaded[i].name)
                        if (loaded[i].id == saved) position = i + 1
                    }
                    selected = if (position == 0) -1 else loaded[position - 1].id
                    picker!!.adapter =
                        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
                    picker!!.setSelection(position)
                    setStatus(
                        if (loaded.isEmpty()) R.string.slice_no_calendars else R.string.slice_ready
                    )
                    updateButton()
                }
            } catch (unavailable: RuntimeException) {
                runOnUiThread {
                    if (!isDestroyed && generation == refreshGeneration) {
                        refreshing = false
                        selected = -1
                        setStatus(R.string.slice_calendar_unavailable)
                        updateButton()
                    }
                }
            }
        }
    }

    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        if (code == 1 && calendarAccess()) refresh()
        else if (code == 2) setStatus(R.string.slice_tap_again)
        else setStatus(R.string.slice_permission_needed)
    }

    private fun message(result: SliceEngine.Result): Int =
        when (result) {
            SliceEngine.Result.SAVED -> R.string.slice_saved
            SliceEngine.Result.ALREADY_LOGGED -> R.string.slice_already
            SliceEngine.Result.RECOVERED -> R.string.slice_recovered
            SliceEngine.Result.NO_CALL -> R.string.slice_no_call
            SliceEngine.Result.PERMISSION_REQUIRED -> R.string.slice_permission_needed
            SliceEngine.Result.DESTINATION_UNAVAILABLE -> R.string.slice_calendar_unavailable
            SliceEngine.Result.REVIEW_REQUIRED -> R.string.slice_review
            SliceEngine.Result.INVALID_CALL -> R.string.slice_invalid
            else -> R.string.slice_failed
        }

    public override fun onDestroy() {
        ++refreshGeneration
        executor.shutdown()
        super.onDestroy()
    }
}
