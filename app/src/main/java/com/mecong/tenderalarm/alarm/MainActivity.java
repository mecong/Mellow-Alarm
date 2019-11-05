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
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    public static final String TIME_TO_SLEEP_CHANNEL_ID = "TIME_TO_SLEEP";
    public static final String BEFORE_ALARM_CHANNEL_ID = "BEFORE_ALARM_CHANNEL_ID";
    public static final String ALARM_CHANNEL_ID = "MECONG_ALARM_CHANNEL_ID";
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

        if (savedInstanceState != null) {
            return;
        }

        final MainAlarmFragment mainAlarmFragment = new MainAlarmFragment();
        final SleepAssistantFragment sleepAssistantFragment = new SleepAssistantFragment();

        createNotificationChannel();

        final FragmentManager supportFragmentManager = MainActivity.this.getSupportFragmentManager();

        ibOpenSleepAssistant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();

                fragmentTransaction.hide(mainAlarmFragment);
                Fragment sleep_fragment = supportFragmentManager.findFragmentByTag("SLEEP_FRAGMENT");
                if (sleep_fragment == null) {
                    fragmentTransaction.add(R.id.container, sleepAssistantFragment, "SLEEP_FRAGMENT");
                    fragmentTransaction.show(sleepAssistantFragment);
                    ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);
                    ibOpenAlarm.setImageResource(R.drawable.alarm_inactive);
                } else {
                    if (sleep_fragment.isHidden()) {
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
                Fragment alarm_fragment = supportFragmentManager.findFragmentByTag("ALARM_FRAGMENT");
                if (alarm_fragment == null) {
                    fragmentTransaction.add(R.id.container, mainAlarmFragment, "ALARM_FRAGMENT");
                    fragmentTransaction.show(mainAlarmFragment);
                    ibOpenAlarm.setImageResource(R.drawable.alarm_active);
                    ibOpenSleepAssistant.setImageResource(R.drawable.sleep_inactive);
                } else {
                    if (alarm_fragment.isHidden()) {
                        fragmentTransaction.hide(sleepAssistantFragment);
                        fragmentTransaction.show(mainAlarmFragment);
                        ibOpenAlarm.setImageResource(R.drawable.alarm_active);
                        ibOpenSleepAssistant.setImageResource(R.drawable.sleep_inactive);
                    }

                }
                fragmentTransaction.commit();
            }
        });


        supportFragmentManager.beginTransaction()
                .add(R.id.container, mainAlarmFragment, "ALARM_FRAGMENT")
                .commit();
        ibOpenAlarm.setImageResource(R.drawable.alarm_active);

//        supportFragmentManager.beginTransaction()
//                .add(R.id.container, sleepAssistantFragment, "SLEEP_FRAGMENT")
//                .commit();
//        ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active);

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext();

            NotificationChannel timeToSleepChannel = new NotificationChannel(
                    TIME_TO_SLEEP_CHANNEL_ID,
                    context.getString(R.string.time_to_sleep_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            timeToSleepChannel.setDescription(context.getString(R.string.time_to_sleep_channel_description));

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationChannel beforeAlarmChannel = new NotificationChannel(
                    BEFORE_ALARM_CHANNEL_ID,
                    context.getString(R.string.time_to_sleep_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            timeToSleepChannel.setDescription(context.getString(R.string.time_to_sleep_channel_description));

            NotificationChannel alarmChannel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    context.getString(R.string.add),
                    NotificationManager.IMPORTANCE_HIGH);
            alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            alarmChannel.setShowBadge(false);
            alarmChannel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);

            timeToSleepChannel.setDescription(context.getString(R.string.alarm_time));


            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(timeToSleepChannel);
            notificationManager.createNotificationChannel(beforeAlarmChannel);
            notificationManager.createNotificationChannel(alarmChannel);
        }
    }
}
