package com.mecong.myalarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.activity.MainActivity;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import static com.mecong.myalarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.myalarm.AlarmUtils.TAG;

public class UpcomingAlarmNotificationReceiver extends BroadcastReceiver {
    private static final int UPCOMING_ALARM_NOTIFICATION_ID = 2;

    public static void cancelNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(UPCOMING_ALARM_NOTIFICATION_ID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        if ((context.getPackageName() + "CANCEL_ALARM").equals(intent.getAction())) {
            HyperLog.i(TAG, "Canceling alarm: " + entity);
            AlarmUtils.cancelNextAlarm(String.valueOf(entity.getId()), context);
            Toast.makeText(context, context.getString(R.string.upcoming_alarm_canceled_toast,
                    entity.getNextTime()), Toast.LENGTH_LONG).show();
        } else {
            HyperLog.i(TAG, "Before alarm notification: " + entity);
            showNotification(entity, context);
        }
    }

    private void showNotification(AlarmEntity entity, Context context) {
        Intent intent = new Intent(context, this.getClass());
        intent.setAction(context.getPackageName() + "CANCEL_ALARM");
        intent.putExtra(ALARM_ID_PARAM, String.valueOf(entity.getId()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        String message = context.getString(R.string.upcoming_alarm_notification_message, entity.getNextTime());
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(context, MainActivity.BEFORE_ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_info_black_24dp)
                .setContentTitle(context.getString(R.string.upcoming_alarm_notification_title))
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(UPCOMING_ALARM_NOTIFICATION_ID, builder.build());
    }
}
