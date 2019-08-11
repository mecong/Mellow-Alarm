package com.mecong.myalarm.alarm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.LogsActivity;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;
import static com.mecong.myalarm.alarm.AlarmUtils.MINUTE;
import static com.mecong.myalarm.alarm.AlarmUtils.setUpNextSleepTimeNotification;

public class MainAlarmFragment extends Fragment {


    private static final int ALARM_ADDING_REQUEST_CODE = 42;
    private AlarmsListCursorAdapter alarmsAdapter;
    private TextView textNextAlarm, textNextAlarmDate;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.content_main, container, false);

        final Context context = this.getActivity().getApplicationContext();
        HyperLog.initialize(context);
        HyperLog.setLogLevel(Log.VERBOSE);

        textNextAlarm = rootView.findViewById(R.id.textNextAlarm);
        textNextAlarmDate = rootView.findViewById(R.id.textNextAlarmDate);

        textNextAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addAlarmIntent = new Intent(context, LogsActivity.class);
                startActivity(addAlarmIntent);
            }
        });


        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);
        alarmsAdapter =
                new AlarmsListCursorAdapter(this, sqLiteDBHelper.getAllAlarms());

        ListView alarmsList = rootView.findViewById(R.id.alarms_list);
        alarmsList.setAdapter(alarmsAdapter);
        updateNextActiveAlarm();

        Button fab = rootView.findViewById(R.id.btnAddAlarm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addAlarmIntent = new Intent(context, AlarmAddingActivity.class);
                MainAlarmFragment.this.startActivityForResult(addAlarmIntent, ALARM_ADDING_REQUEST_CODE);
            }
        });

        return rootView;
    }


    private void updateNextActiveAlarm() {
        Context context = this.getActivity().getApplicationContext();
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        AlarmEntity nextActiveAlarm = sqLiteDBHelper.getNextActiveAlarm();
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Context context = this.getActivity().getApplicationContext();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        // Check which request we're responding to
        if (requestCode == ALARM_ADDING_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
                updateNextActiveAlarm();
                Snackbar.make(this.textNextAlarm,
                        "New alarm created", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }
    }

    void deleteAlarm(String id) {
        Context context = this.getActivity().getApplicationContext();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        AlarmUtils.turnOffAlarm(id, context);
        sqLiteDBHelper.deleteAlarm(id);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
    }

    void setActive(String id, boolean active) {
        Context context = this.getActivity().getApplicationContext();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(context);

        if (active) {
            AlarmUtils.setUpNextAlarm(id, context, true);
        } else {
            AlarmUtils.turnOffAlarm(id, context);
        }
        sqLiteDBHelper.toggleAlarmActive(id, active);
        alarmsAdapter.changeCursor(sqLiteDBHelper.getAllAlarms());
        updateNextActiveAlarm();
        sqLiteDBHelper.close();
    }
}
