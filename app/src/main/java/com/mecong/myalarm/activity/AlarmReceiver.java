package com.mecong.myalarm.activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.io.IOException;

import static com.mecong.myalarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.myalarm.AlarmUtils.TAG;
import static com.mecong.myalarm.AlarmUtils.setUpSleepTimeAlarm;

public class AlarmReceiver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.VERBOSE);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


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

        if (entity.getDays() > 0 || entity.getExactDate() != null) {
            AlarmUtils.setUpNextAlarm(entity, context, false);
        } else {
            sqLiteDBHelper.toggleAlarmActive(alarmId, false);
        }

        setUpSleepTimeAlarm(context);

        setContentView(R.layout.activity_alarm_receiver);

        TextView alarmInfo = findViewById(R.id.alarm_info);
        alarmInfo.setText(entity.getMessage());

        Button closeButton = findViewById(R.id.alarm_ok);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0);

        // Create the Handler object (on the main thread by default)
        final Handler handlerTicks = new Handler();
        final float[] volume = {0.4f};

        final MediaPlayer ticksMediaPlayer = new MediaPlayer();
        try {
            ticksMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
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

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(2000);

                    volume[0] = 0.01f;
                    alarmMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
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
                alarmMediaPlayer.stop();
                alarmMediaPlayer.reset();
                alarmMediaPlayer.release();
                System.exit(0);
            }
        });
    }
}
