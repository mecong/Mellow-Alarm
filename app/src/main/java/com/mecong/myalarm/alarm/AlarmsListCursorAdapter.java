package com.mecong.myalarm.alarm;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;

import java.util.Map;

public class AlarmsListCursorAdapter extends CursorAdapter {
    private MainActivity mainActivity;

    AlarmsListCursorAdapter(MainActivity mainActivity, Cursor c) {
        super(mainActivity.getApplicationContext(), c, 0);
        this.mainActivity = mainActivity;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        return LayoutInflater.from(context)
                .inflate(R.layout.alarm_row_item, parent, false);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {

        final AlarmEntity entity = new AlarmEntity(cursor);

        ImageButton btnDeleteAlarm = view.findViewById(R.id.btnDeleteAlarm);
        btnDeleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.deleteAlarm(String.valueOf(entity.getId()));
            }
        });

        Switch switchToggleAlarm = view.findViewById(R.id.switchToggleAlarm);
        switchToggleAlarm.setChecked(entity.isActive());
        switchToggleAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainActivity.setActive(String.valueOf(entity.getId()), isChecked);
            }
        });

        TextView time = view.findViewById(R.id.textRow1);
        time.setText(context.getString(R.string.alarm_time, entity.getHour(), entity.getMinute()));

        TextView textViewCanceled = view.findViewById(R.id.textViewCanceled);
        textViewCanceled.setText(entity.getCanceledNextAlarms() > 0 ?
                context.getString(R.string.next_s_cancel, entity.getCanceledNextAlarms())
                : "");


        String daysMessage;
        if (entity.getNextTime() == -1) {
            daysMessage = context.getString(R.string.never);
        } else {
            int days = cursor.getInt(cursor.getColumnIndex("days"));
            if (days == 254) {
                daysMessage = context.getString(R.string.every_day);
            } else if (days == 248) {
                daysMessage = context.getString(R.string.work_days);
            } else if (days == 0) {
                daysMessage = context.getString(R.string.single_day, entity.getNextTime());
            } else {
                daysMessage = getDaysHtml(entity, context);
            }
        }

        TextView textRow2 = view.findViewById(R.id.textRow2);
        textRow2.setText(Html.fromHtml(daysMessage));
    }

    private String getDaysHtml(AlarmEntity entity, final Context context) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, Boolean> day : entity.daysAsMap().entrySet()) {
            if (day.getValue()) {
                builder.append("<font color='#222222'>")
                        .append(context.getString(day.getKey()).toUpperCase())
                        .append("</font>");
            } else {
                builder.append("<font color='#DDDDDD'>")
                        .append(context.getString(day.getKey()).toUpperCase())
                        .append("</font>");
            }
            builder.append(" ");
        }

        return builder.toString();

    }
}
