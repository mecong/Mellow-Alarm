package com.mecong.myalarm.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(AlarmUtils.TAG, "DeviceBootReceiver");
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            AlarmUtils.resetupAllAlarms(context);
        }
    }
}