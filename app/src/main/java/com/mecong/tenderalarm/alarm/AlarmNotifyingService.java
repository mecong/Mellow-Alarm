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
import java.util.concurrent.TimeUnit;

import static com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

public class AlarmNotifyingService extends Service {
    private static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/alarms");
    private MediaPlayer alarmMediaPlayer;
    private Handler handlerVolume;
    private Runnable runnableVolume;

    public AlarmNotifyingService() {
    }

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
        AlarmEntity entity = SQLiteDBHelper.getInstance(getBaseContext()).getAlarmById(alarmId);
        HyperLog.i(TAG, "Running alarm: " + entity);

        usePowerManagerWakeup();
        startAlarmNotification(getBaseContext(), entity);
        startSound(entity);
        return START_NOT_STICKY;
    }

    private void usePowerManagerWakeup() {
        HyperLog.i(TAG, "usePowerManagerWakeup");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
        wakeLock.acquire(TimeUnit.SECONDS.toMillis(10));
    }

    @Subscribe
    public void onPlayFileChanged(AlarmMessage message) {
        if (message == AlarmMessage.CANCEL_VOLUME_INCREASE) {
            cancelVolumeIncreasing();
        } else if (message == AlarmMessage.STOP_ALARM) {
            turnOffAlarm();
        }
    }

    private void cancelVolumeIncreasing() {
        handlerVolume.removeCallbacks(runnableVolume);
    }

    private void startSound(AlarmEntity entity) {
        final Context context = getBaseContext();

        final AudioAttributes audioAttributesAlarm = new AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0);

        // Create the Handler object (on the main thread by default)
        final Handler handlerTicks = new Handler();
        final float[] volume = {0.4f};

        final MediaPlayer ticksMediaPlayer = new MediaPlayer();
        try {
            ticksMediaPlayer.setAudioAttributes(audioAttributesAlarm);
            ticksMediaPlayer.setDataSource(context, Uri.parse("android.resource://"
                    + context.getPackageName() + "/" + R.raw.metal_knock));
            ticksMediaPlayer.prepare();
            ticksMediaPlayer.setVolume(volume[0], volume[0]);
        } catch (IOException e) {
            //TODO: make correct reaction
            e.printStackTrace();
        }

        alarmMediaPlayer = new MediaPlayer();


        final Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                try {
                    ticksMediaPlayer.start();
                    HyperLog.d(TAG, "Tick!");
                } catch (Exception e) {
                    HyperLog.e(TAG, "Exception: " + e.getMessage(), e);
                }
                // Repeat this the same runnable code block again another 20 seconds
                // 'this' is referencing the Runnable object
                handlerTicks.postDelayed(this, 20000);
            }
        };


        Runnable runnableRealAlarm = new Runnable() {
            @Override
            public void run() {
                try {
                    // Removes pending code execution
                    handlerTicks.removeCallbacks(runnableCode);
                    ticksMediaPlayer.stop();
                    ticksMediaPlayer.reset();
                    ticksMediaPlayer.release();

                    volume[0] = 0.01f;

                    alarmMediaPlayer.setAudioAttributes(audioAttributesAlarm);
                    alarmMediaPlayer.setDataSource(context, Uri.parse("android.resource://"
                            + context.getPackageName() + "/" + R.raw.long_music));
                    alarmMediaPlayer.prepare();
                    alarmMediaPlayer.setLooping(true);
                    alarmMediaPlayer.setVolume(volume[0], volume[0]);
                    alarmMediaPlayer.start();
                    HyperLog.d(TAG, "Real alarm started!");

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
                    alarmMediaPlayer.setVolume(volume[0], volume[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (volume[0] < 1)
                    handlerVolume.postDelayed(this, 2000);
            }
        };

        if (entity.getTicksTime() > 0) {
            handlerTicks.post(runnableCode);
        }

        handlerTicks.postDelayed(runnableRealAlarm, entity.getTicksTime() * 60 * 1000);
        handlerVolume.post(runnableVolume);
    }

    private void turnOffAlarm() {

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
        Vibrator vibrator = (Vibrator) getApplicationContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(1000);
            }
        }
        if (alarmMediaPlayer.isPlaying()) {
            alarmMediaPlayer.stop();
            alarmMediaPlayer.reset();
            alarmMediaPlayer.release();
        }
        stopForeground(true);
        stopSelf();
        System.exit(0);
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
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();

        startForeground(42, alarmNotification.build());
    }

}
