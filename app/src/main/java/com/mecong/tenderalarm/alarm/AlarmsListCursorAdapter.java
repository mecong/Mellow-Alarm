package com.mecong.tenderalarm.alarm;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ToggleButton;

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
        final String alarmId = String.valueOf(entity.getId());

        final TextView time = view.findViewById(R.id.textRow1);
        final TextView textRow2 = view.findViewById(R.id.textRow2);
        final TextView textViewCanceled = view.findViewById(R.id.textViewCanceled);
        final ToggleButton toggleButton = view.findViewById(R.id.toggleButton);
        final ImageButton btnDeleteAlarm = view.findViewById(R.id.btnDeleteAlarm);

        HyperLog.i(TAG, "binding view : " + view);


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.editAlarm(alarmId);
            }
        });


        btnDeleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(context, v);
                menu.getMenuInflater().inflate(R.menu.menu_media_element, menu.getMenu());
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mainActivity.deleteAlarm(alarmId);
                        return true;
                    }
                });

                menu.show();
            }
        });


        final PopupMenu popup = new PopupMenu(context, toggleButton);
        popup.getMenuInflater().inflate(R.menu.menu_alarm_enable, popup.getMenu());


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_turn_off_1_day) {
                    mainActivity.cancelNextAlarms(alarmId, 1);
                } else if (item.getItemId() == R.id.action_turn_off_2_days) {
                    mainActivity.cancelNextAlarms(alarmId, 2);
                } else if (item.getItemId() == R.id.action_turn_off_3_days) {
                    mainActivity.cancelNextAlarms(alarmId, 3);
                } else if (item.getItemId() == R.id.action_turn_off_4_days) {
                    mainActivity.cancelNextAlarms(alarmId, 4);
                } else if (item.getItemId() == R.id.action_turn_off_5_days) {
                    mainActivity.cancelNextAlarms(alarmId, 5);
                } else {
                    mainActivity.setActive(alarmId, false);
                }

                return true;
            }
        });

        toggleButton.setChecked(entity.isActive());


        toggleButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    HyperLog.i(TAG, ">>>>>>>>>onTouch");

                    if (!toggleButton.isChecked()) {
                        mainActivity.setActive(alarmId, true);
                        toggleButton.setChecked(true);
                    } else {
                        popup.show();//showing popup menu
                    }
                }
                return true;
            }
        });


        time.setText(context.getString(R.string.alarm_time, entity.getHour(), entity.getMinute()));
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
