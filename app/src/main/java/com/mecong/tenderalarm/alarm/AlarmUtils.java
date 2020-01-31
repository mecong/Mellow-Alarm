package com.mecong.tenderalarm.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.SystemClock;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.BuildConfig;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.mecong.tenderalarm.alarm.SleepTimeAlarmReceiver.RECOMMENDED_SLEEP_TIME;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AlarmUtils {
    public static final String TAG = "A.L.A.R.M.A";
    static final String ALARM_ID_PARAM = BuildConfig.APPLICATION_ID + ".alarm_id";
    static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    static final long HOUR = TimeUnit.HOURS.toMillis(1);
    static final long DAY = TimeUnit.DAYS.toMillis(1);

    static void setUpNextAlarm(String alarmId, Context context, boolean manually) {
        AlarmEntity entity = SQLiteDBHelper.getInstance(context).getAlarmById(alarmId);
        setUpNextAlarm(entity, context, manually);
    }

    static void setUpNextAlarm(AlarmEntity alarmEntity, Context context, boolean manually) {
        alarmEntity.updateNextAlarmDate(manually);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent alarmIntent = alarmViaBroadcastReceiver(alarmEntity, context);

        setTheAlarm(alarmEntity.getNextTime(), alarmIntent, alarmMgr);

        SQLiteDBHelper.getInstance(context).addOrUpdateAlarm(alarmEntity);
        HyperLog.i(TAG, "Next alarm with[id=" + alarmEntity.getId() + "] set to: "
                + context.getString(R.string.next_alarm_date_time, alarmEntity.getNextTimeWithTicks()));

        AlarmManager.AlarmClockInfo nextAlarmClock = alarmMgr.getNextAlarmClock();

        if (nextAlarmClock != null) {
            HyperLog.i(TAG, "Next alarm MgrTime: " + nextAlarmClock.getTriggerTime() + " (" +
                    context.getString(R.string.next_alarm_date_time, alarmEntity.getNextTimeWithTicks()) + ") intent: "
                    + nextAlarmClock.getShowIntent());
        }

        setUpNextSleepTimeNotification(context);

        if (alarmEntity.isBeforeAlarmNotification() && alarmEntity.getCanceledNextAlarms() == 0) {
            setupUpcomingAlarmNotification(context, alarmEntity);
        }
    }

    static void snoozeAlarm(int minutes, AlarmEntity alarmEntity, Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent alarmIntent = alarmViaBroadcastReceiver(alarmEntity, context);


        Calendar calendarNow = Calendar.getInstance();
        calendarNow.add(Calendar.MINUTE, minutes);
        setTheAlarm(calendarNow.getTimeInMillis(), alarmIntent, alarmMgr);
    }

    private static PendingIntent alarmViaBroadcastReceiver(AlarmEntity alarmEntity, Context context) {
        Intent intentToFire = new Intent(context, TenderAlarmReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        intentToFire.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context,
                alarmEntity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);
    }

    private static void setTheAlarm(Long time, PendingIntent alarmIntent, AlarmManager alarmMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo =
                    new AlarmManager.AlarmClockInfo(time, alarmIntent);
            alarmMgr.setAlarmClock(alarmClockInfo, alarmIntent);
        } else {
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, time, alarmIntent);
            HyperLog.i(TAG, "setExact");
        }
    }

    private static void setupUpcomingAlarmNotification(Context context, AlarmEntity alarmEntity) {
        if (alarmEntity.getNextTime() - System.currentTimeMillis() < TimeUnit.MINUTES.toMillis(50)) {
            return;
        }

        Intent intentToFire = new Intent(context, UpcomingAlarmNotificationReceiver.class);
        intentToFire.putExtra(ALARM_ID_PARAM, String.valueOf(alarmEntity.getId()));
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context,
                alarmEntity.getNextRequestCode() + 1, intentToFire, FLAG_UPDATE_CURRENT);


        long at = SystemClock.elapsedRealtime()
                + alarmEntity.getNextTime() - HOUR
                - Calendar.getInstance().getTimeInMillis();

        at = Math.max(0, at);

        HyperLog.i(TAG, "Upcoming alarm notification will start in " + (at - SystemClock.elapsedRealtime()) + " ms");

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr != null) {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME, at, alarmIntent);
        }
    }

    static void setUpNextSleepTimeNotification(Context context) {
        AlarmEntity nextMorningAlarm = SQLiteDBHelper.getInstance(context).getNextMorningAlarm();

        Intent intent = new Intent(context, SleepTimeAlarmReceiver.class);
        PendingIntent operation = PendingIntent
                .getBroadcast(context, 22, intent, 0);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) {
            return;
        }

        if (nextMorningAlarm != null) {

            long triggerAfter = SystemClock.elapsedRealtime()
                    + nextMorningAlarm.getNextTime() - RECOMMENDED_SLEEP_TIME * HOUR
                    - Calendar.getInstance().getTimeInMillis();

            triggerAfter = Math.max(0, triggerAfter);
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAfter,
                    INTERVAL_FIFTEEN_MINUTES, operation);
            HyperLog.i(TAG, "Sleep time will start in " +
                    TimeUnit.MILLISECONDS.toMinutes(triggerAfter - SystemClock.elapsedRealtime()) + " min");
        } else {
            alarmMgr.cancel(operation);
            HyperLog.i(TAG, "Sleep time alarm removed");
        }
    }

    static void turnOffAlarm(String id, Context context) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(id);
        turnOffAlarm(entity, context);
    }

    private static void turnOffAlarm(AlarmEntity entity, Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr == null) return;

        Intent intentToFire = new Intent(context, AlarmReceiverActivity.class);
        PendingIntent alarmIntent = PendingIntent.getActivity(context,
                entity.getNextRequestCode(), intentToFire, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);

        intentToFire = new Intent(context, UpcomingAlarmNotificationReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context,
                entity.getNextRequestCode() + 1, intentToFire, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);

        HyperLog.i(TAG, "Next alarm with[id=" + entity.getId() + "] canceled");
    }

    static void resetupAllAlarms(Context context) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        try (Cursor allAlarms = sqLiteDBHelper.getAllAlarms()) {
            while (allAlarms.moveToNext()) {
                AlarmEntity alarmEntity = new AlarmEntity(allAlarms);
                turnOffAlarm(alarmEntity, context);
                setUpNextAlarm(alarmEntity, context, false);
            }
        }

        setUpNextSleepTimeNotification(context);
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
