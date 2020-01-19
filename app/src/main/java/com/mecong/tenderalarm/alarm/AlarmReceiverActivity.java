package com.mecong.tenderalarm.alarm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.alarm.turnoff.AlarmTurnOffComponent;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

public class AlarmReceiverActivity extends FragmentActivity implements SensorEventListener {
    private static final float SHAKE_THRESHOLD = 7f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECONDS = 1000;

    @BindView(R.id.alarm_info)
    TextView alarmInfo;
    @BindView(R.id.taskNote)
    TextView taskNote;
    @BindView(R.id.turnOffComponent)
    AlarmTurnOffComponent alarmTurnOffComponent;

    @BindView(R.id.btnSnooze2m)
    Button btnSnooze2m;
    @BindView(R.id.btnSnooze3m)
    Button btnSnooze3m;
    @BindView(R.id.btnSnooze5m)
    Button btnSnooze5m;

    long mLastShakeTime;
    int shakeCount;
    int snoozedMinutes = 0;

    private void turnScreenOnThroughKeyguard() {
        usePowerManagerWakeup();
        useWindowFlags();
        useActivityScreenMethods();
    }

    private void useWindowFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    private void useActivityScreenMethods() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                HyperLog.i(TAG, "useActivityScreenMethods");

                this.setTurnScreenOn(true);
                this.setShowWhenLocked(true);
            } catch (NoSuchMethodError e) {
                HyperLog.e(TAG, "Enable setTurnScreenOn and setShowWhenLocked is not present on device!", e);
            }
        }
    }

    public static void unlockScreen(AlarmReceiverActivity activity) {

        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // in addition to flags
            activity.setShowWhenLocked(true);
            activity.setTurnScreenOn(true);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService(KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(activity, new KeyguardManager.KeyguardDismissCallback() {
                    @Override
                    public void onDismissError() {
                        super.onDismissError();
                        HyperLog.i(TAG, "Keyguard Dismiss Error");
                    }

                    @Override
                    public void onDismissSucceeded() {
                        super.onDismissSucceeded();
                        HyperLog.i(TAG, "Keyguard Dismiss Success");
                    }

                    @Override
                    public void onDismissCancelled() {
                        super.onDismissCancelled();
                        HyperLog.i(TAG, "Keyguard Dismiss Cancelled");
                    }
                });
            }
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }


    }

    private void usePowerManagerWakeup() {
        HyperLog.i(TAG, "usePowerManagerWakeup");

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getLocalClassName());
            wakeLock.acquire(TimeUnit.SECONDS.toMillis(10));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            HyperLog.initialize(this);
            HyperLog.setLogLevel(Log.VERBOSE);

            turnScreenOnThroughKeyguard();


            // Close dialogs and window shade, so this is fully visible
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

            // Honor rotation on tablets; fix the orientation on phones.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

            setContentView(R.layout.activity_alarm_receiver);

            ButterKnife.bind(this);

            String alarmId = getIntent().getStringExtra(ALARM_ID_PARAM);
            HyperLog.i(TAG, "Running alarm with extras: " + getIntent().getExtras());
            HyperLog.i(TAG, "Running alarm with id: " + alarmId);
            final Context context = getApplicationContext();

            if (alarmId == null) {
                HyperLog.e(TAG, "Alarm id is null");
                System.exit(0);
//                alarmId = "1";
            }
            initializeShaker();

            final AlarmEntity entity = SQLiteDBHelper.getInstance(context).getAlarmById(alarmId);
            HyperLog.i(TAG, "Running alarm: " + entity);

            alarmInfo.setText(entity.getMessage());

            int complexity = entity.getComplexity();
            shakeCount = complexity * 2;
            taskNote.setText(this.getString(R.string.alarm_turn_off_prompt, shakeCount));
            alarmTurnOffComponent.setComplexity(complexity);


            final OnClickListener snoozeOnClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int time = Integer.parseInt(v.getTag().toString());
                    snoozedMinutes += time;
                    if (time == 2) {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE2M);
                    } else if (time == 3) {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE3M);
                    } else {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE5M);
                    }
                    btnSnooze2m.setVisibility(View.INVISIBLE);
                    btnSnooze3m.setVisibility(View.INVISIBLE);
                    btnSnooze5m.setVisibility(View.INVISIBLE);

                    HyperLog.d(TAG, "Snoozed Minutes: " + snoozedMinutes + " max: " + entity.getSnoozeMaxTimes());

                    if (snoozedMinutes < entity.getSnoozeMaxTimes()) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btnSnooze2m.setVisibility(View.VISIBLE);
                                btnSnooze3m.setVisibility(View.VISIBLE);
                                btnSnooze5m.setVisibility(View.VISIBLE);
                            }
                        }, time * 60000);
                    }
                }
            };
            btnSnooze2m.setOnClickListener(snoozeOnClickListener);
            btnSnooze3m.setOnClickListener(snoozeOnClickListener);
            btnSnooze5m.setOnClickListener(snoozeOnClickListener);
        } catch (Exception ex) {
            HyperLog.e(TAG, "Exception in Alarm receiver: " + ex);
        }
    }


    private void cancelVolumeIncreasing() {
        EventBus.getDefault().post(AlarmMessage.CANCEL_VOLUME_INCREASE);
    }

    private void turnOffAlarm() {
        EventBus.getDefault().post(AlarmMessage.STOP_ALARM);
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
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECONDS) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
//                Log.d(TAG, "Acceleration is " + acceleration + "m/s^2");

                if (acceleration > SHAKE_THRESHOLD) {

                    mLastShakeTime = curTime;
                    Log.d(TAG, "Shake, Rattle, and Roll");
                    Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(200,
                                    VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(200);
                        }
                    }

                    cancelVolumeIncreasing();


                    if (--shakeCount <= 0) {
                        HyperLog.d(TAG, "Alarm stopped by accelerometer");

                        turnOffAlarm();
                    }
                    taskNote.setText(this.getString(R.string.alarm_turn_off_prompt, shakeCount));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
