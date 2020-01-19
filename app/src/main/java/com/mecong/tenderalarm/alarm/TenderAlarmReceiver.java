package com.mecong.tenderalarm.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import static com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

public class TenderAlarmReceiver extends BroadcastReceiver {
    //adb shell dumpsys alarm > alarms.dump

    @Override
    public void onReceive(Context context, Intent intent) {
        HyperLog.initialize(context);
        HyperLog.setLogLevel(Log.VERBOSE);
        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);


        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        SleepTimeAlarmReceiver.cancelNotification(context);
        UpcomingAlarmNotificationReceiver.cancelNotification(context);

        Integer canceledNextAlarms = entity.getCanceledNextAlarms();
        HyperLog.i(TAG, "TenderAlarmReceiver received alarm: " + entity);
        if (canceledNextAlarms == 0) {

            if (entity.getDays() > 0) {
                AlarmUtils.setUpNextAlarm(entity, context, false);
            } else {
                sqLiteDBHelper.toggleAlarmActive(alarmId, false);
            }
            startAlarmNotification(context, alarmId);
        } else {
            if (entity.getDays() > 0) {
                entity.setCanceledNextAlarms(canceledNextAlarms - 1);
                sqLiteDBHelper.addOrUpdateAlarm(entity);
                AlarmUtils.setUpNextAlarm(entity, context, false);
            }
        }
    }

    private void startAlarmNotification(Context context, String alarmId) {
        Intent startAlarmIntent = new Intent(context, AlarmNotifyingService.class);
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startAlarmIntent);
        } else {
            context.startService(startAlarmIntent);
        }
    }
}
