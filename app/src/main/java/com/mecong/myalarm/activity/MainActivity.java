package com.mecong.myalarm.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.AlarmUtils;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.mecong.myalarm.AlarmUtils.MINUTE;
import static com.mecong.myalarm.AlarmUtils.setUpSleepTimeAlarm;

public class MainActivity extends AppCompatActivity {
    public static final int SLEEP_TIME_JOB_ID = 1;
    public static final String CHANNEL_ID = "CHANNEL_ID";
    private static final int ALARM_ADDING = 42;
    private SQLiteDBHelper sqLiteDBHelper;
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

        sqLiteDBHelper = new SQLiteDBHelper(context);
        Cursor cursor = sqLiteDBHelper.getAllAlarms();
        alarmsAdapter =
                new AlarmsListCursorAdapter(this, context, cursor);

        ListView alarmsList = findViewById(R.id.alarms_list);
        alarmsList.setAdapter(alarmsAdapter);
        updateNextActiveAlarm();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                Intent addAlarmIntent = new Intent(MainActivity.this, AlarmAdding.class);
                MainActivity.this.startActivityForResult(addAlarmIntent, ALARM_ADDING);
            }
        });
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNextActiveAlarm() {
        AlarmEntity nextActiveAlarm = sqLiteDBHelper.getNextActiveAlarm();
        Context context = getApplicationContext();
        if (nextActiveAlarm != null) {
            AlarmUtils.setBootReceiverActive(context);

            Calendar calendar = Calendar.getInstance();
            long nextAlarmTime = nextActiveAlarm.getNextTime() + nextActiveAlarm.getTicksTime() * MINUTE;
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

        setUpSleepTimeAlarm(context);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Check which request we're responding to
        if (requestCode == ALARM_ADDING) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
                updateNextActiveAlarm();
            }
        }
    }

    void deleteAlarm(String id) {
        AlarmUtils.cancelNextAlarms(id, getApplicationContext());
        sqLiteDBHelper.deleteAlarm(id);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
    }

    void setActive(String id, boolean active) {
        if (active) {
            AlarmUtils.setUpNextAlarm(id, getApplicationContext(), true);
        } else {
            AlarmUtils.cancelNextAlarms(id, getApplicationContext());
        }
        sqLiteDBHelper.toggleAlarmActive(id, active);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
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
