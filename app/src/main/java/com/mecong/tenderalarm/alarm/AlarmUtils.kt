package com.mecong.tenderalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

val MINUTE = TimeUnit.MINUTES.toMillis(1)
val HOUR = TimeUnit.HOURS.toMillis(1)
val DAY = TimeUnit.DAYS.toMillis(1)

object AlarmUtils {
  const val ALARM_ID_PARAM = BuildConfig.APPLICATION_ID + ".alarm_id"
  const val ALARM_ID_PARAM_SAME_ID = BuildConfig.APPLICATION_ID + ".alarm_id.same_id"

  fun setUpNextAlarm(alarmId: String, context: Context, manually: Boolean) {
    val entity = sqLiteDBHelper(context)!!.getAlarmById(alarmId)
    setUpNextAlarm(entity, context, manually)
  }

  fun setUpNextAlarm(alarmEntity: AlarmEntity?, context: Context, manually: Boolean) {
    alarmEntity!!.updateNextAlarmDate(manually)
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val alarmIntent = primaryAlarmPendingIntent(alarmEntity, context)
    setTheAlarm(context, alarmEntity.nextTime, alarmIntent, alarmMgr)
    sqLiteDBHelper(context)!!.addOrUpdateAlarm(alarmEntity)
    Timber.i(
      "%s %s",
      """Next alarm with[id=${alarmEntity.id}] set to:""",
      context.getString(R.string.next_alarm_date_time, alarmEntity.nextTimeWithTicks)
    )

    setupUpcomingAlarmNotification(context, alarmEntity)
  }

  fun snoozeAlarmNotification(minutes: Int, alarmEntity: AlarmEntity?, context: Context) {
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intentToFire = Intent(context, MellowAlarmReceiver::class.java)
    intentToFire.putExtra(ALARM_ID_PARAM, alarmEntity!!.id.toString())
    intentToFire.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    val alarmIntent = PendingIntent.getBroadcast(
      context,
      alarmEntity.snoozedAlarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )

    val calendarSnoozeEnd = Calendar.getInstance()
    calendarSnoozeEnd.add(Calendar.MINUTE, minutes)

    setTheAlarm(context, calendarSnoozeEnd.timeInMillis, alarmIntent, alarmMgr)
  }

  private fun primaryAlarmPendingIntent(alarmEntity: AlarmEntity?, context: Context): PendingIntent {
    val intentToFire = Intent(context, MellowAlarmReceiver::class.java)
    intentToFire.putExtra(ALARM_ID_PARAM, alarmEntity!!.id.toString())
    intentToFire.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    return PendingIntent.getBroadcast(
      context,
      alarmEntity.alarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )
  }

