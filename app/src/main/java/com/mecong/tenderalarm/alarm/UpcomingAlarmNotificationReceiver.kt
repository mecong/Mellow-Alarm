package com.mecong.tenderalarm.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper

class UpcomingAlarmNotificationReceiver : BroadcastReceiver() {
    private val actionCancelAlarm = UpcomingAlarmNotificationReceiver::class.java.canonicalName + "--CANCEL_ALARM"

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
        val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
        val sqLiteDBHelper = sqLiteDBHelper(context)
        val entity = sqLiteDBHelper!!.getAlarmById(alarmId)
        if (actionCancelAlarm == intent.action) {
            ////HyperLog.i(TAG, "Canceling alarm: $entity")
            entity!!.canceledNextAlarms = 1
            sqLiteDBHelper.addOrUpdateAlarm(entity)
            setUpNextAlarm(entity, context, false)
            Toast.makeText(context, context.getString(R.string.upcoming_alarm_canceled_toast,
                    entity.nextTimeWithTicks), Toast.LENGTH_LONG).show()
        } else {
            if (entity!!.canceledNextAlarms == 0) {
                ////HyperLog.i(TAG, "Before alarm notification: $entity")
                showNotification(entity, context)
            }
        }
    }

    private fun showNotification(entity: AlarmEntity?, context: Context) {
        val intent = Intent(context, this.javaClass)
        intent.action = actionCancelAlarm
        intent.putExtra(ALARM_ID_PARAM, entity!!.id.toString())
        val pendingIntent = PendingIntent.getBroadcast(context, entity.nextRequestCode + 1, intent, 0)
        val message = context.getString(R.string.upcoming_alarm_notification_message,
                entity.nextTime + entity.ticksTime * MINUTE)
        val builder = NotificationCompat.Builder(context, MainActivity.BEFORE_ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.cat_purr)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.cat_purr))
                .setContentTitle(context.getString(R.string.upcoming_alarm_notification_title))
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(UPCOMING_ALARM_NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val UPCOMING_ALARM_NOTIFICATION_ID = 2
        fun cancelNotification(context: Context?) {
            val notificationManager = NotificationManagerCompat.from(context!!)
            notificationManager.cancel(UPCOMING_ALARM_NOTIFICATION_ID)
        }
    }
}