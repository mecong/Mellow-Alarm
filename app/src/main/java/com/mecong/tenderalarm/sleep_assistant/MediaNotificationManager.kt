package com.mecong.tenderalarm.sleep_assistant

import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.MainActivity

class MediaNotificationManager(private val service: RadioService) {
    private val strAppName: String
    private val resources: Resources = service.resources
    private val notificationManager: NotificationManagerCompat

    fun startNotify(playbackStatus: String, contentText: String?) {
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        var icon = R.drawable.ic_pause_white
        val playbackAction = Intent(service, RadioService::class.java)
        playbackAction.action = RadioService.ACTION_PAUSE
        var action = PendingIntent.getService(service, 1, playbackAction, 0)
        if (playbackStatus == RadioService.PAUSED) {
            icon = R.drawable.ic_play_white
            playbackAction.action = RadioService.ACTION_PLAY
            action = PendingIntent.getService(service, 2, playbackAction, 0)
        }
        val stopIntent = Intent(service, RadioService::class.java)
        stopIntent.action = RadioService.ACTION_STOP
        val stopAction = PendingIntent.getService(service, 3, stopIntent, 0)
        val intent = Intent(service, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.putExtra(MainActivity.FRAGMENT_NAME_PARAM, MainActivity.ASSISTANT_FRAGMENT)
        val pendingIntent = PendingIntent.getActivity(service, 0, intent, 0)
        notificationManager.cancel(NOTIFICATION_ID)
        val builder = NotificationCompat.Builder(service, MainActivity.SLEEP_ASSISTANT_MEDIA_CHANNEL_ID)
                .setAutoCancel(false)
                .setContentTitle(strAppName)
                .setContentText(contentText)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .addAction(icon, "pause", action)
                .addAction(R.drawable.ic_stop_white, "stop", stopAction)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
        //                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
//                        .setMediaSession(service.getMediaSession().getSessionToken())
//                        .setShowActionsInCompactView(0, 1)
//                        .setShowCancelButton(true)
//                        .setCancelButtonIntent(stopAction));
        service.startForeground(NOTIFICATION_ID, builder.build())
        //notificationManager.notify(NOTIFICATION_ID,builder.build());
    }

    fun cancelNotify() {
        service.stopForeground(true)
    }

    companion object {
        private const val NOTIFICATION_ID = 555
    }

    init {
        strAppName = resources.getString(R.string.app_name)
        notificationManager = NotificationManagerCompat.from(service)
    }
}