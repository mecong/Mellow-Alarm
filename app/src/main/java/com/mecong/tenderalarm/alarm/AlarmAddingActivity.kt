package com.mecong.tenderalarm.alarm

import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.util.Strings
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.utils.OnSeekBarChangeAdapter
import kotlinx.android.synthetic.main.activity_alarm_adding.*
import java.util.*

class AlarmAddingActivity : AppCompatActivity() {
    private var selectedMinute = 0
    private var selectedHour = 0
    private var selectedYear = 0
    private var selectedMonth = 0
    private var getSelectedDayOfMonth = 0
    private var exactDate: Long = 0
    private var alarmId: String? = "0"
    private var melodyUrl: String? = null

    fun timeTextViewClick(v: View?) { // Launch Time Picker Dialog
        val timePickerDialog = TimePickerDialog(this,
                android.R.style.Theme_DeviceDefault_Dialog,
                OnTimeSetListener { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    updateTimeTextView()
                }, selectedHour, selectedMinute, true)
        timePickerDialog.show()
    }

    fun selectDateClick(v: View?) { // Launch Time Picker Dialog
        val datePickerDialog = DatePickerDialog(this,
                android.R.style.Theme_DeviceDefault_Dialog,
                OnDateSetListener { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    getSelectedDayOfMonth = dayOfMonth
                    val c = Calendar.getInstance()
                    c[selectedYear, selectedMonth, getSelectedDayOfMonth, selectedHour, selectedMinute] = 0
                    exactDate = c.timeInMillis
                    uncheckDaysCheckboxes()
                    updateDateTextView()
                }, selectedYear, selectedMonth, getSelectedDayOfMonth)
        datePickerDialog.show()
    }

    private fun uncheckDaysCheckboxes() {
        checkBoxMo.isChecked = false
        checkBoxTu.isChecked = false
        checkBoxWe.isChecked = false
        checkBoxTh.isChecked = false
        checkBoxFr.isChecked = false
        checkBoxSa.isChecked = false
        checkBoxSu.isChecked = false
    }

