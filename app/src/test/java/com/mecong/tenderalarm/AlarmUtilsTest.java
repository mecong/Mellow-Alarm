package com.mecong.tenderalarm;

import com.mecong.tenderalarm.model.AlarmEntity;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlarmUtilsTest {

    @Test
    void getNextAlarmDate() {

        AlarmEntity entity = new AlarmEntity();
        entity.setHour(17);
        entity.setMinute(54);
        entity.setDays(0b00000000);

        Calendar expectedCalendar = Calendar.getInstance();
        expectedCalendar.set(Calendar.HOUR_OF_DAY, 17);
        expectedCalendar.set(Calendar.MINUTE, 54);

        entity.updateNextAlarmDate(true);
        Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(entity.getNextTime());

        assertEquals(expectedCalendar.get(Calendar.HOUR_OF_DAY), actualCalendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(expectedCalendar.get(Calendar.MINUTE), actualCalendar.get(Calendar.MINUTE));
    }
}