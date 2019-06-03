package com.mecong.myalarm.activity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.AlarmUtils;
import com.mecong.myalarm.R;
import com.mecong.myalarm.SleepTimeAlarmReceiver;
import com.mecong.myalarm.UpcomingAlarmNotificationReceiver;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.mecong.myalarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.myalarm.AlarmUtils.TAG;
import static com.mecong.myalarm.AlarmUtils.setUpSleepTimeAlarm;

public class AlarmReceiver extends AppCompatActivity {

    public void turnScreenOnThroughKeyguard() {
        userPowerManagerWakeup();
        useWindowFlags();
        useActivityScreenMethods();
    }

    private void useActivityScreenMethods() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                this.setTurnScreenOn(true);
                this.setShowWhenLocked(true);
            } catch (NoSuchMethodError e) {
                HyperLog.e(TAG, "Enable setTurnScreenOn and setShowWhenLocked is not present on device!", e);
            }
        }
    }

    private void useWindowFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    private void userPowerManagerWakeup() {
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, this.getLocalClassName());
        wakeLock.acquire(TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.VERBOSE);

        String alarmId = getIntent().getStringExtra(ALARM_ID_PARAM);
        HyperLog.i(TAG, "Running alarm with extras: " + getIntent().getExtras());
        HyperLog.i(TAG, "Running alarm with id: " + alarmId);
        final Context context = getApplicationContext();

        if (alarmId == null) {
            HyperLog.e(TAG, "Alarm id is null");
            System.exit(0);
        }

        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(context);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);
        HyperLog.i(TAG, "Running alarm: " + entity);

        if (entity.getDays() > 0) {
            AlarmUtils.setUpNextAlarm(entity, context, false);
        } else {
            sqLiteDBHelper.toggleAlarmActive(alarmId, false);
        }

        setUpSleepTimeAlarm(context);
        SleepTimeAlarmReceiver.cancelNotification(context);
        UpcomingAlarmNotificationReceiver.cancelNotification(context);

        turnScreenOnThroughKeyguard();

        setContentView(R.layout.activity_alarm_receiver);

        TextView alarmInfo = findViewById(R.id.alarm_info);
        alarmInfo.setText(entity.getMessage());

        Button closeButton = findViewById(R.id.alarm_ok);

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

        final MediaPlayer alarmMediaPlayer = new MediaPlayer();


        final Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                try {
                    ticksMediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };


        final Handler handlerVolume = new Handler();
        Runnable runnableVolume = new Runnable() {
            @Override
            public void run() {
                try {
                    volume[0] += 0.001f;
                    HyperLog.v(TAG, "New alarm volume: " + volume[0]);
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


        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(2000);
                }
                alarmMediaPlayer.stop();
                alarmMediaPlayer.reset();
                alarmMediaPlayer.release();
                System.exit(0);
            }
        });
    }
}
