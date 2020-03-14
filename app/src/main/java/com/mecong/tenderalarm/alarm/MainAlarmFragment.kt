package com.mecong.tenderalarm.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.AdditionalActivity
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.MINUTE
import com.mecong.tenderalarm.alarm.AlarmUtils.setBootReceiverActive
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification
import com.mecong.tenderalarm.logs.LogsActivity
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class MainAlarmFragment : Fragment() {
    private var alarmsAdapter: AlarmsListCursorAdapter? = null

    //    private var textNextAlarm: TextView? = null
//    private var textNextAlarmDate: TextView? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val context = this.activity!!
        HyperLog.initialize(context)
        HyperLog.setLogLevel(Log.INFO)
        textNextAlarm.setOnClickListener(View.OnClickListener {
            val addAlarmIntent = Intent(context, LogsActivity::class.java)
            startActivity(addAlarmIntent)
        })

        ibtnInfo.setOnClickListener {
            val additionalIntent = Intent(context, AdditionalActivity::class.java)
            this@MainAlarmFragment.startActivity(additionalIntent)
        }
        val sqLiteDBHelper = sqLiteDBHelper(context)
        alarmsAdapter = AlarmsListCursorAdapter(this, sqLiteDBHelper!!.allAlarms)

        alarms_list.adapter = alarmsAdapter
        updateNextActiveAlarm(sqLiteDBHelper)
        ibtnAddAlarm.setOnClickListener {
            val addAlarmIntent = Intent(context, AlarmAddingActivity::class.java)
            this@MainAlarmFragment.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE)
        }
        sqLiteDBHelper.close()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.content_main, container, false) as ViewGroup

        return rootView
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
            if (difference < 0) {
                textNextAlarm!!.setText(R.string.all_alarms_are_off)
            } else if (difference < MINUTE) {
                textNextAlarm!!.text = context.getString(R.string.next_alarm_soon)
            } else if (difference < AlarmUtils.HOUR) {
                textNextAlarm!!.text = context.getString(R.string.next_alarm_within_hour,
                        calendar[Calendar.MINUTE])
            } else if (difference < AlarmUtils.DAY) {
                textNextAlarm!!.text = context.getString(R.string.next_alarm_today,
                        calendar[Calendar.HOUR_OF_DAY], calendar[Calendar.MINUTE])
            } else {
                textNextAlarm!!.text = context.getString(R.string.next_alarm,
                        calendar[Calendar.DAY_OF_YEAR])
            }
            textNextAlarmDate!!.text = context
                    .getString(R.string.next_alarm_date_time, nextAlarmTime)
        } else {
            textNextAlarm!!.setText(R.string.all_alarms_are_off)
            textNextAlarmDate!!.text = ""
        }
        setUpNextSleepTimeNotification(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val context: Context? = this.activity
        val sqLiteDBHelper = sqLiteDBHelper(context!!)
        // Check which request we're responding to
// Make sure the request was successful
        if (requestCode == ALARM_ADDING_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            alarmsAdapter!!.changeCursor(sqLiteDBHelper!!.allAlarms)
            updateNextActiveAlarm(sqLiteDBHelper)
            //            Snackbar.make(this.textNextAlarm,
//                    "New alarm created", LENGTH_SHORT)
//                    .setAction("Action", null).show();
        }
        sqLiteDBHelper!!.close()
    }

    override fun onResume() {
        super.onResume()
        val sqLiteDBHelper = sqLiteDBHelper(this.activity!!)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper!!.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
    }

    fun deleteAlarm(id: String?) {
        val context: Context? = this.activity
        val sqLiteDBHelper = sqLiteDBHelper(context!!)
        AlarmUtils.turnOffAlarm(id, context)
        sqLiteDBHelper!!.deleteAlarm(id!!)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
        sqLiteDBHelper.close()
    }

    fun setActive(id: String, active: Boolean) {
        val context: Context = this.activity!!
        if (active) {
            setUpNextAlarm(id, context, true)
        } else {
            AlarmUtils.turnOffAlarm(id, context)
        }
        val sqLiteDBHelper = sqLiteDBHelper(context)
        sqLiteDBHelper!!.toggleAlarmActive(id, active)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
        sqLiteDBHelper.close()
    }

    fun cancelNextAlarms(id: String, num: Int) {
        val context: Context? = this.activity
        val sqLiteDBHelper = sqLiteDBHelper(context!!)
        val alarmById = sqLiteDBHelper!!.getAlarmById(id)
        alarmById!!.canceledNextAlarms = num
        sqLiteDBHelper.addOrUpdateAlarm(alarmById)
        setUpNextAlarm(id, context, true)
        alarmsAdapter!!.changeCursor(sqLiteDBHelper.allAlarms)
        updateNextActiveAlarm(sqLiteDBHelper)
        sqLiteDBHelper.close()
    }

    fun editAlarm(id: String) {
        val addAlarmIntent = Intent(this.activity, AlarmAddingActivity::class.java)
        addAlarmIntent.putExtra("alarmId", id)
        this@MainAlarmFragment.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE)
    }

    companion object {
        private const val ALARM_ADDING_REQUEST_CODE = 42
    }
}