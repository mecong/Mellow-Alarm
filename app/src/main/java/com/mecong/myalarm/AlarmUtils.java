package com.mecong.myalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.activity.AlarmReceiver;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.util.Calendar;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.mecong.myalarm.SleepTimeAlarmReceiver.RECOMMENDED_SLEEP_TIME;

public class AlarmUtils {
    public static final String TAG = "A.L.A.R.M.A";
    public static final String ALARM_ID_PARAM = "com.mecong.myalarm.alarm_id";
    public static final int MINUTE = 60 * 1000;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;

    public static void setUpNextAlarm(String alarmId, Context context, boolean manually) {
        AlarmEntity entity = new SQLiteDBHelper(context).getAlarmById(alarmId);
        setUpNextAlarm(entity, context, manually);
    }

    public static void setUpNextAlarm(AlarmEntity alarmEntity, Context context, boolean manually) {
        Calendar alarmCalendar = alarmEntity.getNextAlarmDate(manually);
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        if (alarmCalendar != null) {
            Intent intentToFire = new Intent(context, AlarmReceiver.class);
            intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
            HyperLog.v(TAG, "Intent: " + intentToFire);
            HyperLog.v(TAG, "Intent extra: " + intentToFire.getExtras());
            int requestCode = getRequestCode(alarmEntity);
            PendingIntent alarmIntent = PendingIntent.getActivity(context,
                    requestCode, intentToFire, FLAG_UPDATE_CURRENT);

            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            HyperLog.i(TAG, "Next alarm with[id=" + alarmEntity.getId() + "] set to: "
                    + context.getString(R.string.next_alarm_date, alarmCalendar.getTime()));
            long nextAlarmTime = alarmCalendar.getTimeInMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        nextAlarmTime, alarmIntent);
            } else {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime, alarmIntent);
            }

            sqLiteDBHelper
                    .updateNextAlarmTimeAndCode(alarmEntity.getId(), nextAlarmTime, requestCode);
        } else {
            sqLiteDBHelper.toggleAlarmActive(String.valueOf(alarmEntity.getId()), false);
        }

        setUpSleepTimeAlarm(context);
    }

    public static void setUpSleepTimeAlarm(Context context) {
        AlarmEntity nextMorningAlarm = new SQLiteDBHelper(context).getNextMorningAlarm();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, SleepTimeAlarmReceiver.class);
        PendingIntent operation = PendingIntent
                .getBroadcast(context, 1, intent, 0);
        if (nextMorningAlarm != null) {
            Calendar now = Calendar.getInstance();
            long triggerAt = nextMorningAlarm.getNextTime() - RECOMMENDED_SLEEP_TIME * HOUR - now.getTimeInMillis();
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAt,
                    INTERVAL_FIFTEEN_MINUTES, operation);
            HyperLog.i(TAG, "Sleep time alarm set to " + triggerAt + " ms");
        } else {
            alarmMgr.cancel(operation);
            HyperLog.i(TAG, "Sleep time alarm removed");
        }
    }

    private static int getRequestCode(AlarmEntity alarmEntity) {
        return (int) alarmEntity.getId() * 100000;
    }

    static void resetupAllAlarms(Context context) {
        // TODO: implement
        Toast.makeText(context, "Alarms Was not reset", Toast.LENGTH_LONG).show();

        setUpSleepTimeAlarm(context);
    }

    public static void cancelNextAlarms(String id, Context context) {
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(id);

        Intent intentToFire = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getActivity(context,
                entity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmMgr.cancel(alarmIntent);

        HyperLog.i(TAG, "Next alarm with[id=" + id + "] canceled");

        setUpSleepTimeAlarm(context);
    }


    //TODO: set when there are alarms and turn of if no any
    public static void setBootReceiverActive(Context context) {
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}
