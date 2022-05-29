package com.mecong.tenderalarm

import com.mecong.tenderalarm.model.AlarmEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class AlarmUtilsTest {
  @Test
  fun test_getNextAlarmDate() {
    val entity = AlarmEntity()
    entity.hour = 17
    entity.minute = 54
    entity.days = 0
    val expectedCalendar = Calendar.getInstance()
    expectedCalendar[Calendar.HOUR_OF_DAY] = 17
    expectedCalendar[Calendar.MINUTE] = 54
    entity.updateNextAlarmDate(true)
    val actualCalendar = Calendar.getInstance()
    actualCalendar.timeInMillis = entity.nextTime
    Assertions.assertEquals(expectedCalendar[Calendar.HOUR_OF_DAY], actualCalendar[Calendar.HOUR_OF_DAY])
    Assertions.assertEquals(expectedCalendar[Calendar.MINUTE], actualCalendar[Calendar.MINUTE])
  }
}