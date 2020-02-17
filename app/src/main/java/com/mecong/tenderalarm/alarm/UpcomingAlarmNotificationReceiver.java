package com.mecong.tenderalarm.alarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import static com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.tenderalarm.alarm.AlarmUtils.MINUTE;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

public class UpcomingAlarmNotificationReceiver extends BroadcastReceiver {
    private static final int UPCOMING_ALARM_NOTIFICATION_ID = 2;
    private String actionCancelAlarm = UpcomingAlarmNotificationReceiver.class.getCanonicalName() + "--CANCEL_ALARM";


    public static void cancelNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(UPCOMING_ALARM_NOTIFICATION_ID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();

        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        if ((actionCancelAlarm).equals(intent.getAction())) {
            HyperLog.i(TAG, "Canceling alarm: " + entity);
            entity.setCanceledNextAlarms(1);
            sqLiteDBHelper.addOrUpdateAlarm(entity);
            AlarmUtils.setUpNextAlarm(entity, context, false);
            Toast.makeText(context, context.getString(R.string.upcoming_alarm_canceled_toast,
                    entity.getNextTimeWithTicks()), Toast.LENGTH_LONG).show();
        } else {
            if (entity.getCanceledNextAlarms() == 0) {
                HyperLog.i(TAG, "Before alarm notification: " + entity);
                showNotification(entity, context);
            }
        }
    }

    private void showNotification(AlarmEntity entity, Context context) {
        Intent intent = new Intent(context, this.getClass());
        intent.setAction(actionCancelAlarm);
        intent.putExtra(ALARM_ID_PARAM, String.valueOf(entity.getId()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, entity.getNextRequestCode() + 1, intent, 0);

        String message = context.getString(R.string.upcoming_alarm_notification_message,
                entity.getNextTime() + entity.getTicksTime() * MINUTE);
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(context, MainActivity.BEFORE_ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(context.getString(R.string.upcoming_alarm_notification_title))
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(UPCOMING_ALARM_NOTIFICATION_ID, builder.build());
    }
}