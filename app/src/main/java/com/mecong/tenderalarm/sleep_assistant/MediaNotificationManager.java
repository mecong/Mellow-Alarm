package com.mecong.tenderalarm.sleep_assistant;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.alarm.MainActivity;

import static com.mecong.tenderalarm.sleep_assistant.RadioService.PAUSED;


class MediaNotificationManager {

    private static final int NOTIFICATION_ID = 555;
    private RadioService service;
    private String strAppName;
    private Resources resources;
    private NotificationManagerCompat notificationManager;

    MediaNotificationManager(RadioService service) {
        this.service = service;
        this.resources = service.getResources();
        strAppName = resources.getString(R.string.app_name);
        notificationManager = NotificationManagerCompat.from(service);
    }

    void startNotify(String playbackStatus, String contentText) {

        Bitmap largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher);

        int icon = R.drawable.ic_pause_white;
        Intent playbackAction = new Intent(service, RadioService.class);
        playbackAction.setAction(RadioService.ACTION_PAUSE);
        PendingIntent action = PendingIntent.getService(service, 1, playbackAction, 0);

        if (playbackStatus.equals(PAUSED)) {
            icon = R.drawable.ic_play_white;
            playbackAction.setAction(RadioService.ACTION_PLAY);
            action = PendingIntent.getService(service, 2, playbackAction, 0);
        }

        Intent stopIntent = new Intent(service, RadioService.class);
        stopIntent.setAction(RadioService.ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(service, 3, stopIntent, 0);

        Intent intent = new Intent(service, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(MainActivity.FRAGMENT_NAME_PARAM, MainActivity.ASSISTANT_FRAGMENT);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, 0);

        notificationManager.cancel(NOTIFICATION_ID);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, MainActivity.SLEEP_ASSISTANT_MEDIA_CHANNEL_ID)
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
                .setWhen(System.currentTimeMillis());
//                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
//                        .setMediaSession(service.getMediaSession().getSessionToken())
//                        .setShowActionsInCompactView(0, 1)
//                        .setShowCancelButton(true)
//                        .setCancelButtonIntent(stopAction));
        service.startForeground(NOTIFICATION_ID, builder.build());
//notificationManager.notify(NOTIFICATION_ID,builder.build());
    }

    void cancelNotify() {
        service.stopForeground(true);
    }

}