  private fun setTheAlarm(context: Context, time: Long, alarmIntent: PendingIntent, alarmManager: AlarmManager) {
//    val notificationPermission = ContextCompat.checkSelfPermission(
//      context,
//      ACTION_APP_NOTIFICATION_SETTINGS
//    )
//
//    val fullScreenPermission = ContextCompat.checkSelfPermission(
//      context,
//      Manifest.permission.SYSTEM_ALERT_WINDOW
//    )
//
//    val alarmPermission = ContextCompat.checkSelfPermission(
//      context,
//      Manifest.permission.SET_ALARM
//    )
//
//
//    ActivityCompat.requestPermissions(
//      context,
//      arrayOf(
//        Manifest.permission.NOTIFICATION_POLICY_ACCESS,
//        Manifest.permission.SYSTEM_ALERT_WINDOW,
//        Manifest.permission.SET_ALARM
//      ),
//      requestCode
//    )


    val notificationManager = NotificationManagerCompat.from(context)

    if (!notificationManager.areNotificationsEnabled()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        startActivity(context, Intent().apply {
          action = ACTION_APP_NOTIFICATION_SETTINGS
          putExtra(EXTRA_APP_PACKAGE, "com.mecong.tenderalarm")
        }, Bundle.EMPTY)
      }
    } else {
      // Permission already granted, proceed with notification creation
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      if (!notificationManager.canUseFullScreenIntent()) {
        // Ask users to go to exact alarm page in system settings.
        startActivity(context, Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT), Bundle.EMPTY)
        return
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      when {
        // If permission is granted, proceed with scheduling exact alarms.
        alarmManager.canScheduleExactAlarms() -> {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, alarmIntent)
          Timber.v("set Exact alarm:$time")
        }

        else -> {
          // Ask users to go to exact alarm page in system settings.
          startActivity(context, Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM), Bundle.EMPTY)
        }
      }
    }
  }

  private fun setupUpcomingAlarmNotification(context: Context, alarmEntity: AlarmEntity?) {
    if (alarmIsTooSoon(alarmEntity) || !alarmEntity!!.isHeadsUp || alarmEntity.canceledNextAlarms != 0) {
      return
    }

    val intentToFire = Intent(context, UpcomingAlarmNotificationReceiver::class.java)
    intentToFire.putExtra(ALARM_ID_PARAM, alarmEntity.id.toString())
    val alarmIntent = PendingIntent.getBroadcast(
      context,
      alarmEntity.upcomingAlarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )

    var at =
      (SystemClock.elapsedRealtime() + alarmEntity.nextTimeWithTicks) - HOUR - Calendar.getInstance().timeInMillis
    at = max(0, at)

    Timber.i("""Upcoming alarm notification will start in ${at - SystemClock.elapsedRealtime()} ms""")
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmMgr.set(AlarmManager.ELAPSED_REALTIME, at, alarmIntent)
  }

  private fun alarmIsTooSoon(alarmEntity: AlarmEntity?): Boolean {
    return alarmEntity!!.nextTime - System.currentTimeMillis() < TimeUnit.MINUTES.toMillis(50)
  }

  fun setUpNextSleepTimeNotification(context: Context) {
    val nextMorningAlarm = sqLiteDBHelper(context)!!.nextMorningAlarm
    val intent = Intent(context, SleepTimeAlarmReceiver::class.java)
    val operation = PendingIntent.getBroadcast(context, 22, intent, FLAG_IMMUTABLE)
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmMgr.cancel(operation)
    if (nextMorningAlarm != null) {
      var triggerAfter = (SystemClock.elapsedRealtime()
          + nextMorningAlarm.nextNotCanceledTime) - SleepTimeAlarmReceiver.RECOMMENDED_SLEEP_TIME * HOUR - Calendar.getInstance().timeInMillis

      triggerAfter = max(0, triggerAfter)
      alarmMgr.setInexactRepeating(
        AlarmManager.ELAPSED_REALTIME, triggerAfter,
        AlarmManager.INTERVAL_FIFTEEN_MINUTES, operation
      )
      Timber.d(
        "%s min",
        """Sleep time will start in ${TimeUnit.MILLISECONDS.toMinutes(triggerAfter - SystemClock.elapsedRealtime())}"""
      )
    }
  }

  fun turnOffAlarm(id: String?, context: Context) {
    val sqLiteDBHelper = sqLiteDBHelper(context)
    val entity = sqLiteDBHelper!!.getAlarmById(id)!!
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    Timber.i("Turning off alarm: %s", entity)

    turnOffPrimaryAlarm(entity, alarmMgr, context)
    turnOffUpcomingNotificationAlarm(entity, alarmMgr, context)
    turnOffSnoozeAlarm(entity, alarmMgr, context)
  }

  private fun turnOffPrimaryAlarm(entity: AlarmEntity, alarmMgr: AlarmManager, context: Context) {
    val alarmIntent = primaryAlarmPendingIntent(entity, context)

    alarmMgr.cancel(alarmIntent)
  }

  private fun turnOffUpcomingNotificationAlarm(entity: AlarmEntity, alarmMgr: AlarmManager, context: Context) {
    val intentToFire = Intent(context, UpcomingAlarmNotificationReceiver::class.java)
    val alarmIntent = PendingIntent.getBroadcast(
      context,
      entity.upcomingAlarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )

    alarmMgr.cancel(alarmIntent)
  }

  fun turnOffSnoozeAlarm(entity: AlarmEntity, alarmMgr: AlarmManager, context: Context) {
    val intentToFire = Intent(context, MellowAlarmReceiver::class.java)
    intentToFire.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    val alarmIntent = PendingIntent.getBroadcast(
      context,
      entity.snoozedAlarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )

    alarmMgr.cancel(alarmIntent)
  }

  /**
   * @deprecated
   */
  private fun turnOffAlarm(entity: AlarmEntity?, context: Context) {
    val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var alarmIntent = primaryAlarmPendingIntent(entity, context)
    alarmMgr.cancel(alarmIntent)


    val intentToFire = Intent(context, UpcomingAlarmNotificationReceiver::class.java)
    alarmIntent = PendingIntent.getBroadcast(
      context,
      entity!!.snoozedAlarmRequestCode, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE
    )
    alarmMgr.cancel(alarmIntent)
    Timber.i("""Next alarm with[id=${entity.id}] canceled""")
  }

  @JvmStatic
  fun resetupAllAlarms(context: Context) {
    val sqLiteDBHelper = sqLiteDBHelper(context)
    sqLiteDBHelper!!.allAlarms.use { allAlarms ->
      while (allAlarms.moveToNext()) {
        val alarmEntity = AlarmEntity(allAlarms)
        if (alarmEntity.isActive) {
          setUpNextAlarm(alarmEntity, context, false)
        }
      }
    }
  }

  //TODO: set when there are alarms and turn of if no any
  @JvmStatic
  fun setBootReceiverActive(context: Context) {
    val receiver = ComponentName(context, DeviceBootReceiver::class.java)
    val pm = context.packageManager
    pm.setComponentEnabledSetting(
      receiver,
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      PackageManager.DONT_KILL_APP
    )
  }
}