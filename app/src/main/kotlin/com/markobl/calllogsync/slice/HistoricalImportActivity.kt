package com.markobl.calllogsync.slice

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import com.markobl.calllogsync.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.TimeZone
import java.util.concurrent.Executors

/** Foreground bounded runs; opening/resuming never confirms historical delivery. */
class HistoricalImportActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false
    private var busy = false
    private var currentId: String? = null
    private var zone: String? = null
    private var session: HistoricalImport.Session? = null
    private var from: EditText? = null
    private var to: EditText? = null
    private var destination: Spinner? = null
    private var incoming: CheckBox? = null
    private var outgoing: CheckBox? = null
    private var missed: CheckBox? = null
    private var create: Button? = null
    private var continueRun: Button? = null
    private var confirm: Button? = null
    private var cancel: Button? = null
    private var stop: Button? = null
    private var older: Button? = null
    private var newer: Button? = null
    private var summary: TextView? = null
    private var feedback: TextView? = null
    private var history: LinearLayout? = null
    private var choices: List<AndroidCalendarSink.Choice> = Collections.emptyList()
    private var selected = -1L
    private var historyCursor = -1L
    private var renderVersion = 0L
    private val historyBack = ArrayList<Long>()
    private var nextHistoryCursor = -1L

    public override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        zone =
            if (saved == null) ZoneId.systemDefault().id
            else saved.getString("zone", ZoneId.systemDefault().id)
        currentId = saved?.getString("session")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(pad, pad, pad, pad)
        val intro = TextView(this)
        intro.text = getString(R.string.import_intro, zone)
        layout.addView(intro)
        val access = button(layout, R.string.auto_access, 0)
        access.setOnClickListener {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                ),
                9,
            )
        }
        destination = Spinner(this)
        destination!!.id = R.id.import_calendar_picker
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
        from =
            date(
                layout,
                R.string.import_from,
                R.id.import_from,
                if (saved == null) LocalDate.now(ZoneId.of(zone)).minusDays(7).toString()
                else saved.getString("from"),
            )
        to =
            date(
                layout,
                R.string.import_to,
                R.id.import_to,
                if (saved == null) LocalDate.now(ZoneId.of(zone)).plusDays(1).toString()
                else saved.getString("to"),
            )
        incoming =
            check(layout, R.string.auto_incoming, saved == null || saved.getBoolean("incoming"))
        outgoing =
            check(layout, R.string.auto_outgoing, saved == null || saved.getBoolean("outgoing"))
        missed = check(layout, R.string.auto_missed, saved == null || saved.getBoolean("missed"))
        create = button(layout, R.string.import_create, R.id.import_create)
        create!!.setOnClickListener { startPreview() }
        summary = TextView(this)
        summary!!.id = R.id.import_summary
        summary!!.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        summary!!.setText(R.string.import_none)
        layout.addView(summary)
        continueRun = button(layout, R.string.import_continue, R.id.import_continue)
        continueRun!!.setOnClickListener {
            running = true
            step()
        }
        confirm = button(layout, R.string.import_confirm, R.id.import_confirm)
        confirm!!.setOnClickListener { confirmImport() }
        stop = button(layout, R.string.import_stop, R.id.import_stop)
        stop!!.setOnClickListener {
            running = false
            feedback!!.setText(R.string.import_stopping)
            buttons()
        }
        cancel = button(layout, R.string.import_cancel, R.id.import_cancel)
        cancel!!.setOnClickListener { cancelImport() }
        feedback = TextView(this)
        feedback!!.id = R.id.import_feedback
        layout.addView(feedback)
        val historyLabel = TextView(this)
        historyLabel.setText(R.string.import_history)
        layout.addView(historyLabel)
        history = LinearLayout(this)
        history!!.orientation = LinearLayout.VERTICAL
        history!!.id = R.id.import_history
        layout.addView(history)
        newer = button(layout, R.string.import_newer, 0)
        newer!!.setOnClickListener {
            if (historyBack.isNotEmpty()) loadHistory(historyBack.removeAt(historyBack.size - 1))
        }
        older = button(layout, R.string.import_older, 0)
        older!!.setOnClickListener {
            if (nextHistoryCursor > 0) {
                historyBack.add(historyCursor)
                loadHistory(nextHistoryCursor)
            }
        }
        val scroll = ScrollView(this)
        scroll.addView(layout)
        ActivityInsets.apply(scroll)
        setContentView(scroll)
        buttons()
        loadCalendars(if (saved == null) -1 else saved.getLong("calendar", -1))
        loadHistory(-1)
        if (currentId != null) loadSession(currentId)
    }

    private fun button(layout: LinearLayout, text: Int, id: Int): Button {
        val result = Button(this)
        result.setText(text)
        if (id != 0) result.id = id
        layout.addView(result)
        return result
    }

    private fun date(layout: LinearLayout, label: Int, id: Int, value: String?): EditText {
        val text = TextView(this)
        text.setText(label)
        text.labelFor = id
        layout.addView(text)
        val field = EditText(this)
        field.id = id
        field.setSingleLine(true)
        field.inputType =
            android.text.InputType.TYPE_CLASS_DATETIME or
                android.text.InputType.TYPE_DATETIME_VARIATION_DATE
        field.setText(value)
        layout.addView(field)
        return field
    }

    private fun check(layout: LinearLayout, label: Int, checked: Boolean): CheckBox {
        val box = CheckBox(this)
        box.setText(label)
        box.isChecked = checked
        layout.addView(box)
        box.setOnCheckedChangeListener { _, _ -> buttons() }
        return box
    }

    private fun access(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED && calendarAccess()

    private fun calendarAccess(): Boolean =
        checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun buttons() {
        if (create != null)
            create!!.isEnabled =
                !busy &&
                    selected >= 0 &&
                    access() &&
                    (incoming!!.isChecked || outgoing!!.isChecked || missed!!.isChecked)
        destination?.isEnabled = !busy && calendarAccess()
        val value = session
        continueRun?.isEnabled =
            !busy &&
                value != null &&
                (value.phase == HistoricalImport.Phase.BUILDING ||
                    value.phase == HistoricalImport.Phase.CONFIRMED)
        confirm?.isEnabled = !busy && value != null && value.phase == HistoricalImport.Phase.PREVIEW
        cancel?.isEnabled =
            value != null &&
                value.phase != HistoricalImport.Phase.COMPLETE &&
                value.phase != HistoricalImport.Phase.CANCELLED
        stop?.isEnabled = running
    }

    private fun loadCalendars(preferred: Long) {
        executor.execute {
            try {
                val loaded =
                    if (calendarAccess()) AndroidCalendarSink(applicationContext).choices()
                    else Collections.emptyList()
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    choices = loaded
                    val labels = ArrayList<String?>()
                    labels.add(getString(R.string.slice_choose))
                    var position = 0
                    for (i in loaded.indices) {
                        labels.add(loaded[i].name)
                        if (loaded[i].id == preferred) position = i + 1
                    }
                    destination!!.adapter =
                        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
                    destination!!.setSelection(position)
                    if (session != null) render()
                    buttons()
                }
            } catch (failed: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun startPreview() {
        if (busy || selected < 0) return
        val range =
            try {
                ImportRange.parse(from!!.text.toString(), to!!.text.toString(), zone)
            } catch (invalid: RuntimeException) {
                feedback!!.setText(R.string.import_invalid_range)
                return
            }
        val calendar = selected
        val inCalls = incoming!!.isChecked
        val outCalls = outgoing!!.isChecked
        val missedCalls = missed!!.isChecked
        val start = Runnable {
            running = true
            perform(
                { importer, _ ->
                    importer.start(
                        calendar,
                        range.timezone,
                        range.from,
                        range.to,
                        inCalls,
                        outCalls,
                        missedCalls,
                    )
                },
                true,
            )
        }
        val value = session
        if (
            value != null &&
                value.phase != HistoricalImport.Phase.COMPLETE &&
                value.phase != HistoricalImport.Phase.CANCELLED
        )
            AlertDialog.Builder(this)
                .setMessage(R.string.import_another)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_create) { _, _ -> start.run() }
                .show()
        else start.run()
    }

    private fun confirmImport() {
        val value = session
        if (busy || value == null || value.phase != HistoricalImport.Phase.PREVIEW) return
        val id = currentId
        AlertDialog.Builder(this)
            .setTitle(R.string.import_confirm)
            .setMessage(
                getString(
                    R.string.import_confirm_text,
                    value.count,
                    destinationName(value),
                    dates(value),
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.import_confirm) { _, _ ->
                running = true
                perform({ importer, _ -> importer.confirm(id) }, true)
            }
            .show()
    }

    private fun cancelImport() {
        if (session == null) return
        val id = currentId
        AlertDialog.Builder(this)
            .setMessage(R.string.import_cancel_text)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.import_cancel) { _, _ ->
                running = false
                perform({ importer, _ -> importer.cancel(id) }, false)
            }
            .show()
    }

    private fun step() {
        if (!running || busy || currentId == null) return
        val id = currentId
        perform(
            { importer, store ->
                val latest = store.get(id) ?: throw IllegalArgumentException()
                when (latest.phase) {
                    HistoricalImport.Phase.BUILDING -> importer.preview(id, 20)
                    HistoricalImport.Phase.CONFIRMED -> importer.deliver(id, 20) { running }
                    else -> latest
                }
            },
            true,
        )
    }

    private fun interface Action {
        fun run(importer: HistoricalImport, store: SqliteImportStore): HistoricalImport.Session
    }

    private fun perform(action: Action, follow: Boolean) {
        val ticket = ++renderVersion
        busy = true
        buttons()
        executor.execute {
            try {
                SqliteImportStore(applicationContext).use { store ->
                    SqliteLedger(applicationContext).use { ledger ->
                        ledger.expirePayloads(50)
                        val importer =
                            HistoricalImport(
                                store,
                                AndroidCallLogSource(applicationContext),
                                ledger.outbox(),
                                DurableDelivery(
                                    ledger.outbox(),
                                    AndroidCalendarSink(applicationContext),
                                ),
                            )
                        val result = action.run(importer, store)
                        runOnUiThread {
                            if (isDestroyed || ticket != renderVersion) return@runOnUiThread
                            busy = false
                            val previous = session
                            if (
                                currentId == null ||
                                    currentId != result.id ||
                                    previous == null ||
                                    result.revision >= previous.revision
                            ) {
                                currentId = result.id
                                session = result
                                render()
                            }
                            if (result.stopped != null) {
                                running = false
                                feedback!!.setText(R.string.import_blocked)
                            } else feedback!!.text = ""
                            buttons()
                            if (
                                follow &&
                                    running &&
                                    (result.phase == HistoricalImport.Phase.BUILDING ||
                                        result.phase == HistoricalImport.Phase.CONFIRMED)
                            )
                                summary!!.post { step() }
                            else {
                                running = false
                                buttons()
                                loadHistory(historyCursor)
                            }
                        }
                    }
                }
            } catch (failed: RuntimeException) {
                runOnUiThread {
                    if (isDestroyed || ticket != renderVersion) return@runOnUiThread
                    running = false
                    busy = false
                    feedback!!.setText(R.string.import_failed)
                    buttons()
                }
            }
        }
    }

    private fun loadSession(id: String?) {
        if (busy) return
        running = false
        currentId = id
        session = null
        perform({ _, store -> store.get(id) ?: throw IllegalArgumentException() }, false)
    }

    private fun dates(s: HistoricalImport.Session): String {
        val z = TimeZone.getTimeZone(s.timezone).toZoneId()
        return "${Instant.ofEpochMilli(s.from).atZone(z).toLocalDate()} → ${Instant.ofEpochMilli(s.to).atZone(z).toLocalDate()} (${s.timezone})"
    }

    private fun destinationName(value: HistoricalImport.Session): String? {
        for (choice in choices) if (choice.id == value.calendar) return choice.name
        return getString(R.string.auto_destination_unavailable)
    }

    private fun typeNames(value: HistoricalImport.Session): String {
        val names = ArrayList<String>()
        if (value.incoming) names.add(getString(R.string.auto_incoming))
        if (value.outgoing) names.add(getString(R.string.auto_outgoing))
        if (value.missed) names.add(getString(R.string.auto_missed))
        return android.text.TextUtils.join(", ", names)
    }

    private fun render() {
        val value = session!!
        summary!!.text =
            getString(
                R.string.import_progress,
                dates(value),
                getString(phaseLabel(value.phase)),
                destinationName(value),
                typeNames(value),
                value.count,
                value.saved,
                value.review,
            )
    }

    private fun phaseLabel(phase: HistoricalImport.Phase): Int =
        when (phase) {
            HistoricalImport.Phase.BUILDING -> R.string.import_building
            HistoricalImport.Phase.PREVIEW -> R.string.import_preview
            HistoricalImport.Phase.CONFIRMED -> R.string.import_confirmed
            HistoricalImport.Phase.COMPLETE -> R.string.import_complete
            HistoricalImport.Phase.CANCELLED -> R.string.import_cancelled
            else -> R.string.import_review
        }

    private fun loadHistory(before: Long) {
        executor.execute {
            try {
                SqliteImportStore(applicationContext).use { store ->
                    val records = store.sessions(before, 11)
                    runOnUiThread {
                        if (isDestroyed) return@runOnUiThread
                        historyCursor = before
                        history!!.removeAllViews()
                        val size = minOf(10, records.size)
                        for (i in 0 until size) {
                            val item = records[i].session!!
                            val open = Button(this)
                            open.text =
                                getString(
                                    R.string.import_history_item,
                                    dates(item),
                                    getString(phaseLabel(item.phase)),
                                )
                            open.setOnClickListener { loadSession(item.id) }
                            history!!.addView(open)
                        }
                        if (size == 0) {
                            val empty = TextView(this)
                            empty.setText(R.string.import_empty_history)
                            history!!.addView(empty)
                        }
                        nextHistoryCursor = if (records.size > 10) records[9].cursor else -1
                        older!!.isEnabled = nextHistoryCursor > 0
                        newer!!.isEnabled = historyBack.isNotEmpty()
                    }
                }
            } catch (failed: RuntimeException) {
                showFailure()
            }
        }
    }

    private fun showFailure() {
        runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            feedback!!.setText(R.string.import_failed)
            buttons()
        }
    }

    public override fun onPause() {
        running = false
        super.onPause()
    }

    public override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("zone", zone)
        out.putString("session", currentId)
        out.putString("from", from!!.text.toString())
        out.putString("to", to!!.text.toString())
        out.putLong("calendar", selected)
        out.putBoolean("incoming", incoming!!.isChecked)
        out.putBoolean("outgoing", outgoing!!.isChecked)
        out.putBoolean("missed", missed!!.isChecked)
    }

    public override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        grants: IntArray,
    ) {
        super.onRequestPermissionsResult(code, permissions, grants)
        loadCalendars(selected)
        buttons()
    }

    public override fun onDestroy() {
        running = false
        executor.shutdown()
        super.onDestroy()
    }
}
