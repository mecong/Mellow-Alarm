package com.mecong.myalarm.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import static com.mecong.myalarm.alarm.AlarmUtils.ALARM_ID_PARAM;

public class TenderAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);
        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        SleepTimeAlarmReceiver.cancelNotification(context);
        UpcomingAlarmNotificationReceiver.cancelNotification(context);

        Integer canceledNextAlarms = entity.getCanceledNextAlarms();
        if (canceledNextAlarms == 0) {

            if (entity.getDays() > 0) {
                AlarmUtils.setUpNextAlarm(entity, context, false);
            } else {
                sqLiteDBHelper.toggleAlarmActive(alarmId, false);
            }

            Intent startAlarmIntent = new Intent(context, AlarmReceiverActivity.class);
            startAlarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startAlarmIntent.putExtra(ALARM_ID_PARAM, String.valueOf(entity.getId()));
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
