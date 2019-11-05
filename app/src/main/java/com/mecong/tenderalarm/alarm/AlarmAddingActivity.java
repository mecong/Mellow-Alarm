package com.mecong.tenderalarm.alarm;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.util.Strings;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.AlarmEntity;
import com.mecong.tenderalarm.model.SQLiteDBHelper;
import com.mecong.tenderalarm.utils.OnSeekBarChangeAdapter;

import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static com.mecong.tenderalarm.model.AlarmEntity.daysMarshaling;

@FieldDefaults(level = AccessLevel.PACKAGE)
public class AlarmAddingActivity extends AppCompatActivity {
    @BindView(R.id.timeTextView)
    TextView timeTextView;
    @BindView(R.id.dateTextView)
    TextView dateTextView;
    @BindView(R.id.txtAlarmMessage)
    EditText txtAlarmMessage;
    @BindView(R.id.checkBoxMo)
    CheckBox checkBoxMo;
    @BindView(R.id.checkBoxTu)
    CheckBox checkBoxTu;
    @BindView(R.id.checkBoxWe)
    CheckBox checkBoxWe;
    @BindView(R.id.checkBoxTh)
    CheckBox checkBoxTh;
    @BindView(R.id.checkBoxFr)
    CheckBox checkBoxFr;
    @BindView(R.id.checkBoxSa)
    CheckBox checkBoxSa;
    @BindView(R.id.checkBoxSu)
    CheckBox checkBoxSu;
    @BindView(R.id.seekBarTicks)
    SeekBar seekBarTicks;
    @BindView(R.id.seekBarSnooze)
    SeekBar seekBarSnooze;
    @BindView(R.id.txtTicks)
    TextView txtTicks;
    @BindView(R.id.txtSnooze)
    TextView txtSnooze;

    int selectedMinute;
    int selectedHour;
    int selectedYear;
    int selectedMonth;
    int getSelectedDayOfMonth;
    long exactDate = 0;
    String alarmId = "0";


