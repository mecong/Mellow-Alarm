package com.mecong.tenderalarm.alarm;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.BuildConfig;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import timber.log.Timber;

import static androidx.core.app.NotificationCompat.FLAG_NO_CLEAR;
import static androidx.core.app.NotificationCompat.FLAG_ONGOING_EVENT;
import static androidx.core.app.NotificationCompat.FLAG_SHOW_LIGHTS;
import static com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlarmNotifyingService extends Service {
    private static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/alarms");
    MediaPlayer alarmMediaPlayer;
    Handler handlerVolume;
    Runnable runnableVolume;
    Random random = new Random();
    Handler handlerTicks;
    Runnable runnableRealAlarm;
    MediaPlayer ticksMediaPlayer;

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String alarmId = intent.getStringExtra(ALARM_ID_PARAM);
        AlarmEntity entity = SQLiteDBHelper.getInstance(this).getAlarmById(alarmId);
        HyperLog.i(TAG, "Running alarm: " + entity);

        usePowerManagerWakeup();
        stopForeground(true);
        startAlarmNotification(this, entity);
        startSound(entity);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stopAlarmNotification();
    }

    private void usePowerManagerWakeup() {
        HyperLog.v(TAG, "Alarm Notifying service usePowerManagerWakeup");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wakeLock =
                    pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
            wakeLock.acquire(TimeUnit.SECONDS.toMillis(10));
        }
    }

    @Subscribe
    public void messageReceived(AlarmMessage message) {
        if (message == AlarmMessage.CANCEL_VOLUME_INCREASE) {
            cancelVolumeIncreasing();
        } else if (message == AlarmMessage.STOP_ALARM) {
            stopAlarmNotification();
        } else if (message == AlarmMessage.SNOOZE2M) {
            snooze(2);
        } else if (message == AlarmMessage.SNOOZE3M) {
            snooze(3);
        } else if (message == AlarmMessage.SNOOZE5M) {
            snooze(5);
        }
    }

    private void snooze(int minutes) {
        HyperLog.d(TAG, "Snooze for " + minutes + " min");
        handlerTicks.removeCallbacksAndMessages(null);
        cancelVolumeIncreasing();
        if (ticksMediaPlayer.isPlaying()) {
            ticksMediaPlayer.pause();
            ticksMediaPlayer.seekTo(0);
        }
        if (alarmMediaPlayer.isPlaying()) {
            alarmMediaPlayer.pause();
            alarmMediaPlayer.seekTo(0);
        }
//        handlerTicks.postDelayed(runnableRealAlarm, minutes * 60 * 1000);
//        handlerVolume.postDelayed(runnableVolume, minutes * 60 * 1000);
    }

    private void cancelVolumeIncreasing() {
        handlerVolume.removeCallbacks(runnableVolume);
    }

    private void startSound(AlarmEntity entity) {

        final AudioAttributes audioAttributesAlarm = new AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0);

        // Create the Handler object (on the main thread by default)
        handlerTicks = new Handler();
        final float[] volume = {0.001f};


        try {
            ticksMediaPlayer = new MediaPlayer();
            ticksMediaPlayer.setAudioAttributes(audioAttributesAlarm);
            ticksMediaPlayer.setDataSource(this, Uri.parse("android.resource://"
                    + getPackageName() + "/" + R.raw.tick));
            ticksMediaPlayer.prepare();
            ticksMediaPlayer.setVolume(volume[0], volume[0]);
        } catch (IOException e) {
            //TODO: make correct reaction
            e.printStackTrace();
        }

        try {
            alarmMediaPlayer = new MediaPlayer();
            alarmMediaPlayer.setAudioAttributes(audioAttributesAlarm);
            final Uri melody = getMelody(this, entity);
            alarmMediaPlayer.setDataSource(this, melody);
            alarmMediaPlayer.prepare();
            alarmMediaPlayer.setLooping(true);
        } catch (Exception ex) {
            Timber.e(ex);
        }

        final Runnable predAlarm = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!ticksMediaPlayer.isPlaying()) {
                        ticksMediaPlayer.start();
                    }
                    HyperLog.v(TAG, "Tick!");
                } catch (Exception e) {
                    HyperLog.e(TAG, "Exception: " + e.getMessage(), e);
                }
                // Repeat this the same runnable code block again another 20 seconds
                // 'this' is referencing the Runnable object
                handlerTicks.postDelayed(this, random.nextInt(20000));
            }
        };


        runnableRealAlarm = new Runnable() {
            @Override
            public void run() {
                try {
                    // Removes pending code execution
                    handlerTicks.removeCallbacks(predAlarm);
                    if (ticksMediaPlayer.isPlaying()) {
                        ticksMediaPlayer.stop();
                    }

                    volume[0] = 0.01f;


                    alarmMediaPlayer.setVolume(volume[0], volume[0]);
                    alarmMediaPlayer.start();

                    handlerVolume.removeCallbacks(runnableVolume);
                    handlerVolume.post(runnableVolume);
                    HyperLog.i(TAG, "Real alarm started!");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        handlerVolume = new Handler();
        runnableVolume = new Runnable() {
            @Override
            public void run() {
                try {
                    volume[0] += 0.001f;
                    Log.v(TAG, "New alarm volume: " + volume[0]);
                    if (alarmMediaPlayer.isPlaying()) {
                        alarmMediaPlayer.setVolume(volume[0], volume[0]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (volume[0] < 1)
                    handlerVolume.postDelayed(this, 2000);
            }
        };

        if (entity.getTicksTime() > 0) {
            handlerTicks.post(predAlarm);
        }

        handlerTicks.postDelayed(runnableRealAlarm, entity.getTicksTime() * 60 * 1000);
    }

    private Uri getMelody(Context context, AlarmEntity entity) {
        if (entity.getMelodyUrl() != null) {
            return Uri.parse(entity.getMelodyUrl());
        } else {
            return Uri.parse(String.format(Locale.ENGLISH, "android.resource://%s/%d",
                    context.getPackageName(), R.raw.long_music));
        }
    }

    private void stopAlarmNotification() {
        HyperLog.i(TAG, "Stop Alarm notification");
        handlerTicks.removeCallbacksAndMessages(null);
        stopTicksAlarm();
        stopAlarmMediaPlayer();
        alarmEndVibration();
        cancelVolumeIncreasing();
        stopForeground(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
        stopSelf();
    }

    private void stopTicksAlarm() {
        if (ticksMediaPlayer.isPlaying()) {
            ticksMediaPlayer.stop();
            ticksMediaPlayer.reset();
            ticksMediaPlayer.release();
        }
    }

    private void stopAlarmMediaPlayer() {
        if (alarmMediaPlayer.isPlaying()) {
            alarmMediaPlayer.stop();
            alarmMediaPlayer.reset();
            alarmMediaPlayer.release();
        }
    }

    private void alarmEndVibration() {
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(1000);
            }
        }
    }


    private void startAlarmNotification(Context context, AlarmEntity entity) {
        String alarmId = String.valueOf(entity.getId());
        Intent startAlarmIntent = new Intent(context, AlarmReceiverActivity.class)
                .setData(ContentUris.withAppendedId(CONTENT_URI, Long.parseLong(alarmId)));
        startAlarmIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 42,
                startAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder alarmNotification = new NotificationCompat
                .Builder(context, MainActivity.ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle("Alarm")
                .setContentText(entity.getMessage())
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(0)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(false)
                .setDefaults(FLAG_SHOW_LIGHTS | FLAG_ONGOING_EVENT | FLAG_NO_CLEAR)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();

        startForeground(42, alarmNotification.build());
    }
}
