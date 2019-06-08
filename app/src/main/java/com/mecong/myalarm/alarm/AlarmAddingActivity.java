package com.mecong.myalarm.alarm;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.mecong.myalarm.R;
import com.mecong.myalarm.model.AlarmEntity;
import com.mecong.myalarm.model.SQLiteDBHelper;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static com.mecong.myalarm.model.AlarmEntity.daysMarshaling;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlarmAddingActivity extends AppCompatActivity {
    TimePicker timePicker;
    CheckBox checkBoxMo;
    CheckBox checkBoxTu;
    CheckBox checkBoxWe;
    CheckBox checkBoxTh;
    CheckBox checkBoxFr;
    CheckBox checkBoxSa;
    CheckBox checkBoxSu;
    SeekBar seekBarTicks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_adding);

        timePicker = findViewById(R.id.alarm_time);
        timePicker.setIs24HourView(true);
        Button buttonOk = findViewById(R.id.ok_button);
        Button buttonCancel = findViewById(R.id.btnCancel);

        checkBoxMo = findViewById(R.id.checkBoxMo);
        checkBoxTu = findViewById(R.id.checkBoxTu);
        checkBoxWe = findViewById(R.id.checkBoxWe);
        checkBoxTh = findViewById(R.id.checkBoxTh);
        checkBoxFr = findViewById(R.id.checkBoxFr);
        checkBoxSa = findViewById(R.id.checkBoxSa);
        checkBoxSu = findViewById(R.id.checkBoxSu);

        final TextView txtTicks = findViewById(R.id.txtTicks);

        seekBarTicks = findViewById(R.id.seekBarTicks);
        txtTicks.setText(getApplicationContext().getString(R.string.ticks, seekBarTicks.getProgress()));
        seekBarTicks.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtTicks.setText(getApplicationContext().getString(R.string.ticks, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDB();

                Intent data = new Intent();
                //---set the data to pass back---
                data.setData(Uri.parse("Result to be returned...."));
                setResult(RESULT_OK, data);
//---close the activity---
                finish();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }

    private void saveToDB() {
        int hour, minute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }
        int days = daysMarshaling(checkBoxMo.isChecked(), checkBoxTu.isChecked(), checkBoxWe.isChecked(), checkBoxTh.isChecked(), checkBoxFr.isChecked(), checkBoxSa.isChecked(), checkBoxSu.isChecked());

        AlarmEntity alarmEntity = AlarmEntity.builder()
                .hour(hour)
                .minute(minute)
                .message("Ooops... alarma")
                .days(days)
                .active(true)
                .ticksTime(seekBarTicks.getProgress())
                .beforeAlarmNotification(true)
                .exactDate(0)
                .canceledNextAlarms(0)
                .nextTime(-1L)
                .nextRequestCode(-1)
                .build();

        SQLiteDBHelper sqLiteDBHelper = new SQLiteDBHelper(getApplicationContext());
        long id = sqLiteDBHelper.addAOrUpdateAlarm(alarmEntity);
        alarmEntity.setId(id);

        AlarmUtils.setUpNextAlarm(alarmEntity, getApplicationContext(), true);
    }
}
