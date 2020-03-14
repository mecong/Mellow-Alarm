package com.mecong.tenderalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.TAG
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper

class TenderAlarmReceiver : BroadcastReceiver() {
    //adb shell dumpsys alarm > alarms.dump
    override fun onReceive(context: Context, intent: Intent) {
        HyperLog.initialize(context)
        HyperLog.setLogLevel(Log.INFO)
        val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
        val sqLiteDBHelper = sqLiteDBHelper(context)
        val entity = sqLiteDBHelper!!.getAlarmById(alarmId)
        SleepTimeAlarmReceiver.cancelNotification(context)
        UpcomingAlarmNotificationReceiver.cancelNotification(context)
        val canceledNextAlarms = entity!!.canceledNextAlarms
        HyperLog.i(TAG, "TenderAlarmReceiver received alarm: $entity")
        if (canceledNextAlarms == 0) {
            if (entity.days > 0) {
                setUpNextAlarm(entity, context, false)
            } else {
                sqLiteDBHelper.toggleAlarmActive(alarmId, false)
            }
            startAlarmNotification(context, alarmId)
        } else {
            if (entity.days > 0) {
                entity.canceledNextAlarms = canceledNextAlarms - 1
                sqLiteDBHelper.addOrUpdateAlarm(entity)
                setUpNextAlarm(entity, context, false)
            }
        }
    }

    private fun startAlarmNotification(context: Context, alarmId: String) {
        val startAlarmIntent = Intent(context, AlarmNotifyingService::class.java)
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startAlarmIntent)
        } else {
            context.startService(startAlarmIntent)
        }
    }
}