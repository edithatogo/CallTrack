package com.markobl.calllogsync.slice

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import com.markobl.calllogsync.R
import java.util.Collections
import java.util.TimeZone
import java.util.concurrent.Executors

/** Explicit activation controls; reopening can resume enabled policy but never enable it. */
class AutomaticSettingsActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var destination: Spinner? = null
    private var incoming: CheckBox? = null
    private var outgoing: CheckBox? = null
    private var missed: CheckBox? = null
    private var enable: Button? = null
    private var pause: Button? = null
    private var state: TextView? = null
    private var counts: TextView? = null
    private var last: TextView? = null
    private var feedback: TextView? = null
    private var choices: List<AndroidCalendarSink.Choice> = Collections.emptyList()
    private var selected = -1L
    private var busy = false
    private var active = false
    private var initialised = false
    private var reloadRequested = false
    private var reloadReconcile = false

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro = TextView(this)
        intro.setText(R.string.auto_intro)
        layout.addView(intro)
        val access = Button(this)
        access.id = R.id.auto_permissions
        access.setText(R.string.auto_access)
        layout.addView(access)
        access.setOnClickListener {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                ),
                7,
            )
        }
        destination = Spinner(this)
        destination!!.id = R.id.auto_calendar_picker
        destination!!.contentDescription = getString(R.string.slice_choose)
        layout.addView(destination)
        destination!!.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Collections.singletonList(getString(R.string.slice_choose)),
            )
        destination!!.onItemSelectedListener =
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
                    buttons()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selected = -1
                    buttons()
                }
            }
        incoming = check(layout, R.string.auto_incoming, R.id.auto_incoming)
        outgoing = check(layout, R.string.auto_outgoing, R.id.auto_outgoing)
        missed = check(layout, R.string.auto_missed, R.id.auto_missed)
        enable = Button(this)
        enable!!.id = R.id.auto_enable
        enable!!.setText(R.string.auto_enable)
        layout.addView(enable)
        enable!!.setOnClickListener { confirmEnable() }
        pause = Button(this)
        pause!!.id = R.id.auto_pause
        pause!!.setText(R.string.auto_pause)
        layout.addView(pause)
        pause!!.setOnClickListener { change { AutomaticScheduling.pause(applicationContext) } }
        state = TextView(this)
        state!!.id = R.id.auto_state
        state!!.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        layout.addView(state)
        counts = TextView(this)
        counts!!.id = R.id.auto_counts
        layout.addView(counts)
        feedback = TextView(this)
        feedback!!.id = R.id.auto_feedback
        layout.addView(feedback)
        last = TextView(this)
        last!!.id = R.id.auto_last
        layout.addView(last)
        val refresh = Button(this)
        refresh.setText(R.string.auto_refresh)
        layout.addView(refresh)
        refresh.setOnClickListener { load(false) }
        val imports = Button(this)
        imports.id = R.id.open_imports
        imports.setText(R.string.import_open)
        imports.setOnClickListener {
            startActivity(Intent(this, HistoricalImportActivity::class.java))
        }
        layout.addView(imports)
        val recovery = Button(this)
        recovery.id = R.id.open_recovery
        recovery.setText(R.string.recovery_open)
        recovery.setOnClickListener {
            startActivity(Intent(this, ReceiptRecoveryActivity::class.java))
        }
        layout.addView(recovery)
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
        buttons()
    }

    private fun check(layout: LinearLayout, text: Int, id: Int): CheckBox {
        val box = CheckBox(this)
        box.id = id
        box.setText(text)
        box.isChecked = true
        layout.addView(box)
        box.setOnCheckedChangeListener { _, _ -> buttons() }
        return box
    }

    public override fun onResume() {
        super.onResume()
        load(true)
    }

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun callAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        if (enable != null)
            enable!!.isEnabled =
                !busy &&
                    selected >= 0 &&
                    calendarAccess() &&
                    callAccess() &&
                    (incoming!!.isChecked || outgoing!!.isChecked || missed!!.isChecked)
        pause?.isEnabled = !busy
        destination?.isEnabled = !busy && calendarAccess()
        incoming?.isEnabled = !busy
        outgoing?.isEnabled = !busy
        missed?.isEnabled = !busy
    }

    private fun confirmEnable() {
        if (selected < 0 || busy) return
        val calendar = selected
        val inCalls = incoming!!.isChecked
        val outCalls = outgoing!!.isChecked
        val missedCalls = missed!!.isChecked
        AlertDialog.Builder(this)
            .setTitle(R.string.auto_enable)
            .setMessage(R.string.auto_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.auto_enable) { _, _ ->
                change {
                    kotlin.check(AndroidCalendarSink(applicationContext).writable(calendar)) {
                        "Destination unavailable"
                    }
                    AutomaticScheduling.activate(
                        applicationContext,
                        calendar,
                        TimeZone.getDefault().id,
                        inCalls,
                        outCalls,
                        missedCalls,
                    )
                }
            }
            .show()
    }

    private fun change(action: Runnable?) {
        if (busy) return
        busy = true
        buttons()
        executor.execute {
            val done =
                try {
                    action!!.run()
                    true
                } catch (failed: RuntimeException) {
                    false
                }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy = false
                feedback!!.setText(
                    if (done) R.string.auto_action_saved else R.string.auto_action_failed
                )
                buttons()
                load(false)
            }
        }
    }

    private fun load(reconcile: Boolean) {
        if (busy) {
            reloadRequested = true
            reloadReconcile = reloadReconcile || reconcile
            return
        }
        busy = true
        buttons()
        executor.execute {
            try {
                val settings =
                    ActivationControl(AndroidActivationStore(applicationContext)).current()
                if (reconcile && settings != null && settings.enabled)
                    AutomaticScheduling.reconcile(applicationContext)
                val loaded =
                    if (calendarAccess()) AndroidCalendarSink(applicationContext).choices()
                    else Collections.emptyList()
                val summary =
                    SqliteLedger(applicationContext).use { ledger ->
                        ledger.expirePayloads(50)
                        ledger.counts()
                    }
                val worker =
                    if (settings == null) null
                    else AndroidWorkStatusStore(applicationContext).read(settings.generation)
                val code = worker?.code?.name
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    active = settings != null && settings.enabled
                    var activeName: String? = getString(R.string.auto_destination_unavailable)
                    if (settings != null)
                        for (choice in loaded) if (choice.id == settings.calendar)
                            activeName = choice.name
                    state!!.text =
                        if (active) getString(R.string.auto_enabled, activeName)
                        else getString(R.string.auto_disabled)
                    counts!!.text =
                        getString(
                            R.string.auto_count_text,
                            summary.pending,
                            summary.verified,
                            summary.review,
                            summary.suppressed,
                        )
                    last!!.text =
                        getString(
                            R.string.auto_status_summary,
                            getString(workerMessage(code)),
                            getString(R.string.auto_failure_count, worker?.failures ?: 0L),
                        )
                    choices = loaded
                    val labels = ArrayList<String?>()
                    labels.add(getString(R.string.slice_choose))
                    var position = 0
                    val preferred =
                        if (initialised) selected
                        else
                            settings?.calendar
                                ?: getSharedPreferences("slice", MODE_PRIVATE)
                                    .getLong("calendar", -1)
                    for (i in loaded.indices) {
                        labels.add(loaded[i].name)
                        if (loaded[i].id == preferred) position = i + 1
                    }
                    selected = if (position == 0) -1 else loaded[position - 1].id
                    destination!!.adapter =
                        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
                    destination!!.setSelection(position)
                    if (!initialised && settings != null) {
                        incoming!!.isChecked = settings.incoming
                        outgoing!!.isChecked = settings.outgoing
                        missed!!.isChecked = settings.missed
                    }
                    initialised = calendarAccess()
                    buttons()
                    reloadIfRequested()
                }
            } catch (unavailable: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    busy = false
                    state!!.setText(R.string.auto_action_failed)
                    buttons()
                    reloadIfRequested()
                }
            }
        }
    }

    private fun reloadIfRequested() {
        if (reloadRequested) {
            val reconcile = reloadReconcile
            reloadRequested = false
            reloadReconcile = false
            load(reconcile)
        }
    }

    private fun workerMessage(code: String?): Int =
        when (code) {
            null -> R.string.auto_not_run
            "COMPLETE" -> R.string.auto_complete
            "PAUSED" -> R.string.auto_paused
            "BACKLOG" -> R.string.auto_backlog
            "PERMISSION_REQUIRED" -> R.string.slice_permission_needed
            "DESTINATION_UNAVAILABLE" -> R.string.slice_calendar_unavailable
            "SOURCE_CHANGED",
            "REVIEW_REQUIRED" -> R.string.slice_review
            "INVALID_CALL" -> R.string.slice_invalid
            "FAILED" -> R.string.auto_run_failed
            else -> R.string.auto_unknown
        }

    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        initialised = false
        load(false)
    }

    public override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