    private fun performFileSearch() { // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
// browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // Filter to only show results that can be "opened", such as a
// file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // Filter to show only images, using the image MIME data type.
// If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
// To search for all documents available via installed storage providers,
// it would be "*/*".
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
// READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
// response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) { // The document selected by the user won't be returned in the intent.
// Instead, a URI to that document will be contained in the return intent
// provided to this method as a parameter.
// Pull that URI using resultData.getData().
            if (resultData != null) {
                val uri = resultData.data
                HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.toString())
                txtMelody!!.text = dumpFileMetaData(uri)
                melodyUrl = uri.toString()
                AsyncTask.execute {
                    this.contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    }

    private fun dumpFileMetaData(uri: Uri?): String? { // The query, since it only applies to a single document, will only return
// one row. There's no need to filter, sort, or select fields, since we want
// all fields for one document.
        this.contentResolver
                .query(uri!!, null, null, null, null, null).use { cursor ->
                    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
// "if there's anything to look at, look at it" conditionals.
                    if (cursor != null && cursor.moveToFirst()) { // Note it's called "Display Name".  This is
// provider-specific, and might not necessarily be the file name.
                        val displayName = cursor.getString(
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        HyperLog.i(AlarmUtils.TAG, "Display Name: $displayName")
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        // If the size is unknown, the value stored is null.  But since an
// int can't be null in Java, the behavior is implementation-specific,
// which is just a fancy term for "unpredictable".  So as
// a rule, check if it's null before assigning to an int.  This will
// happen often:  The storage API allows for remote files, whose
// size might not be locally known.
                        val size: String
                        size = if (!cursor.isNull(sizeIndex)) { // Technically the column stores an int, but cursor.getString()
// will do the conversion automatically.
                            cursor.getString(sizeIndex)
                        } else {
                            "Unknown"
                        }
                        HyperLog.i(AlarmUtils.TAG, "Size: $size")
                        return displayName
                    }
                }
        return null
    }

    private fun updateTimeTextView() {
        timeTextView.text = baseContext.getString(R.string.alarm_time, selectedHour, selectedMinute)
    }

    private fun updateDateTextView() {
        if (exactDate == 0L) {
            dateTextView.visibility = View.GONE
        } else {
            dateTextView.visibility = View.VISIBLE
            dateTextView.text = this.getString(R.string.next_alarm_date, exactDate)
        }
    }

    fun buttonOkClick(v: View?) {
        saveToDB()
        val data = Intent()
        //---set the data to pass back---
        data.data = Uri.parse("Result to be returned....")
        setResult(Activity.RESULT_OK, data)
        //---close the activity---
        finish()
    }

    fun buttonCancelClick(v: View?) {
        setResult(Activity.RESULT_CANCELED, null)
        finish()
    }

    fun workDaysClick(v: View?) {
        checkBoxMo.isChecked = !checkBoxMo.isChecked
        checkBoxTu.isChecked = !checkBoxTu.isChecked
        checkBoxWe.isChecked = !checkBoxWe.isChecked
        checkBoxTh.isChecked = !checkBoxTh.isChecked
        checkBoxFr.isChecked = !checkBoxFr.isChecked

        exactDate = 0
        updateDateTextView()
    }

    fun allDaysClick(v: View?) {
        workDaysClick(v)
        checkBoxSa.isChecked = !checkBoxSa.isChecked
        checkBoxSu.isChecked = !checkBoxSu.isChecked

        exactDate = 0
        updateDateTextView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        initLogsAndControls()

        val extras = intent.extras
        if (extras != null) {
            alarmId = extras.getString("alarmId")
        }

        txtTicks.text = this.resources.getQuantityString(R.plurals.ticks_label, seekBarTicks!!.progress, seekBarTicks!!.progress)
        seekBarTicks.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                txtTicks.text = this@AlarmAddingActivity.resources.getQuantityString(R.plurals.ticks_label, progress, progress)
                ticksTypeLayout.visibility = if (progress > 0) View.VISIBLE else View.GONE
            }
        })

