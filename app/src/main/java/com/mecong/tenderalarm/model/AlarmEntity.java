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

import static java.util.concurrent.TimeUnit.MINUTES;


public class AlarmEntity {
    static final int
            MO_BINARY = 0b10000000, TU_BINARY = 0b01000000, WE_BINARY = 0b00100000,
            TH_BINARY = 0b00010000,
            SA_BINARY = 0b00000100, FR_BINARY = 0b00001000, SU_BINARY = 0b00000010;
    long id;
    int hour;
    int minute;

    int days = 0;

    long exactDate = 0;

    int complexity = 1;

    String message;

    boolean active = true;
    int ticksTime;

    String melodyUrl;

    String melodyName;

    String vibrationType;

    Integer volume;

    Integer snoozeMaxTimes = 10;

    Integer canceledNextAlarms = 0;

    Long nextTime = -1L;

    Long nextNotCanceledTime = -1L;

    Integer nextRequestCode = -1;
    boolean timeToSleepNotification;
    boolean headsUp;

    public AlarmEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public long getExactDate() {
        return exactDate;
    }

    public void setExactDate(long exactDate) {
        this.exactDate = exactDate;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getTicksTime() {
        return ticksTime;
    }

    public void setTicksTime(int ticksTime) {
        this.ticksTime = ticksTime;
    }

    public String getMelodyUrl() {
        return melodyUrl;
    }

    public void setMelodyUrl(String melodyUrl) {
        this.melodyUrl = melodyUrl;
    }

    public String getMelodyName() {
        return melodyName;
    }

    public void setMelodyName(String melodyName) {
        this.melodyName = melodyName;
    }

    public String getVibrationType() {
        return vibrationType;
    }

    public void setVibrationType(String vibrationType) {
        this.vibrationType = vibrationType;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getSnoozeMaxTimes() {
        return snoozeMaxTimes;
    }

    public void setSnoozeMaxTimes(Integer snoozeMaxTimes) {
        this.snoozeMaxTimes = snoozeMaxTimes;
    }

    public Integer getCanceledNextAlarms() {
        return canceledNextAlarms;
    }

    public void setCanceledNextAlarms(Integer canceledNextAlarms) {
        this.canceledNextAlarms = canceledNextAlarms;
    }

    public Long getNextNotCanceledTime() {
        return nextNotCanceledTime;
    }

    public void setNextNotCanceledTime(Long nextNotCanceledTime) {
        this.nextNotCanceledTime = nextNotCanceledTime;
    }

    public Integer getNextRequestCode() {
        return nextRequestCode;
    }

    public void setNextRequestCode(Integer nextRequestCode) {
        this.nextRequestCode = nextRequestCode;
    }

    public boolean isTimeToSleepNotification() {
        return timeToSleepNotification;
    }

    public void setTimeToSleepNotification(boolean timeToSleepNotification) {
        this.timeToSleepNotification = timeToSleepNotification;
    }

    public boolean isHeadsUp() {
        return headsUp;
    }

    public void setHeadsUp(boolean headsUp) {
        this.headsUp = headsUp;
    }

    public Long getNextTime() {
        return nextTime;
    }

    public void setNextTime(Long nextTime) {
        this.nextTime = nextTime;
    }

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

    //@ToString.Include
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


