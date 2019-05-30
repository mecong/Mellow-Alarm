package com.mecong.myalarm.activity;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.mecong.myalarm.R;

import java.util.Date;

public class AlarmsListCursorAdapter extends CursorAdapter {
    private MainActivity mainActivity;

    AlarmsListCursorAdapter(MainActivity mainActivity, Context context, Cursor c) {
        super(context, c, 0);
        this.mainActivity = mainActivity;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int alarmRowItem = R.layout.alarm_row_item;
        return LayoutInflater.from(context)
                .inflate(alarmRowItem, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        ImageButton btnDeleteAlarm = view.findViewById(R.id.btnDeleteAlarm);
        final String id = cursor.getString(cursor.getColumnIndex("_id"));
        btnDeleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.deleteAlarm(id);
            }
        });

        Switch switchToggleAlarm = view.findViewById(R.id.switchToggleAlarm);
        boolean active = 1 == cursor.getInt(cursor.getColumnIndex("active"));
        switchToggleAlarm.setChecked(active);
        switchToggleAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainActivity.setActive(id, isChecked);
            }
        });

        TextView textViewPersonName = view.findViewById(R.id.textRow1);
        int hour = cursor.getInt(cursor.getColumnIndex("hour"));
        int minute = cursor.getInt(cursor.getColumnIndex("minute"));
        Date nextTime = new Date(cursor.getLong(cursor.getColumnIndex("next_time")));

        textViewPersonName.setText(context.getString(R.string.alarm_time, hour, minute));

        TextView textRow2 = view.findViewById(R.id.textRow2);

        String daysMessage;
        int days = cursor.getInt(cursor.getColumnIndex("days"));
        if (days == 254) {
            daysMessage = context.getString(R.string.every_day);
        } else if (days == 248) {
            daysMessage = context.getString(R.string.work_days);
        } else if (days == 0) {
            daysMessage = context.getString(R.string.single_day, nextTime);
        } else {
            daysMessage = String.valueOf(days);//TODO: print days
        }
        textRow2.setText(daysMessage);
    }
}
