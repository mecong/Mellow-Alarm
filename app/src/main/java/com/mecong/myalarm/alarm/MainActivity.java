package com.mecong.myalarm.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.LogsActivity;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;
import com.mecong.myalarm.sleep_assistant.SleepAssistantActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.mecong.myalarm.alarm.AlarmUtils.MINUTE;
import static com.mecong.myalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification;

public class MainActivity extends AppCompatActivity {
    public static final String TIME_TO_SLEEP_CHANNEL_ID = "TIME_TO_SLEEP";
    public static final String BEFORE_ALARM_CHANNEL_ID = "BEFORE_ALARM_CHANNEL_ID";
    private static final int ALARM_ADDING_REQUEST_CODE = 42;
    private AlarmsListCursorAdapter alarmsAdapter;
    private TextView textNextAlarm, textNextAlarmDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.VERBOSE);
        Context context = this.getApplicationContext();
        createNotificationChannel();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textNextAlarm = findViewById(R.id.textNextAlarm);
        textNextAlarmDate = findViewById(R.id.textNextAlarmDate);
        View viewById = findViewById(R.id.alarmsInfo);
        viewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addAlarmIntent = new Intent(MainActivity.this, SleepAssistantActivity.class);
                MainActivity.this.startActivity(addAlarmIntent);
            }
        });

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        alarmsAdapter =
                new AlarmsListCursorAdapter(this, sqLiteDBHelper.getAllAlarms());

        ListView alarmsList = findViewById(R.id.alarms_list);
        alarmsList.setAdapter(alarmsAdapter);
        updateNextActiveAlarm();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addAlarmIntent = new Intent(MainActivity.this, AlarmAddingActivity.class);
                MainActivity.this.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE);
            }
        });
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

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(timeToSleepChannel);
            notificationManager.createNotificationChannel(beforeAlarmChannel);
        }
    }

    private void updateNextActiveAlarm() {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(getApplicationContext());

        AlarmEntity nextActiveAlarm = sqLiteDBHelper.getNextActiveAlarm();
        Context context = getApplicationContext();
        if (nextActiveAlarm != null) {
            AlarmUtils.setBootReceiverActive(context);

            Calendar calendar = Calendar.getInstance();
            long nextAlarmTime = nextActiveAlarm.getNextTime()
                    + TimeUnit.MINUTES.toMillis(nextActiveAlarm.getTicksTime());
            long difference = nextAlarmTime - calendar.getTimeInMillis();
            calendar.setTime(new Date(difference));
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (difference < MINUTE) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_soon));
            } else if (difference < AlarmUtils.HOUR) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_within_hour,
                        calendar.get(Calendar.MINUTE)));
            } else if (difference < AlarmUtils.DAY) {
                textNextAlarm.setText(context.getString(R.string.next_alarm_today,
                        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
            } else {
                textNextAlarm.setText(context.getString(R.string.next_alarm,
                        calendar.get(Calendar.DAY_OF_YEAR)));
            }
            textNextAlarmDate.setText(context
                    .getString(R.string.next_alarm_date, nextAlarmTime));
        } else {
            textNextAlarm.setText(R.string.all_alarms_are_off);
            textNextAlarmDate.setText("");
        }

        setUpNextSleepTimeNotification(context);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(getApplicationContext());

        // Check which request we're responding to
        if (requestCode == ALARM_ADDING_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
                updateNextActiveAlarm();
                Snackbar.make(findViewById(R.id.alarms_list),
                        "New alarm created", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }
    }

    void deleteAlarm(String id) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(getApplicationContext());

        AlarmUtils.turnOffAlarm(id, getApplicationContext());
        sqLiteDBHelper.deleteAlarm(id);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
    }

    void setActive(String id, boolean active) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(getApplicationContext());

        if (active) {
            AlarmUtils.setUpNextAlarm(id, getApplicationContext(), true);
        } else {
            AlarmUtils.turnOffAlarm(id, getApplicationContext());
        }
        sqLiteDBHelper.toggleAlarmActive(id, active);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
        sqLiteDBHelper.close();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//         Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logs) {
            Intent addAlarmIntent = new Intent(MainActivity.this, LogsActivity.class);
            MainActivity.this.startActivity(addAlarmIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
