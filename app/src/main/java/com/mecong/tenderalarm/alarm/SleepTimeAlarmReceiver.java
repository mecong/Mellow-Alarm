package com.mecong.tenderalarm.alarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.TimeZone;

import static com.mecong.tenderalarm.alarm.AlarmUtils.HOUR;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

public class SleepTimeAlarmReceiver extends BroadcastReceiver {

    public static final int RECOMMENDED_SLEEP_TIME = 9;
    private static final int TIME_TO_SLEEP_NOTIFICATION_ID = 1;

    public static void cancelNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(TIME_TO_SLEEP_NOTIFICATION_ID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        HyperLog.initialize(context);
        HyperLog.setLogLevel(Log.INFO);

        HyperLog.i(TAG, "Sleep time job started");

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        AlarmEntity nextActiveAlarm = sqLiteDBHelper.getNextActiveAlarm();

        if (noNextActiveAlarms(nextActiveAlarm)) return;

        if (nextActiveAlarm.getNextNotCanceledTime() - Calendar.getInstance().getTimeInMillis() < 4 * HOUR) {
            AlarmUtils.setUpNextSleepTimeNotification(context);
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            boolean isScreenOn = pm.isInteractive();
            HyperLog.v(TAG, "Device is used: " + isScreenOn);
            if (isScreenOn) {
                Calendar timeToBed = timeToGoToBed(nextActiveAlarm);
                if (alarmInTheMorning(nextActiveAlarm) && timeToBed != null) {
                    showNotification(timeToBed, context);
                }
            }
        }
    }

    private boolean noNextActiveAlarms(AlarmEntity nextActiveAlarm) {
        return nextActiveAlarm == null;
    }

    private void showNotification(Calendar calendar, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(MainActivity.FRAGMENT_NAME_PARAM, MainActivity.ASSISTANT_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String message = context.getString(R.string.time_to_sleep_notification_message, calendar);
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(context, MainActivity.TIME_TO_SLEEP_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(context.getString(R.string.time_to_sleep_notification_title))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(TIME_TO_SLEEP_NOTIFICATION_ID, builder.build());
        Toast.makeText(context, context.getString(R.string.time_to_sleep_notification_title), Toast.LENGTH_SHORT).show();
    }

    private Calendar timeToGoToBed(AlarmEntity nextActiveAlarm) {
        Calendar calendar = Calendar.getInstance();
        long nextAlarmTime = nextActiveAlarm.getNextTime();
        long difference = nextAlarmTime - calendar.getTimeInMillis();
        calendar.setTimeInMillis(difference);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (calendar.get(Calendar.HOUR_OF_DAY) < RECOMMENDED_SLEEP_TIME) {
            return calendar;
        } else {
            return null;
        }
    }

    private boolean alarmInTheMorning(AlarmEntity nextActiveAlarm) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextActiveAlarm.getNextTime());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour > 1 && hour < 11;
    }

}