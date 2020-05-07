package com.mecong.tenderalarm.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import java.util.*

class SleepTimeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
//        HyperLog.initialize(context)
//        HyperLog.setLogLevel(Log.ERROR)
        //HyperLog.i(TAG, "Sleep time job started")
        val sqLiteDBHelper = sqLiteDBHelper(context)
        val nextActiveAlarm = sqLiteDBHelper!!.nextActiveAlarm

        if (noNextActiveAlarms(nextActiveAlarm)) return

        if (nextActiveAlarm!!.nextNotCanceledTime - Calendar.getInstance().timeInMillis < 4 * HOUR) {
            setUpNextSleepTimeNotification(context)
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive
        //HyperLog.v(TAG, "Device is used: $isScreenOn")
        if (isScreenOn) {
            val timeToBed = timeToGoToBed(nextActiveAlarm)
            if (timeToBed != null && alarmInTheMorning(nextActiveAlarm)) {
                showNotification(timeToBed, context)
            }
        }
    }

    private fun noNextActiveAlarms(nextActiveAlarm: AlarmEntity?): Boolean {
        return nextActiveAlarm == null
    }

    private fun showNotification(calendar: Calendar, context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(MainActivity.FRAGMENT_NAME_PARAM, MainActivity.ASSISTANT_FRAGMENT)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val hours = calendar[Calendar.HOUR_OF_DAY]
        val minutes = calendar[Calendar.MINUTE]
        val hoursText = context.resources.getQuantityString(R.plurals.n_hours_plural, hours, hours)
        val minutesText = context.resources.getQuantityString(R.plurals.n_minutes_plural, minutes, minutes)
        val message = context.getString(R.string.time_to_sleep_notification_message, hoursText, minutesText)

        val builder = NotificationCompat.Builder(context, MainActivity.TIME_TO_SLEEP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cat_sleep)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_cat_sleep))
                .setContentTitle(context.getString(R.string.time_to_sleep_notification_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(TIME_TO_SLEEP_NOTIFICATION_ID, builder.build())
        Toast.makeText(context, context.getString(R.string.time_to_sleep_notification_title), Toast.LENGTH_SHORT).show()
    }

    private fun timeToGoToBed(nextActiveAlarm: AlarmEntity?): Calendar? {
        val calendar = Calendar.getInstance()
        val nextAlarmTime = nextActiveAlarm!!.nextTimeWithTicks
        val difference = nextAlarmTime - calendar.timeInMillis
        calendar.timeInMillis = difference
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        return if (calendar[Calendar.HOUR_OF_DAY] < RECOMMENDED_SLEEP_TIME) {
            calendar
        } else {
            null
        }
    }

    private fun alarmInTheMorning(nextActiveAlarm: AlarmEntity?): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nextActiveAlarm!!.nextTime
        val hour = calendar[Calendar.HOUR_OF_DAY]
        return hour in 0..10
    }

    companion object {
        const val RECOMMENDED_SLEEP_TIME = 9
        private const val TIME_TO_SLEEP_NOTIFICATION_ID = 1
        fun cancelNotification(context: Context?) {
            val notificationManager = NotificationManagerCompat.from(context!!)
            notificationManager.cancel(TIME_TO_SLEEP_NOTIFICATION_ID)
        }
    }
}