package com.mecong.myalarm.alarm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mecong.myalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.myalarm.alarm.AlarmUtils.TAG;

public class AlarmReceiverActivity extends AppCompatActivity implements SensorEventListener {
    private static final float SHAKE_THRESHOLD = 7f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    @BindView(R.id.alarm_info)
    TextView alarmInfo;
    @BindView(R.id.alarm_ok)
    Button closeButton;
    private long mLastShakeTime;
    private MediaPlayer alarmMediaPlayer;
    private int shakeCount = 3;
    private Handler handlerVolume;
    private Runnable runnableVolume;

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
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getLocalClassName());
        wakeLock.acquire(TimeUnit.SECONDS.toMillis(10));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_alarm_receiver);

            HyperLog.initialize(this);
            HyperLog.setLogLevel(Log.VERBOSE);
            ButterKnife.bind(this);

            String alarmId = getIntent().getStringExtra(ALARM_ID_PARAM);
            HyperLog.i(TAG, "Running alarm with extras: " + getIntent().getExtras());
            HyperLog.i(TAG, "Running alarm with id: " + alarmId);
            final Context context = getApplicationContext();

            if (alarmId == null) {
                HyperLog.e(TAG, "Alarm id is null");
                System.exit(0);
            }
            initializeShaker();

            AlarmEntity entity = SQLiteDBHelper.getInstance(context).getAlarmById(alarmId);
            HyperLog.i(TAG, "Running alarm: " + entity);

            turnScreenOnThroughKeyguard();

            alarmInfo.setText(entity.getMessage());

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

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    turnOffAlarm();
                }
            });
        } catch (Throwable ex) {
            HyperLog.e(TAG, "Exception in Alarm receiver: " + ex);
        }
    }

    private void turnOffAlarm() {
        Vibrator vibrator = (Vibrator) getApplicationContext()
                .getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(1000);
        }
        alarmMediaPlayer.stop();
        alarmMediaPlayer.reset();
        alarmMediaPlayer.release();
        System.exit(0);
    }

    public void initializeShaker() {
        // Get a sensor manager to listen for shakes
        SensorManager mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }

        // Listen for shakes
        Sensor accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            boolean supported = mSensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            if (!supported) {
                mSensorMgr.unregisterListener(this, accelerometer);
                throw new UnsupportedOperationException("Accelerometer not supported");
            }
        }
    }


    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
                Log.d(TAG, "Acceleration is " + acceleration + "m/s^2");

                if (acceleration > SHAKE_THRESHOLD) {
                    mLastShakeTime = curTime;
                    Log.d(TAG, "Shake, Rattle, and Roll");
                    shakeCount--;
                    Vibrator vibrator = (Vibrator) getApplicationContext()
                            .getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50,
                                VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }

                    if (shakeCount < 3) {
                        handlerVolume.removeCallbacks(runnableVolume);
                    }

                    if (shakeCount <= 0) {
                        turnOffAlarm();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
