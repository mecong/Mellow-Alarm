package com.mecong.tenderalarm.alarm;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private static final int READ_REQUEST_CODE = 42;

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
    @BindView(R.id.txtTicks)
    TextView txtTicks;
    @BindView(R.id.seekBarTicks)
    SeekBar seekBarTicks;
    @BindView(R.id.txtSnooze)
    TextView txtSnooze;
    @BindView(R.id.seekBarSnooze)
    SeekBar seekBarSnooze;
    @BindView(R.id.txtComplexity)
    TextView txtComplexity;
    @BindView(R.id.seekBarComplexity)
    SeekBar seekBarComplexity;

    @BindView(R.id.txtMelody)
    TextView txtMelody;
    @BindView(R.id.txtMelodyCaption)
    TextView txtMelodyCaption;

    int selectedMinute;
    int selectedHour;
    int selectedYear;
    int selectedMonth;
    int getSelectedDayOfMonth;
    long exactDate = 0;
    String alarmId = "0";
    String melodyUrl;

    public void timeTextViewClick(View v) {

        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                android.R.style.Theme_DeviceDefault_Dialog,
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
                android.R.style.Theme_DeviceDefault_Dialog,
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

    private void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.toString());
                txtMelody.setText(dumpFileMetaData(uri));
                melodyUrl = uri.toString();
                this.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }


    private String dumpFileMetaData(Uri uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.

        try (Cursor cursor = this.getContentResolver()
                .query(uri, null, null, null, null, null)) {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                HyperLog.i(AlarmUtils.TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                String size;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
                HyperLog.i(AlarmUtils.TAG, "Size: " + size);

                return displayName;
            }
        }

        return null;
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


        txtComplexity.setText(this.getString(R.string.alarm_complexity, seekBarComplexity.getProgress()));
        seekBarComplexity.setOnSeekBarChangeListener(new OnSeekBarChangeAdapter() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    seekBarComplexity.setProgress(1);
                } else {
                    txtComplexity.setText(AlarmAddingActivity.this.getString(R.string.alarm_complexity, progress));
                }
            }
        });

        final OnClickListener melodySelect = new OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        };

        txtMelody.setOnClickListener(melodySelect);
        txtMelodyCaption.setOnClickListener(melodySelect);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (Objects.equals(alarmId, "0")) {
            initFormForNewAlarm();
            timeTextView.performClick();
        } else {
            initFormForExistingAlarm(alarmId);
        }

        updateTimeTextView();
        updateDateTextView();
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

        seekBarComplexity.setProgress(entity.getComplexity());

        seekBarSnooze.setProgress(entity.getSnoozeMaxTimes());

        if (!Strings.isEmptyOrWhitespace(entity.getMelodyName())) {
            txtMelody.setText(entity.getMelodyName());
            melodyUrl = entity.getMelodyUrl();
        }
    }

    private void initFormForNewAlarm() {
        final Calendar c = Calendar.getInstance();
        selectedHour = c.get(Calendar.HOUR_OF_DAY);
        selectedMinute = c.get(Calendar.MINUTE);
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        getSelectedDayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        seekBarComplexity.setProgress(1);
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
                .complexity(seekBarComplexity.getProgress())
                .days(days)
                .message(getMessage())
                .melodyName(txtMelody.getText().toString())
                .melodyUrl(melodyUrl)
                .snoozeMaxTimes(seekBarSnooze.getProgress())
                .ticksTime(seekBarTicks.getProgress())
                .beforeAlarmNotification(true)
                .build();

        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this);
        long id = sqLiteDBHelper.addOrUpdateAlarm(alarmEntity);
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
