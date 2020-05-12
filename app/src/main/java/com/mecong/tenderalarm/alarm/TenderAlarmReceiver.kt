package com.mecong.tenderalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mecong.tenderalarm.alarm.AlarmNotifyingService.Companion.ALARM_PLAYING
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM_SAME_ID
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import timber.log.Timber

class TenderAlarmReceiver : BroadcastReceiver() {
    //adb shell dumpsys alarm > alarms.dump
    override fun onReceive(context: Context, intent: Intent) {
//        HyperLog.initialize(context)
//        HyperLog.setLogLevel(Log.ERROR)
        val alarmId = intent.getStringExtra(ALARM_ID_PARAM) ?: return
        val sqLiteDBHelper = sqLiteDBHelper(context)
        val entity = sqLiteDBHelper!!.getAlarmById(alarmId)
        SleepTimeAlarmReceiver.cancelNotification(context)
        UpcomingAlarmNotificationReceiver.cancelNotification(context)
        val canceledNextAlarms = entity!!.canceledNextAlarms
        Timber.i("TenderAlarmReceiver received alarm: $entity, ALARM_PLAYING: $ALARM_PLAYING")
        if (canceledNextAlarms == 0) {
            if (entity.days > 0) {
                setUpNextAlarm(entity, context, false)
                AlarmUtils.setUpNextSleepTimeNotification(context)
            } else {
                sqLiteDBHelper.toggleAlarmActive(alarmId, false)
            }

            if (ALARM_PLAYING == null) {
                ALARM_PLAYING = alarmId
                startAlarmNotification(context, alarmId, false)
            } else if (ALARM_PLAYING == alarmId) {
                startAlarmNotification(context, alarmId, true)
            }
        } else {
            if (entity.days > 0) {
                entity.canceledNextAlarms = canceledNextAlarms - 1
                sqLiteDBHelper.addOrUpdateAlarm(entity)
                setUpNextAlarm(entity, context, false)
                AlarmUtils.setUpNextSleepTimeNotification(context)
            }
        }
    }

    private fun startAlarmNotification(context: Context, alarmId: String, sameId: Boolean = false) {
        val startAlarmIntent = Intent(context, AlarmNotifyingService::class.java)
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId)
        startAlarmIntent.putExtra(ALARM_ID_PARAM_SAME_ID, sameId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startAlarmIntent)
        } else {
            context.startService(startAlarmIntent)
        }
    }
}