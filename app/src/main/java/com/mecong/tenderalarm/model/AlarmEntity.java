package com.mecong.tenderalarm.model;

import android.database.Cursor;

import com.mecong.tenderalarm.R;

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

import static java.util.concurrent.TimeUnit.MINUTES;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlarmEntity {
    static final int
            MO_BINARY = 0b10000000, TU_BINARY = 0b01000000, WE_BINARY = 0b00100000,
            TH_BINARY = 0b00010000,
            SA_BINARY = 0b00000100, FR_BINARY = 0b00001000, SU_BINARY = 0b00000010;
    long id;
    int hour;
    int minute;
    @Builder.Default
    int days = 0;
    @Builder.Default
    long exactDate = 0;
    @Builder.Default
    int complexity = 1;
    @ToString.Exclude
    String message;
    @Builder.Default
    boolean active = true;
    int ticksTime;
    @ToString.Exclude
    String melodyUrl;
    @ToString.Exclude
    String melodyName;
    @ToString.Exclude
    String vibrationType;
    @ToString.Exclude
    Integer volume;
    @Builder.Default
    Integer snoozeMaxTimes = 10;
    @Builder.Default
    Integer canceledNextAlarms = 0;
    @Builder.Default
    Long nextTime = -1L;
    @Builder.Default
    Long nextNotCanceledTime = -1L;
    @Builder.Default
    Integer nextRequestCode = -1;
    boolean timeToSleepNotification;
    boolean headsUp;

    public AlarmEntity(Cursor cursor) {

        this.id = cursor.getLong(cursor.getColumnIndex("_id"));
        this.hour = cursor.getInt(cursor.getColumnIndex("hour"));
        this.minute = cursor.getInt(cursor.getColumnIndex("minute"));
        this.days = cursor.getInt(cursor.getColumnIndex("days"));
        this.complexity = cursor.getInt(cursor.getColumnIndex("complexity"));
        this.exactDate = cursor.getLong(cursor.getColumnIndex("exact_date"));
        this.message = cursor.getString(cursor.getColumnIndex("message"));
        this.active = cursor.getInt(cursor.getColumnIndex("active")) == 1;
        this.ticksTime = cursor.getInt(cursor.getColumnIndex("ticks_time"));
        this.melodyUrl = cursor.getString(cursor.getColumnIndex("melody_url"));
        this.melodyName = cursor.getString(cursor.getColumnIndex("melody_name"));
        this.vibrationType = cursor.getString(cursor.getColumnIndex("vibration_type"));
        this.volume = cursor.getInt(cursor.getColumnIndex("volume"));
        this.snoozeMaxTimes = cursor.getInt(cursor.getColumnIndex("snooze_max_times"));
        this.canceledNextAlarms = cursor.getInt(cursor.getColumnIndex("canceled_next_alarms"));
        this.nextTime = cursor.getLong(cursor.getColumnIndex("next_time"));
        this.nextNotCanceledTime = cursor.getLong(cursor.getColumnIndex("next_not_canceled_time"));
        this.nextRequestCode = cursor.getInt(cursor.getColumnIndex("next_request_code"));

        this.headsUp = cursor.getInt(cursor.getColumnIndex("heads_up")) == 1;
        this.timeToSleepNotification = cursor.getInt(cursor.getColumnIndex("tts_notification")) == 1;
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

    public long getNextTimeWithTicks() {
        return getNextTime() + MINUTES.toMillis(getTicksTime());
    }

    @ToString.Include
    private String nextTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return format.format(new Date(nextTime));
    }

    public void updateNextAlarmDate(boolean manually) {
        Calendar calendar = Calendar.getInstance();
        Calendar calendarNow = Calendar.getInstance();
        nextTime = -1L;

        if (exactDate == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (!manually) {
                calendarNow.add(Calendar.MINUTE, ticksTime);
            }

            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (days > 0) {
                List<Boolean> allDaysAsList = allDaysAsList();
                int i = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int skip = canceledNextAlarms;
                while (true) {
                    if (i >= allDaysAsList.size()) {
                        i = 0;
                    }

                    final boolean fireAtThisDayOfWeek = allDaysAsList.get(i);
                    if (fireAtThisDayOfWeek) {
                        if (skip <= 0) {
                            break;
                        } else {
                            skip--;
                            if (nextTime == -1) {
                                nextTime = calendar.getTimeInMillis();
                            }
                        }
                    }

                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    i++;
                }
            }
        } else {
            calendar.setTimeInMillis(exactDate);
            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                exactDate = calendar.getTimeInMillis();
            }
        }

        nextNotCanceledTime = calendar.getTimeInMillis();
        if (nextTime == -1) {
            if (ticksTime > 0) {
                calendar.add(Calendar.MINUTE, -ticksTime);
            }
            nextTime = calendar.getTimeInMillis();
        }

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

    public Map<Integer, Boolean> getDaysAsMap() {
        Map<Integer, Boolean> daysMap = new LinkedHashMap<>();
        daysMap.put(R.string.su, (days & SU_BINARY) == SU_BINARY);
        daysMap.put(R.string.mo, (days & MO_BINARY) == MO_BINARY);
        daysMap.put(R.string.tu, (days & TU_BINARY) == TU_BINARY);
        daysMap.put(R.string.we, (days & WE_BINARY) == WE_BINARY);
        daysMap.put(R.string.th, (days & TH_BINARY) == TH_BINARY);
        daysMap.put(R.string.fr, (days & FR_BINARY) == FR_BINARY);
        daysMap.put(R.string.sa, (days & SA_BINARY) == SA_BINARY);
        return daysMap;
    }
}


