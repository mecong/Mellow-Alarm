package com.mecong.tenderalarm.alarm;

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

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;

import java.util.Map;

import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;
import static java.lang.Boolean.TRUE;

public class AlarmsListCursorAdapter extends CursorAdapter {
    private MainAlarmFragment mainActivity;

    AlarmsListCursorAdapter(MainAlarmFragment mainActivity, Cursor c) {
        super(mainActivity.getActivity(), c, 0);
        HyperLog.i(TAG, "Cursor count: " + c.getCount());
        this.mainActivity = mainActivity;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        long id = cursor.getLong(cursor.getColumnIndex("_id"));

        View inflate = LayoutInflater.from(context)
                .inflate(R.layout.alarm_row_item, parent, false);

        HyperLog.i(TAG, "New view for id: " + id + " view:" + inflate);
        return inflate;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {

        final AlarmEntity entity = new AlarmEntity(cursor);
        final String id = String.valueOf(entity.getId());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.editAlarm(id);
            }
        });

        ImageButton btnDeleteAlarm = view.findViewById(R.id.btnDeleteAlarm);
        btnDeleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.deleteAlarm(id);
            }
        });


        HyperLog.i(TAG, this.toString());
        Switch switchToggleAlarm = view.findViewById(R.id.switchToggleAlarm);
        switchToggleAlarm.setChecked(entity.isActive());
        switchToggleAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HyperLog.i(TAG, "this is: " + AlarmsListCursorAdapter.this.toString());
                mainActivity.setActive(id, isChecked);
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
            int days = entity.getDays();
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
        for (Map.Entry<Integer, Boolean> day : entity.getDaysAsMap().entrySet()) {
            if (TRUE.equals(day.getValue())) {
                builder.append("<font color='#DDDDDD'>")
                        .append(context.getString(day.getKey()).toUpperCase())
                        .append("</font>");
            } else {
                builder.append("<font color='#222222'>")
                        .append(context.getString(day.getKey()).toUpperCase())
                        .append("</font>");
            }
            builder.append(" ");
        }

        return builder.toString();

    }
}
