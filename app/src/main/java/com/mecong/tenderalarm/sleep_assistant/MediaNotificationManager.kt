package com.mecong.tenderalarm.sleep_assistant

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.MainActivity

class MediaNotificationManager(private val service: RadioService) {
  private val resources: Resources = service.resources

  fun startNotify(playbackStatus: RadioServiceStatus, contentText: String?) {
    val playbackAction = Intent(service, RadioService::class.java)
    playbackAction.action = RadioService.ACTION_PAUSE

    var icon = R.drawable.ic_pause_white
    var pauseAction = PendingIntent.getService(service, 1, playbackAction, FLAG_IMMUTABLE)
    if (playbackStatus == RadioServiceStatus.PAUSED) {
      icon = R.drawable.ic_play_white
      playbackAction.action = RadioService.ACTION_PLAY
      pauseAction = PendingIntent.getService(service, 2, playbackAction, FLAG_IMMUTABLE)
    }

    val stopIntent = Intent(service, RadioService::class.java)
    stopIntent.action = RadioService.ACTION_STOP
    val stopAction = PendingIntent.getService(service, 3, stopIntent, FLAG_IMMUTABLE)

    val contentIntent = Intent(service, MainActivity::class.java)
    contentIntent.action = Intent.ACTION_MAIN
    contentIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    contentIntent.putExtra(MainActivity.FRAGMENT_NAME_PARAM, MainActivity.ASSISTANT_FRAGMENT)

    val pendingIntent = PendingIntent.getActivity(service, 0, contentIntent, FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(service, MainActivity.SLEEP_ASSISTANT_MEDIA_CHANNEL_ID)
      .setAutoCancel(false)
      .setContentTitle(service.getString(R.string.sleep_assistant))
      .setContentText(contentText)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.cat_purr))
      .setSmallIcon(R.drawable.sleep_active)
      .setContentIntent(pendingIntent)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, service.getString(R.string.stop), stopAction)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setWhen(System.currentTimeMillis())

    service.startForeground(NOTIFICATION_ID, builder.build())
  }

  fun cancelNotify() {
    service.stopForeground(true)
  }

  companion object {
    private const val NOTIFICATION_ID = 555
  }

}