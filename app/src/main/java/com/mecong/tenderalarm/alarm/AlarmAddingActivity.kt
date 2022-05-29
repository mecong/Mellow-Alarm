package com.mecong.tenderalarm.alarm

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.util.Strings
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.databinding.ActivityAlarmAddingBinding
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.utils.OnSeekBarChangeAdapter
import java.util.*

class AlarmAddingActivity : AppCompatActivity() {
  private var selectedMinute = 0
  private var selectedHour = 0
  private var selectedYear = 0
  private var selectedMonth = 0
  private var getSelectedDayOfMonth = 0
  private var exactDate: Long = 0
  private var alarmId: String? = "-1"
  private var melodyUrl: String? = null

  private lateinit var binding: ActivityAlarmAddingBinding


  fun timeTextViewClick(v: View?) { // Launch Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
      this,
      android.R.style.Theme_DeviceDefault_Dialog,
      { _, hourOfDay, minute ->
        selectedHour = hourOfDay
        selectedMinute = minute
        if (exactDate > 0) {
          val c = Calendar.getInstance()
          c.timeInMillis = exactDate
          c.set(Calendar.HOUR_OF_DAY, selectedHour)
          c.set(Calendar.MINUTE, selectedMinute)
          exactDate = c.timeInMillis
        }
        updateTimeTextView()
      }, selectedHour, selectedMinute, true
    )


