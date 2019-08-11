package com.mecong.myalarm.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import static com.mecong.myalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.myalarm.alarm.AlarmUtils.TAG;

public class TenderAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        HyperLog.initialize(context);
        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        SleepTimeAlarmReceiver.cancelNotification(context);
        UpcomingAlarmNotificationReceiver.cancelNotification(context);

        Integer canceledNextAlarms = entity.getCanceledNextAlarms();
        HyperLog.i(TAG, "Received alarm: " + entity);
        if (canceledNextAlarms == 0) {

            if (entity.getDays() > 0) {
                AlarmUtils.setUpNextAlarm(entity, context, false);
            } else {
                sqLiteDBHelper.toggleAlarmActive(alarmId, false);
            }

            Intent startAlarmIntent = new Intent(context, AlarmReceiverActivity.class);
            startAlarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startAlarmIntent.putExtra(ALARM_ID_PARAM, String.valueOf(entity.getId()));
            HyperLog.i(TAG, "Starting alarm activity: " + startAlarmIntent);
            context.startActivity(startAlarmIntent);
        } else {
            if (entity.getDays() > 0) {
                entity.setCanceledNextAlarms(canceledNextAlarms - 1);
                sqLiteDBHelper.addAOrUpdateAlarm(entity);
                AlarmUtils.setUpNextAlarm(entity, context, false);
            }
        }
    }

}
