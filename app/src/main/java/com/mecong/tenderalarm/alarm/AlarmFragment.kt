package com.mecong.tenderalarm.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mecong.tenderalarm.AdditionalActivity
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.setBootReceiverActive
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification
import com.mecong.tenderalarm.logs.LogsActivity
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import kotlinx.android.synthetic.main.content_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*


class AlarmFragment : Fragment() {
    private var alarmsAdapter: AlarmsListCursorAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = this.activity!!

        if (BuildConfig.DEBUG) {
            textNextAlarm.setOnClickListener {
                val addAlarmIntent = Intent(context, LogsActivity::class.java)
                startActivity(addAlarmIntent)
            }
        }

        ibtnInfo.setOnClickListener {
            val additionalIntent = Intent(context, AdditionalActivity::class.java)
            this@AlarmFragment.startActivity(additionalIntent)
        }

        AlarmUtils.resetupAllAlarms(context)

        val sqLiteDBHelper = sqLiteDBHelper(context)
        alarmsAdapter = AlarmsListCursorAdapter(this, sqLiteDBHelper!!.allAlarms)

        alarms_list.adapter = alarmsAdapter
        updateNextActiveAlarm(sqLiteDBHelper)

    }

    @Subscribe
    fun onAddAlarmMessageHandler(message: MainActivityMessages) {
        if (message == MainActivityMessages.ADD_ALARM) {
            val addAlarmIntent = Intent(context, AlarmAddingActivity::class.java)
            this@AlarmFragment.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        return inflater.inflate(
                R.layout.content_main, container, false) as ViewGroup
    }

    private fun updateNextActiveAlarm(sqLiteDBHelper: SQLiteDBHelper?) {
        val context = this.context ?: return
        val nextActiveAlarm = sqLiteDBHelper!!.nextActiveAlarm
        if (nextActiveAlarm != null) {
            setBootReceiverActive(context)
            val calendar = Calendar.getInstance()
            val nextAlarmTime = nextActiveAlarm.nextNotCanceledTime
            val difference = nextAlarmTime - calendar.timeInMillis
            calendar.time = Date(difference)
            calendar.timeZone = TimeZone.getTimeZone("UTC")
            when {
                difference < 0 -> {
                    textNextAlarm!!.setText(R.string.all_alarms_are_off)
                }
                difference < MINUTE -> {
                    textNextAlarm!!.text = context.getString(R.string.next_alarm_soon)
                }
                difference < HOUR -> {
                    val minutes = calendar[Calendar.MINUTE]
                    textNextAlarm!!.text = context.resources.getQuantityString(R.plurals.next_alarm_within_hour, minutes, minutes)
                }
                difference < DAY -> {
                    val hours = calendar[Calendar.HOUR_OF_DAY]
                    val minutes = calendar[Calendar.MINUTE]
                    val nHours = context.resources.getQuantityString(R.plurals.n_hours_plural, hours, hours)
                    val nMinutes = context.resources.getQuantityString(R.plurals.n_minutes_plural_short, minutes, minutes)

                    textNextAlarm!!.text = context.getString(R.string.next_alarm_today, nHours, nMinutes)
                }
                else -> {
                    val days = calendar[Calendar.DAY_OF_YEAR] - 1
                    textNextAlarm!!.text = context.resources.getQuantityString(R.plurals.next_alarm, days, days)
                }
            }
            textNextAlarmDate!!.text = context
                    .getString(R.string.next_alarm_date_time, nextAlarmTime)
        } else {
            val alarmsCount = sqLiteDBHelper.alarmsCount()
            if (alarmsCount == 0L) {
                textNextAlarm!!.setText(R.string.your_alarms)

            } else {
                textNextAlarm!!.setText(R.string.all_alarms_are_off)
            }
            textNextAlarmDate!!.text = ""
        }
        setUpNextSleepTimeNotification(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val context: Context? = this.activity
        val sqLiteDBHelper = sqLiteDBHelper(context!!)!!
        // Check which request we're responding to
        // Make sure the request was successful
        if (requestCode == ALARM_ADDING_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
            updateNextActiveAlarm(sqLiteDBHelper)
        }
//        sqLiteDBHelper.close()
    }

    override fun onStart() {
        super.onStart()

        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        val autoStartTurnedOn = sqLiteDBHelper.getPropertyInt(PropertyName.AUTOSTART_TURNED_ON)
        if (autoStartTurnedOn == 0) {
            AutoStartUtils.runAutostartIfSupported(this.context!!)
            sqLiteDBHelper.setPropertyString(PropertyName.AUTOSTART_TURNED_ON, "1")
        }
    }

    override fun onResume() {
        super.onResume()
        val sqLiteDBHelper = sqLiteDBHelper(this.activity!!)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper!!.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
    }

    fun deleteAlarm(id: String?) {
        val context: Context = this.activity!!
        AlarmUtils.turnOffAlarm(id, context)
        val sqLiteDBHelper = sqLiteDBHelper(context)
        sqLiteDBHelper!!.deleteAlarm(id!!)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
//        sqLiteDBHelper.close()
    }

    fun setActive(id: String, active: Boolean) {
        val context: Context = this.activity!!
        val sqLiteDBHelper = sqLiteDBHelper(context)
        sqLiteDBHelper!!.toggleAlarmActive(id, active)
        if (active) {
            setUpNextAlarm(id, context, true)
        } else {
            AlarmUtils.turnOffAlarm(id, context)
        }
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
    }

    fun cancelNextAlarms(id: String, num: Int) {
        val context: Context? = this.activity
        val sqLiteDBHelper = sqLiteDBHelper(context!!)!!
        val alarmById = sqLiteDBHelper.getAlarmById(id)
        alarmById!!.canceledNextAlarms = num
        sqLiteDBHelper.addOrUpdateAlarm(alarmById)
        setUpNextAlarm(id, context, true)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
    }

    fun editAlarm(id: String) {
        val addAlarmIntent = Intent(this.activity, AlarmAddingActivity::class.java)
        addAlarmIntent.putExtra("alarmId", id)
        this@AlarmFragment.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE)
    }

    companion object {
        private const val ALARM_ADDING_REQUEST_CODE = 42
    }
}