package com.mecong.tenderalarm.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.BuildConfig;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.VERBOSE);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        final MainAlarmFragment mainAlarmFragment = new MainAlarmFragment();
        final SleepAssistantFragment sleepAssistantFragment = new SleepAssistantFragment();

        createNotificationChannels();

        final FragmentManager supportFragmentManager = MainActivity.this.getSupportFragmentManager();

        ibOpenSleepAssistant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();

                fragmentTransaction.hide(mainAlarmFragment);
                Fragment sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT);
                if (sleepFragment == null) {
                    fragmentTransaction.add(R.id.container, sleepAssistantFragment, SLEEP_FRAGMENT);
                    fragmentTransaction.show(sleepAssistantFragment);
                    ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
                    ibOpenAlarm.setImageResource(R.drawable.alarm_inactive);
                } else {
                    if (sleepFragment.isHidden()) {
                        fragmentTransaction.hide(mainAlarmFragment);
                        fragmentTransaction.show(sleepAssistantFragment);
                        ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
                        ibOpenAlarm.setImageResource(R.drawable.alarm_inactive);
                    }

                }
                fragmentTransaction.commit();
            }
        });

        ibOpenAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();

                fragmentTransaction.hide(sleepAssistantFragment);
                Fragment alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT);
                if (alarmFragment == null) {
                    fragmentTransaction.add(R.id.container, mainAlarmFragment, ALARM_FRAGMENT);
                    fragmentTransaction.show(mainAlarmFragment);
                    ibOpenAlarm.setImageResource(R.drawable.alarm_active);
                    ibOpenSleepAssistant.setImageResource(R.drawable.sleep_inactive);
                } else {
                    if (alarmFragment.isHidden()) {
                        fragmentTransaction.hide(sleepAssistantFragment);
                        fragmentTransaction.show(mainAlarmFragment);
                        ibOpenAlarm.setImageResource(R.drawable.alarm_active);
                        ibOpenSleepAssistant.setImageResource(R.drawable.sleep_inactive);
                    }

                }
                fragmentTransaction.commit();
            }
        });


        final String desiredFragment = getIntent().getStringExtra(FRAGMENT_NAME_PARAM);
        if (ASSISTANT_FRAGMENT.equals(desiredFragment)) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, sleepAssistantFragment, SLEEP_FRAGMENT)
                    .commit();
            ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
        } else {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, mainAlarmFragment, ALARM_FRAGMENT)
                    .commit();
            ibOpenAlarm.setImageResource(R.drawable.alarm_active);
        }

//        Calendar calendar = Calendar.getInstance();
//        final SQLiteDBHelper instance = SQLiteDBHelper.getInstance(this);
//        AlarmEntity alarmEntity = AlarmEntity.builder()
//                .hour(calendar.get(Calendar.HOUR_OF_DAY))
//                .minute(calendar.get(Calendar.MINUTE) + 2)
//                .complexity(1)
//                .snoozeMaxTimes(10)
//                .ticksTime(10)
//                .beforeAlarmNotification(true)
//                .build();
//
//        long id= instance.addOrUpdateAlarm(alarmEntity);
//
//        alarmEntity.setId(id);
//        AlarmUtils.setUpNextAlarm(alarmEntity,this, true);
    }

    private void createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext();

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

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(timeToSleepChannel);
                notificationManager.createNotificationChannel(beforeAlarmChannel);
                notificationManager.createNotificationChannel(alarmChannel);
                notificationManager.createNotificationChannel(sleepAssistantChannel);
            }
        }
    }
}
