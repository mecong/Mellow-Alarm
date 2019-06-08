package com.mecong.myalarm.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.mecong.myalarm.alarm.SleepTimeAlarmReceiver.RECOMMENDED_SLEEP_TIME;

public class AlarmUtils {
    public static final String TAG = "A.L.A.R.M.A";
    static final String ALARM_ID_PARAM = "com.mecong.myalarm.alarm_id";
    static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    static final long HOUR = TimeUnit.HOURS.toMillis(1);
    static final long DAY = TimeUnit.DAYS.toMillis(1);

    static void setUpNextAlarm(String alarmId, Context context, boolean manually) {
        AlarmEntity entity = new SQLiteDBHelper(context).getAlarmById(alarmId);
        setUpNextAlarm(entity, context, manually);
    }

    static void setUpNextAlarm(AlarmEntity alarmEntity, Context context, boolean manually) {
        alarmEntity.updateNextAlarmDate(manually);

        Intent intentToFire = new Intent(context, TenderAlarmReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                alarmEntity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);

        HyperLog.v(TAG, "Intent: " + intentToFire);
        HyperLog.v(TAG, "Intent extra: " + intentToFire.getExtras());

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        setTheAlarm(alarmEntity, alarmIntent, alarmMgr);

        new SQLiteDBHelper(context).addAOrUpdateAlarm(alarmEntity);
        HyperLog.i(TAG, "Next alarm with[id=" + alarmEntity.getId() + "] set to: "
                + context.getString(R.string.next_alarm_date, alarmEntity.getNextTime()));

        setUpNextSleepTimeNotification(context);

        if (alarmEntity.isBeforeAlarmNotification() && alarmEntity.getCanceledNextAlarms() == 0) {
            setupUpcomingAlarmNotification(context, alarmEntity);
        }
    }

    private static void setTheAlarm(AlarmEntity alarmEntity, PendingIntent alarmIntent, AlarmManager alarmMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    alarmEntity.getNextTime(), alarmIntent);
        } else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmEntity.getNextTime(), alarmIntent);
        }
    }

    private static void setupUpcomingAlarmNotification(Context context, AlarmEntity alarmEntity) {
        Intent intentToFire = new Intent(context, UpcomingAlarmNotificationReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                alarmEntity.getNextRequestCode() + 1, intentToFire, FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long at = SystemClock.elapsedRealtime()
                + alarmEntity.getNextTime() - HOUR
                - Calendar.getInstance().getTimeInMillis();

        at = Math.max(0, at);

        HyperLog.i(TAG, "Upcoming alarm notification will start in " + at + " ms");

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME, at, alarmIntent);
    }

    static void setUpNextSleepTimeNotification(Context context) {
        AlarmEntity nextMorningAlarm = new SQLiteDBHelper(context).getNextMorningAlarm();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, SleepTimeAlarmReceiver.class);
        PendingIntent operation = PendingIntent
                .getBroadcast(context, 22, intent, 0);
        if (nextMorningAlarm != null) {

            long triggerAfter = SystemClock.elapsedRealtime()
                    + nextMorningAlarm.getNextTime() - RECOMMENDED_SLEEP_TIME * HOUR
                    - Calendar.getInstance().getTimeInMillis();

            triggerAfter = Math.max(0, triggerAfter);
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAfter,
                    INTERVAL_FIFTEEN_MINUTES, operation);
            HyperLog.i(TAG, "Sleep time will start in " + triggerAfter + " ms");
        } else {
            alarmMgr.cancel(operation);
            HyperLog.i(TAG, "Sleep time alarm removed");
        }
    }

    static void resetupAllAlarms(Context context) {
        // TODO: implement
        Toast.makeText(context, "Alarms Was not reset", Toast.LENGTH_LONG).show();

        setUpNextSleepTimeNotification(context);
    }

    static void turnOffAlarm(String id, Context context) {
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(id);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intentToFire = new Intent(context, AlarmReceiverActivity.class);
        PendingIntent alarmIntent = PendingIntent.getActivity(context,
                entity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);

        intentToFire = new Intent(context, UpcomingAlarmNotificationReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context,
                entity.getNextRequestCode() + 1, intentToFire, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);

        HyperLog.i(TAG, "Next alarm with[id=" + id + "] canceled");
    }


    //TODO: set when there are alarms and turn of if no any
    static void setBootReceiverActive(Context context) {
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