    timePickerDialog.show()
  }

  fun selectDateClick(v: View?) { // Launch Time Picker Dialog
    val datePickerDialog = DatePickerDialog(
      this,
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
      }, selectedYear, selectedMonth, getSelectedDayOfMonth
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
    datePickerDialog.show()
  }

  private fun uncheckDaysCheckboxes() {
    with(binding) {
      checkBoxMo.isChecked = false
      checkBoxTu.isChecked = false
      checkBoxWe.isChecked = false
      checkBoxTh.isChecked = false
      checkBoxFr.isChecked = false
      checkBoxSa.isChecked = false
      checkBoxSu.isChecked = false
    }
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
        //HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.toString())
        binding.txtMelody.text = dumpFileMetaData(uri)
        melodyUrl = uri.toString()
        AsyncTask.execute {
          this.contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }
    }
  }

  @SuppressLint("Range")
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
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          )
          //HyperLog.i(AlarmUtils.TAG, "Display Name: $displayName")
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          // If the size is unknown, the value stored is null.  But since an
// int can't be null in Java, the behavior is implementation-specific,
// which is just a fancy term for "unpredictable".  So as
// a rule, check if it's null before assigning to an int.  This will
// happen often:  The storage API allows for remote files, whose
// size might not be locally known.
          val size: String
          size =
            if (!cursor.isNull(sizeIndex)) { // Technically the column stores an int, but cursor.getString()
// will do the conversion automatically.
              cursor.getString(sizeIndex)
            } else {
              "Unknown"
            }
          //HyperLog.i(AlarmUtils.TAG, "Size: $size")
          return displayName
        }
      }
    return null
  }

  private fun updateTimeTextView() {
    binding.timeTextView.text = baseContext.getString(R.string.alarm_time, selectedHour, selectedMinute)
  }

  private fun updateDateTextView() {
    if (exactDate == 0L) {
      binding.dateTextView.visibility = View.GONE
    } else {
      binding.dateTextView.visibility = View.VISIBLE
      binding.dateTextView.text = this.getString(R.string.next_alarm_date, exactDate)
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
    with(binding) {
      checkBoxMo.isChecked = !checkBoxMo.isChecked
      checkBoxTu.isChecked = !checkBoxTu.isChecked
      checkBoxWe.isChecked = !checkBoxWe.isChecked
      checkBoxTh.isChecked = !checkBoxTh.isChecked
      checkBoxFr.isChecked = !checkBoxFr.isChecked
    }


    exactDate = 0
    updateDateTextView()
  }

  fun allDaysClick(v: View?) {
    workDaysClick(v)
    binding.checkBoxSa.isChecked = !binding.checkBoxSa.isChecked
    binding.checkBoxSu.isChecked = !binding.checkBoxSu.isChecked

    exactDate = 0
    updateDateTextView()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityAlarmAddingBinding.inflate(layoutInflater)


    this.window.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
    )

    initLogsAndControls()

    val extras = intent.extras
    if (extras != null) {
      alarmId = extras.getString("alarmId")
    }

    binding.txtTicks.text = this.resources.getQuantityString(
      R.plurals.ticks_label,
      binding.seekBarTicks!!.progress,
      binding.seekBarTicks!!.progress
    )
    binding.seekBarTicks.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.txtTicks.text =
          this@AlarmAddingActivity.resources.getQuantityString(R.plurals.ticks_label, progress, progress)
        binding.ticksTypeLayout.visibility = if (progress > 0) View.VISIBLE else View.GONE
      }
    })

    binding.txtSnooze.text = this.resources.getQuantityString(
      R.plurals.snooze,
      binding.seekBarSnooze!!.progress,
      binding.seekBarSnooze!!.progress
    )
    binding.seekBarSnooze.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.txtSnooze.text =
          this@AlarmAddingActivity.resources.getQuantityString(R.plurals.snooze, progress, progress)
      }
    })

    binding.txtComplexity.text = this.getString(R.string.alarm_complexity, binding.seekBarComplexity!!.progress)
    binding.seekBarComplexity.setOnSeekBarChangeListener(object : OnSeekBarChangeAdapter() {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (progress < 1) {
          binding.seekBarComplexity.progress = 1
        } else {
          binding.txtComplexity.text =
            this@AlarmAddingActivity.getString(R.string.alarm_complexity, progress)
        }
      }
    })

    val melodySelect = View.OnClickListener { performFileSearch() }
    binding.txtMelody.setOnClickListener(melodySelect)
    binding.txtMelodyCaption.setOnClickListener(melodySelect)

    val resetExactDate: (v: View) -> Unit = {
      exactDate = 0
      updateDateTextView()
    }
    binding.checkBoxMo.setOnClickListener(resetExactDate)
    binding.checkBoxTu.setOnClickListener(resetExactDate)
    binding.checkBoxWe.setOnClickListener(resetExactDate)
    binding.checkBoxTh.setOnClickListener(resetExactDate)
    binding.checkBoxFr.setOnClickListener(resetExactDate)
    binding.checkBoxSa.setOnClickListener(resetExactDate)
    binding.checkBoxSu.setOnClickListener(resetExactDate)

    ArrayAdapter.createFromResource(
      this,
      R.array.ticks_type_array,
      android.R.layout.simple_spinner_item
    ).also { adapter ->
      // Specify the layout to use when the list of choices appears
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      // Apply the adapter to the spinner
      binding.spinnerTicksType.adapter = adapter
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)

    if (alarmId == "-1") {
      initFormForNewAlarm()
      binding.timeTextView.performClick()
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

    binding.txtAlarmMessage.setText(entity.message)

    exactDate = entity.exactDate
    val c = Calendar.getInstance()
    if (exactDate > 0) c.timeInMillis = exactDate
    selectedYear = c[Calendar.YEAR]
    selectedMonth = c[Calendar.MONTH]

    getSelectedDayOfMonth = c[Calendar.DAY_OF_MONTH]
    binding.seekBarTicks.progress = entity.ticksTime

    val daysAsMap = entity.daysAsMap
    with(binding) {
      checkBoxMo.isChecked = true == daysAsMap[R.string.mo]
      checkBoxTu.isChecked = true == daysAsMap[R.string.tu]
      checkBoxWe.isChecked = true == daysAsMap[R.string.we]
      checkBoxTh.isChecked = true == daysAsMap[R.string.th]
      checkBoxFr.isChecked = true == daysAsMap[R.string.fr]
      checkBoxSa.isChecked = true == daysAsMap[R.string.sa]
      checkBoxSu.isChecked = true == daysAsMap[R.string.su]

      seekBarComplexity.progress = entity.complexity
      seekBarSnooze.progress = entity.snoozeMaxTimes
    }

    if (!Strings.isEmptyOrWhitespace(entity.melodyName)) {
      binding.txtMelody.text = entity.melodyName
      melodyUrl = entity.melodyUrl
    }

    binding.chbHeadsUp.isChecked = entity.isHeadsUp
    binding.chbTimeToSleepNotification.isChecked = entity.isTimeToSleepNotification
    binding.chbIncreaseVolume.isChecked = entity.increaseVolume > 0
    binding.chbVibroAlarm.isChecked = entity.vibrationType != null

    binding.spinnerTicksType.setSelection(entity.ticksType)
  }

  private fun initFormForNewAlarm() {
    val c = Calendar.getInstance()
    selectedHour = c[Calendar.HOUR_OF_DAY]
    selectedMinute = c[Calendar.MINUTE]
    selectedYear = c[Calendar.YEAR]
    selectedMonth = c[Calendar.MONTH]
    getSelectedDayOfMonth = c[Calendar.DAY_OF_MONTH]
    binding.seekBarComplexity.progress = 1
    binding.chbHeadsUp.isChecked = true
    binding.chbTimeToSleepNotification.isChecked = true
    binding.chbVibroAlarm.isChecked = false
  }

  private fun initLogsAndControls() {
    setContentView(binding.root)
  }

  private fun saveToDB() {
    val days = AlarmEntity.daysMarshaling(
      binding.checkBoxMo.isChecked, binding.checkBoxTu.isChecked,
      binding.checkBoxWe.isChecked, binding.checkBoxTh.isChecked,
      binding.checkBoxFr.isChecked, binding.checkBoxSa.isChecked,
      binding.checkBoxSu.isChecked
    )

    val alarmEntity = AlarmEntity(
      id = alarmId!!.toLong(),
      hour = selectedHour,
      minute = selectedMinute,
      exactDate = exactDate,
      complexity = binding.seekBarComplexity.progress,
      days = days,
      message = message,
      melodyName = binding.txtMelody.text.toString(),
      melodyUrl = melodyUrl,
      snoozeMaxTimes = binding.seekBarSnooze.progress,
      ticksType = binding.spinnerTicksType.selectedItemPosition,
      ticksTime = binding.seekBarTicks.progress,
      isHeadsUp = binding.chbHeadsUp.isChecked,
      increaseVolume = if (binding.chbIncreaseVolume.isChecked) 5 else 0,
      isTimeToSleepNotification = binding.chbTimeToSleepNotification.isChecked,
      vibrationType = if (binding.chbVibroAlarm.isChecked) "1" else null
    )

    val sqLiteDBHelper = sqLiteDBHelper(this)
    alarmEntity.id = sqLiteDBHelper!!.addOrUpdateAlarm(alarmEntity)
    setUpNextAlarm(alarmEntity, this, true)
  }

  private val message: String
    get() {
      val message = binding.txtAlarmMessage.text.toString()
      val defaultMessage = "Alarm Clock"
      return if (Strings.isEmptyOrWhitespace(message)) defaultMessage else message
    }

  companion object {
    private const val READ_REQUEST_CODE = 42
  }
}