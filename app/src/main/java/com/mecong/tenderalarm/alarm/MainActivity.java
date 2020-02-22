package com.mecong.tenderalarm.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.BuildConfig;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    public static final String TIME_TO_SLEEP_CHANNEL_ID = "TA_TIME_TO_SLEEP_CHANNEL";
    public static final String BEFORE_ALARM_CHANNEL_ID = "TA_BEFORE_ALARM_CHANNEL";
    public static final String ALARM_CHANNEL_ID = "TA_BUZZER_CHANNEL";
    public static final String SLEEP_ASSISTANT_MEDIA_CHANNEL_ID = "TA_SLEEP_ASSISTANT_CHANNEL";
    public static final String FRAGMENT_NAME_PARAM = BuildConfig.APPLICATION_ID + ".fragment_name";
    public static final String ASSISTANT_FRAGMENT = "assistant_fragment";
    public static final String SLEEP_FRAGMENT = "SLEEP_FRAGMENT";
    public static final String ALARM_FRAGMENT = "ALARM_FRAGMENT";

    @BindView(R.id.ibOpenSleepAssistant)
    ImageButton ibOpenSleepAssistant;
    @BindView(R.id.ibOpenAlarm)
    ImageButton ibOpenAlarm;

    public static void createNotificationChannels(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel timeToSleepChannel = new NotificationChannel(
                    TIME_TO_SLEEP_CHANNEL_ID,
                    context.getString(R.string.time_to_sleep_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            timeToSleepChannel.setDescription(context.getString(R.string.time_to_sleep_channel_description));

            NotificationChannel beforeAlarmChannel = new NotificationChannel(
                    BEFORE_ALARM_CHANNEL_ID,
                    context.getString(R.string.upcoming_alarm_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            beforeAlarmChannel.setDescription(context.getString(R.string.upcoming_alarm_channel_description));

            NotificationChannel sleepAssistantChannel = new NotificationChannel(
                    SLEEP_ASSISTANT_MEDIA_CHANNEL_ID,
                    context.getString(R.string.sleep_assistant_media_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            sleepAssistantChannel.setShowBadge(false);
            sleepAssistantChannel.setDescription(context.getString(R.string.sleep_assistant_media_channel_description));

            NotificationChannel alarmChannel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    context.getString(R.string.buzzer_channel_description),
                    NotificationManager.IMPORTANCE_HIGH);
            alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            alarmChannel.setShowBadge(false);
            alarmChannel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            alarmChannel.setDescription(context.getString(R.string.buzzer_channel_name));

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(timeToSleepChannel);
                notificationManager.createNotificationChannel(beforeAlarmChannel);
                notificationManager.createNotificationChannel(alarmChannel);
                notificationManager.createNotificationChannel(sleepAssistantChannel);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.INFO);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        createNotificationChannels(this);

        final FragmentManager supportFragmentManager = MainActivity.this.getSupportFragmentManager();

        ibOpenSleepAssistant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HyperLog.i(AlarmUtils.TAG, "Open Sleep Assistant button clicked");
                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
                final Fragment sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT);
                final Fragment alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT);
//                fragmentTransaction.addToBackStack("Back to Alarms");
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);


                HyperLog.i(AlarmUtils.TAG, "Found sleep fragment: " + sleepFragment);
                HyperLog.i(AlarmUtils.TAG, "Found alarm fragment: " + alarmFragment);

                fragmentTransaction.hide(alarmFragment);
                HyperLog.i(AlarmUtils.TAG, "alarmFragment hide " + sleepFragment);

                fragmentTransaction.show(sleepFragment);
                HyperLog.i(AlarmUtils.TAG, "sleepFragment show " + sleepFragment);

                AudioManager audioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
                int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                float volumeCoefficient = (float) systemVolume / streamMaxVolume;

                if (volumeCoefficient < 0.3f || volumeCoefficient > 0.4f) {
                    volumeCoefficient = 0.35f;
                    audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC, (int) (streamMaxVolume * volumeCoefficient), 0);
                    Toast.makeText(MainActivity.this, "System volume set to 30%", Toast.LENGTH_SHORT).show();
                }

                ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
                ibOpenAlarm.setImageResource(R.drawable.alarm_inactive);
                fragmentTransaction.commit();
            }
        });

        ibOpenAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HyperLog.i(AlarmUtils.TAG, "Open Alarm button clicked");
                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
                final Fragment sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT);
                final Fragment alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT);
//                fragmentTransaction.addToBackStack("Back to Sleep assistant");
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);


                HyperLog.i(AlarmUtils.TAG, "Found sleep fragment: " + sleepFragment);
                HyperLog.i(AlarmUtils.TAG, "Found alarm fragment: " + alarmFragment);

                fragmentTransaction.hide(sleepFragment);
                HyperLog.i(AlarmUtils.TAG, "sleepFragment hide " + sleepFragment);
                fragmentTransaction.show(alarmFragment);
                HyperLog.i(AlarmUtils.TAG, "alarmFragment show " + sleepFragment);

                ibOpenAlarm.setImageResource(R.drawable.alarm_active);
                ibOpenSleepAssistant.setImageResource(R.drawable.sleep_inactive);
                fragmentTransaction.commit();
            }
        });

        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
//        fragmentTransaction.addToBackStack("Init");

        final Fragment sleepFragment = new SleepAssistantFragment();
        final Fragment alarmFragment = new MainAlarmFragment();

        fragmentTransaction.add(R.id.container, sleepFragment, SLEEP_FRAGMENT);
        fragmentTransaction.add(R.id.container, alarmFragment, ALARM_FRAGMENT);


        final String desiredFragment = getIntent().getStringExtra(FRAGMENT_NAME_PARAM);
        if (ASSISTANT_FRAGMENT.equals(desiredFragment)) {
            fragmentTransaction.hide(alarmFragment);
            HyperLog.i(AlarmUtils.TAG, "alarmFragment hide " + sleepFragment);
            fragmentTransaction.show(sleepFragment);
            HyperLog.i(AlarmUtils.TAG, "sleepFragment show " + sleepFragment);

            ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
        } else {
            fragmentTransaction.hide(sleepFragment);
            HyperLog.i(AlarmUtils.TAG, "sleepFragment hide " + sleepFragment);
            fragmentTransaction.show(alarmFragment);
            HyperLog.i(AlarmUtils.TAG, "alarmFragment show " + sleepFragment);

            ibOpenAlarm.setImageResource(R.drawable.alarm_active);
        }

        fragmentTransaction.commit();

//        createDebugAlarm();
    }

    private void createDebugAlarm() {
        Calendar calendar = Calendar.getInstance();
        final SQLiteDBHelper instance = SQLiteDBHelper.getInstance(this);
        AlarmEntity alarmEntity = AlarmEntity.builder()
                .hour(calendar.get(Calendar.HOUR_OF_DAY))
                .minute(calendar.get(Calendar.MINUTE) + 2)
                .complexity(1)
                .snoozeMaxTimes(10)
                .ticksTime(1)
                .headsUp(true)
                .build();

        long id = instance.addOrUpdateAlarm(alarmEntity);

        alarmEntity.setId(id);
        AlarmUtils.setUpNextAlarm(alarmEntity, this, true);
    }
}