    public void timeTextViewClick(View v) {

        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                android.R.style.Theme_Holo_Dialog_NoActionBar,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        updateTimeTextView();
                    }
                }, selectedHour, selectedMinute, true);

        timePickerDialog.show();
    }

    public void selectDateClick(View v) {
        // Launch Time Picker Dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                android.R.style.Theme_Holo_Dialog_NoActionBar,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedYear = year;
                        selectedMonth = month;
                        getSelectedDayOfMonth = dayOfMonth;
                        final Calendar c = Calendar.getInstance();
                        c.set(selectedYear, selectedMonth, getSelectedDayOfMonth,
                                selectedHour, selectedMinute, 0);
                        exactDate = c.getTimeInMillis();
                        updateDateTextView();
                    }
                }, selectedYear, selectedMonth, getSelectedDayOfMonth);

        datePickerDialog.show();
    }

    private void updateTimeTextView() {
        timeTextView.setText(getBaseContext().getString(R.string.alarm_time, selectedHour, selectedMinute));
    }

    private void updateDateTextView() {
        if (exactDate == 0) {
            dateTextView.setVisibility(View.GONE);
        } else {
            dateTextView.setVisibility(View.VISIBLE);
            dateTextView.setText(this.getString(R.string.next_alarm_date, exactDate));
        }
    }

    public void buttonOkClick(View v) {
        AlarmAddingActivity.this.saveToDB();

        Intent data = new Intent();
        //---set the data to pass back---
        data.setData(Uri.parse("Result to be returned...."));
        setResult(RESULT_OK, data);
        //---close the activity---
        finish();
    }

    public void buttonCancelClick(View v) {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    public void workDaysClick(View v) {
        checkBoxMo.setChecked(!checkBoxMo.isChecked());
        checkBoxTu.setChecked(!checkBoxTu.isChecked());
        checkBoxWe.setChecked(!checkBoxWe.isChecked());
        checkBoxTh.setChecked(!checkBoxTh.isChecked());
        checkBoxFr.setChecked(!checkBoxFr.isChecked());
    }

    public void allDaysClick(View v) {
        workDaysClick(v);
        checkBoxSa.setChecked(!checkBoxSa.isChecked());
        checkBoxSu.setChecked(!checkBoxSu.isChecked());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initLogsAndControls();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            alarmId = extras.getString("alarmId");
        }


        if (Objects.equals(alarmId, "0")) {
            initFormForNewAlarm();
            timeTextView.performClick();
        } else {
            initFormForExistingAlarm(alarmId);
        }

        updateTimeTextView();
        updateDateTextView();

        txtTicks.setText(this.getString(R.string.ticks, seekBarTicks.getProgress()));
        seekBarTicks.setOnSeekBarChangeListener(new OnSeekBarChangeAdapter() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtTicks.setText(AlarmAddingActivity.this.getString(R.string.ticks, progress));
            }
        });

        txtSnooze.setText(this.getString(R.string.snooze, seekBarSnooze.getProgress()));
        seekBarSnooze.setOnSeekBarChangeListener(new OnSeekBarChangeAdapter() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtSnooze.setText(AlarmAddingActivity.this.getString(R.string.snooze, progress));
            }
        });
    }

    private void initFormForExistingAlarm(String alarmId) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this);
        AlarmEntity entity = sqLiteDBHelper.getAlarmById(alarmId);

        selectedMinute = entity.getMinute();
        selectedHour = entity.getHour();

        txtAlarmMessage.setText(entity.getMessage());
        exactDate = entity.getExactDate();


        final Calendar c = Calendar.getInstance();
        if (exactDate > 0) c.setTimeInMillis(exactDate);
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        getSelectedDayOfMonth = c.get(Calendar.DAY_OF_MONTH);

        seekBarTicks.setProgress(entity.getTicksTime());

        Map<Integer, Boolean> daysAsMap = entity.getDaysAsMap();
        checkBoxMo.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.mo)));
        checkBoxTu.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.tu)));
        checkBoxWe.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.we)));
        checkBoxTh.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.th)));
        checkBoxFr.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.fr)));
        checkBoxSa.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.sa)));
        checkBoxSu.setChecked(Boolean.TRUE.equals(daysAsMap.get(R.string.su)));

//        seekBarSnooze.setProgress(entity.getSnoozeInterval());
    }

    private void initFormForNewAlarm() {
        final Calendar c = Calendar.getInstance();
        selectedHour = c.get(Calendar.HOUR_OF_DAY);
        selectedMinute = c.get(Calendar.MINUTE);
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        getSelectedDayOfMonth = c.get(Calendar.DAY_OF_MONTH);
    }

    private void initLogsAndControls() {
        HyperLog.initialize(this);
        HyperLog.setLogLevel(Log.VERBOSE);
        HyperLog.i(AlarmUtils.TAG, "Start AlarmAddingActivity");

        setContentView(R.layout.activity_alarm_adding);
        ButterKnife.bind(this);
    }

    private void saveToDB() {
        int days = daysMarshaling(checkBoxMo.isChecked(), checkBoxTu.isChecked(),
                checkBoxWe.isChecked(), checkBoxTh.isChecked(),
                checkBoxFr.isChecked(), checkBoxSa.isChecked(),
                checkBoxSu.isChecked());

        AlarmEntity alarmEntity = AlarmEntity.builder()
                .id(Long.parseLong(alarmId))
                .hour(selectedHour)
                .minute(selectedMinute)
                .exactDate(exactDate)
                .days(days)
                .message(getMessage())
                .ticksTime(seekBarTicks.getProgress())
                .beforeAlarmNotification(true)
                .build();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this);
        long id = sqLiteDBHelper.addAOrUpdateAlarm(alarmEntity);
        alarmEntity.setId(id);

        AlarmUtils.setUpNextAlarm(alarmEntity, this, true);
    }

    @NonNull
    private String getMessage() {
        String message = txtAlarmMessage.getText().toString();
        String defaultMessage = "Ooops... alarma";
        return Strings.isEmptyOrWhitespace(message) ? defaultMessage : message;
    }
}
