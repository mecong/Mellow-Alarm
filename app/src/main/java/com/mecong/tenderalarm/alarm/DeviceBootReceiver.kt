package com.mecong.tenderalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mecong.tenderalarm.alarm.AlarmUtils.resetupAllAlarms

class DeviceBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    //HyperLog.i(AlarmUtils.TAG, "DeviceBootReceiver")
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.action, ignoreCase = true)) {
      resetupAllAlarms(context)
    }
  }
}