        txtSnooze.text = this.resources.getQuantityString(R.plurals.snooze, seekBarSnooze!!.progress, seekBarSnooze!!.progress)
        seekBarSnooze.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                txtSnooze!!.text = this@AlarmAddingActivity.resources.getQuantityString(R.plurals.snooze, progress, progress)
            }
        })

        txtComplexity.text = this.getString(R.string.alarm_complexity, seekBarComplexity!!.progress)
        seekBarComplexity.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress < 1) {
                    seekBarComplexity!!.progress = 1
                } else {
                    txtComplexity!!.text = this@AlarmAddingActivity.getString(R.string.alarm_complexity, progress)
                }
            }
        })

        val melodySelect = View.OnClickListener { performFileSearch() }
        txtMelody.setOnClickListener(melodySelect)
        txtMelodyCaption.setOnClickListener(melodySelect)

        val resetExactDate: (v: View) -> Unit = {
            exactDate = 0
            updateDateTextView()
        }
        checkBoxMo.setOnClickListener(resetExactDate)
        checkBoxTu.setOnClickListener(resetExactDate)
        checkBoxWe.setOnClickListener(resetExactDate)
        checkBoxTh.setOnClickListener(resetExactDate)
        checkBoxFr.setOnClickListener(resetExactDate)
        checkBoxSa.setOnClickListener(resetExactDate)
        checkBoxSu.setOnClickListener(resetExactDate)

        ArrayAdapter.createFromResource(
                this,
                R.array.ticks_type_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinnerTicksType.adapter = adapter
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (alarmId == "0") {
            initFormForNewAlarm()
            timeTextView.performClick()
        } else {
            initFormForExistingAlarm(alarmId)
        }
        updateTimeTextView()
        updateDateTextView()
    }

    private fun initFormForExistingAlarm(alarmId: String?) {
        val sqLiteDBHelper = sqLiteDBHelper(this)
        val entity = sqLiteDBHelper!!.getAlarmById(alarmId)

        selectedMinute = entity!!.minute
        selectedHour = entity.hour

        txtAlarmMessage!!.setText(entity.message)

        exactDate = entity.exactDate
        val c = Calendar.getInstance()
        if (exactDate > 0) c.timeInMillis = exactDate
        selectedYear = c[Calendar.YEAR]
        selectedMonth = c[Calendar.MONTH]

        getSelectedDayOfMonth = c[Calendar.DAY_OF_MONTH]
        seekBarTicks!!.progress = entity.ticksTime

        val daysAsMap = entity.daysAsMap
        checkBoxMo!!.isChecked = true == daysAsMap[R.string.mo]
        checkBoxTu!!.isChecked = true == daysAsMap[R.string.tu]
        checkBoxWe!!.isChecked = true == daysAsMap[R.string.we]
        checkBoxTh!!.isChecked = true == daysAsMap[R.string.th]
        checkBoxFr!!.isChecked = true == daysAsMap[R.string.fr]
        checkBoxSa!!.isChecked = true == daysAsMap[R.string.sa]
        checkBoxSu!!.isChecked = true == daysAsMap[R.string.su]

        seekBarComplexity!!.progress = entity.complexity
        seekBarSnooze!!.progress = entity.snoozeMaxTimes
        if (!Strings.isEmptyOrWhitespace(entity.melodyName)) {
            txtMelody!!.text = entity.melodyName
            melodyUrl = entity.melodyUrl
        }

        chbHeadsUp!!.isChecked = entity.isHeadsUp
        chbTimeToSleepNotification!!.isChecked = entity.isTimeToSleepNotification

        spinnerTicksType.setSelection(entity.ticksType)
    }

    private fun initFormForNewAlarm() {
        val c = Calendar.getInstance()
        selectedHour = c[Calendar.HOUR_OF_DAY]
        selectedMinute = c[Calendar.MINUTE]
        selectedYear = c[Calendar.YEAR]
        selectedMonth = c[Calendar.MONTH]
        getSelectedDayOfMonth = c[Calendar.DAY_OF_MONTH]
        seekBarComplexity!!.progress = 1
        chbHeadsUp!!.isChecked = true
        chbTimeToSleepNotification!!.isChecked = true
    }

    private fun initLogsAndControls() {
        HyperLog.initialize(this)
        HyperLog.setLogLevel(Log.VERBOSE)
        HyperLog.i(AlarmUtils.TAG, "Start AlarmAddingActivity")
        setContentView(R.layout.activity_alarm_adding)
    }

    private fun saveToDB() {
        val days = AlarmEntity.daysMarshaling(checkBoxMo.isChecked, checkBoxTu.isChecked,
                checkBoxWe.isChecked, checkBoxTh.isChecked,
                checkBoxFr.isChecked, checkBoxSa.isChecked,
                checkBoxSu.isChecked)

        val alarmEntity = AlarmEntity(
                id = alarmId!!.toLong(),
                hour = selectedHour,
                minute = selectedMinute,
                exactDate = exactDate,
                complexity = seekBarComplexity!!.progress,
                days = days,
                message = message,
                melodyName = txtMelody!!.text.toString(),
                melodyUrl = melodyUrl,
                snoozeMaxTimes = seekBarSnooze!!.progress,
                ticksType = spinnerTicksType.selectedItemPosition,
                ticksTime = seekBarTicks!!.progress,
                isHeadsUp = chbHeadsUp!!.isChecked,
                isTimeToSleepNotification = chbTimeToSleepNotification!!.isChecked)

        val sqLiteDBHelper = sqLiteDBHelper(this)
        alarmEntity.id = sqLiteDBHelper!!.addOrUpdateAlarm(alarmEntity)
        setUpNextAlarm(alarmEntity, this, true)
    }

    private val message: String
        get() {
            val message = txtAlarmMessage!!.text.toString()
            val defaultMessage = "Alarm Clock"
            return if (Strings.isEmptyOrWhitespace(message)) defaultMessage else message
        }

    companion object {
        private const val READ_REQUEST_CODE = 42
    }
}