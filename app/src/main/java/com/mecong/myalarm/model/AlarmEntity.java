package com.mecong.myalarm.model;

import android.database.Cursor;

import com.mecong.myalarm.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import static java.lang.Math.max;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlarmEntity {
    static final int MO_BINARY = 0b10000000;
    static final int TU_BINARY = 0b01000000;
    static final int WE_BINARY = 0b00100000;
    static final int TH_BINARY = 0b00010000;
    static final int FR_BINARY = 0b00001000;
    static final int SA_BINARY = 0b00000100;
    static final int SU_BINARY = 0b00000010;
    long id;
    int hour;
    int minute;

    Integer days;

    long exactDate;
    @ToString.Exclude
    String message;
    boolean active;
    @ToString.Exclude
    boolean beforeAlarmNotification;
    @ToString.Exclude
    Integer lightTime;
    int ticksTime;
    @ToString.Exclude
    String melody;
    @ToString.Exclude
    String vibrationType;
    @ToString.Exclude
    Integer volume;
    @ToString.Exclude
    Integer snoozeInterval;
    @ToString.Exclude
    Integer snoozeTimes;
    Integer canceledNextAlarms;

    Long nextTime;
    Integer nextRequestCode;

    public AlarmEntity(Cursor cursor) {

        this.id = cursor.getLong(cursor.getColumnIndex("_id"));
        this.hour = cursor.getInt(cursor.getColumnIndex("hour"));
        this.minute = cursor.getInt(cursor.getColumnIndex("minute"));
        this.days = cursor.getInt(cursor.getColumnIndex("days"));
        this.exactDate = cursor.getLong(cursor.getColumnIndex("exact_date"));
        this.message = cursor.getString(cursor.getColumnIndex("message"));
        this.active = cursor.getInt(cursor.getColumnIndex("active")) == 1;
        this.beforeAlarmNotification = cursor.getInt(cursor.getColumnIndex("before_alarm_notification")) == 1;
        this.lightTime = cursor.getInt(cursor.getColumnIndex("light_time"));
        this.ticksTime = cursor.getInt(cursor.getColumnIndex("ticks_time"));
        this.melody = cursor.getString(cursor.getColumnIndex("melody"));
        this.vibrationType = cursor.getString(cursor.getColumnIndex("vibration_type"));
        this.volume = cursor.getInt(cursor.getColumnIndex("volume"));
        this.snoozeInterval = cursor.getInt(cursor.getColumnIndex("snooze_interval"));
        this.snoozeTimes = cursor.getInt(cursor.getColumnIndex("snooze_times"));
        this.canceledNextAlarms = cursor.getInt(cursor.getColumnIndex("canceled_next_alarms"));
        this.nextTime = cursor.getLong(cursor.getColumnIndex("next_time"));
        this.nextRequestCode = cursor.getInt(cursor.getColumnIndex("next_request_code"));
    }

    public static int daysMarshaling(boolean mo, boolean tu, boolean we, boolean th, boolean fr, boolean sa, boolean su) {
        int d = 0;
        if (mo) {
            d |= MO_BINARY;
        }

        if (tu) {
            d |= TU_BINARY;
        }

        if (we) {
            d |= WE_BINARY;
        }

        if (th) {
            d |= TH_BINARY;
        }

        if (fr) {
            d |= FR_BINARY;
        }

        if (sa) {
            d |= SA_BINARY;
        }

        if (su) {
            d |= SU_BINARY;
        }

        return d;
    }

    @ToString.Include
    private String nextTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return format.format(new Date(nextTime));
    }

    public void updateNextAlarmDate(boolean manually) {
        Calendar calendar = Calendar.getInstance();
        Calendar calendarNow = Calendar.getInstance();

        if (exactDate == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (!manually) {
                calendarNow.add(Calendar.MINUTE, ticksTime);
            }

            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, max(canceledNextAlarms, 1));
            }

            if (days > 0) {
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                List<Boolean> allDaysAsList = allDaysAsList();
                for (int i = dayOfWeek - 1; i < 8; i++) {
                    if (i >= allDaysAsList.size()) {
                        i = 0;
                    }

                    if (allDaysAsList.get(i)) {
                        break;
                    } else {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                }
            }
        } else {
            calendar.setTimeInMillis(exactDate);
            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                exactDate = calendar.getTimeInMillis();
            }
        }

        if (ticksTime > 0) {
            calendar.add(Calendar.MINUTE, -ticksTime);
        }

        this.nextTime = calendar.getTimeInMillis();
        this.nextRequestCode = getRequestCode();
    }

    private int getRequestCode() {
        return (int) id * 100000;
    }

    private List<Boolean> allDaysAsList() {
        List<Boolean> allDaysAsList = new ArrayList<>(8);
        allDaysAsList.add((days & SU_BINARY) == SU_BINARY);
        allDaysAsList.add((days & MO_BINARY) == MO_BINARY);
        allDaysAsList.add((days & TU_BINARY) == TU_BINARY);
        allDaysAsList.add((days & WE_BINARY) == WE_BINARY);
        allDaysAsList.add((days & TH_BINARY) == TH_BINARY);
        allDaysAsList.add((days & FR_BINARY) == FR_BINARY);
        allDaysAsList.add((days & SA_BINARY) == SA_BINARY);
        return allDaysAsList;
    }

    public Map<Integer, Boolean> daysAsMap() {
        Map<Integer, Boolean> daysMap = new LinkedHashMap<>();
        daysMap.put(R.string.mo, (days & MO_BINARY) == MO_BINARY);
        daysMap.put(R.string.tu, (days & TU_BINARY) == TU_BINARY);
        daysMap.put(R.string.we, (days & WE_BINARY) == WE_BINARY);
        daysMap.put(R.string.th, (days & TH_BINARY) == TH_BINARY);
        daysMap.put(R.string.fr, (days & FR_BINARY) == FR_BINARY);
        daysMap.put(R.string.sa, (days & SA_BINARY) == SA_BINARY);
        daysMap.put(R.string.su, (days & SU_BINARY) == SU_BINARY);
        return daysMap;
    }
}


