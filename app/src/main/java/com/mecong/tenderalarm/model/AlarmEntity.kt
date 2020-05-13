package com.mecong.tenderalarm.model

import android.database.Cursor
import com.mecong.tenderalarm.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AlarmEntity(var id: Long = -1, var hour: Int = 0, var minute: Int = 0, var days: Int = 0,
                       var exactDate: Long = 0, var ticksTime: Int = 0,
                       var ticksType: Int = 0,
                       var melodyUrl: String? = null, var melodyName: String? = null,
                       var vibrationType: String? = null, var volume: Int? = null,
                       var snoozeMaxTimes: Int = 10, var canceledNextAlarms: Int = 0,
                       var complexity: Int = 1, var message: String? = null,
                       var isActive: Boolean = true, var nextTime: Long = -1L,
                       var nextNotCanceledTime: Long = -1L, var nextRequestCode: Int = -1,
                       var increaseVolume: Int = 5,
                       var isTimeToSleepNotification: Boolean = false, var isHeadsUp: Boolean = false) {


    constructor(cursor: Cursor) : this(
            id = cursor.getLong(cursor.getColumnIndex("_id")),
            hour = cursor.getInt(cursor.getColumnIndex("hour")),
            minute = cursor.getInt(cursor.getColumnIndex("minute")),
            days = cursor.getInt(cursor.getColumnIndex("days")),
            complexity = cursor.getInt(cursor.getColumnIndex("complexity")),
            exactDate = cursor.getLong(cursor.getColumnIndex("exact_date")),
            message = cursor.getString(cursor.getColumnIndex("message")),
            isActive = cursor.getInt(cursor.getColumnIndex("active")) == 1,
            ticksTime = cursor.getInt(cursor.getColumnIndex("ticks_time")),
            ticksType = cursor.getInt(cursor.getColumnIndex("ticks_type")),
            melodyUrl = cursor.getString(cursor.getColumnIndex("melody_url")),
            melodyName = cursor.getString(cursor.getColumnIndex("melody_name")),
            vibrationType = cursor.getString(cursor.getColumnIndex("vibration_type")),
            volume = cursor.getInt(cursor.getColumnIndex("volume")),
            snoozeMaxTimes = cursor.getInt(cursor.getColumnIndex("snooze_max_times")),
            canceledNextAlarms = cursor.getInt(cursor.getColumnIndex("canceled_next_alarms")),
            nextTime = cursor.getLong(cursor.getColumnIndex("next_time")),
            nextNotCanceledTime = cursor.getLong(cursor.getColumnIndex("next_not_canceled_time")),
            nextRequestCode = cursor.getInt(cursor.getColumnIndex("next_request_code")),
            isHeadsUp = cursor.getInt(cursor.getColumnIndex("heads_up")) == 1,
            isTimeToSleepNotification = cursor.getInt(cursor.getColumnIndex("tts_notification")) == 1,
            increaseVolume = cursor.getInt(cursor.getColumnIndex("increase_volume"))
    )

    val isRepeatedAlarm
        get() = days > 0

    val nextTimeWithTicks: Long
        get() = nextTime + TimeUnit.MINUTES.toMillis(ticksTime.toLong())

    private val nextTimeFormatted: String
        get() {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            return format.format(Date(nextTime))
        }

    fun updateNextAlarmDate(manually: Boolean) {
        val calendar = Calendar.getInstance()
        val calendarNow = Calendar.getInstance()
        nextTime = -1L
        if (exactDate == 0L) {
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = minute
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            if (!manually) {
                calendarNow.add(Calendar.MINUTE, ticksTime)
            }
            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (days > 0) {
                val allDaysAsList = allDaysAsList()
                var i = calendar[Calendar.DAY_OF_WEEK] - 1
                var skip = canceledNextAlarms
                while (true) {
                    if (i >= allDaysAsList.size) {
                        i = 0
                    }
                    val fireAtThisDayOfWeek = allDaysAsList[i]
                    if (fireAtThisDayOfWeek) {
                        if (skip <= 0) {
                            break
                        } else {
                            skip--
                            if (nextTime == -1L) {
                                nextTime = calendar.timeInMillis
                            }
                        }
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    i++
                }
            }
        } else {
            calendar.timeInMillis = exactDate
            if (calendar.before(calendarNow)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                exactDate = calendar.timeInMillis
            }
        }

        nextNotCanceledTime = calendar.timeInMillis
        if (nextTime == -1L) {
            if (ticksTime > 0) {
                calendar.add(Calendar.MINUTE, -ticksTime)
            }
            nextTime = calendar.timeInMillis
        }
        nextRequestCode = alarmRequestCode
    }

    val alarmRequestCode: Int
        get() = id.toInt() * 100000

    val upcomingAlarmRequestCode: Int
        get() = alarmRequestCode + 1

    val snoozedAlarmRequestCode: Int
        get() = alarmRequestCode + 2

    val fullScreenIntentCode: Int
        get() = alarmRequestCode + 3


    private fun allDaysAsList(): List<Boolean> {
        return listOf(
                days and SU_BINARY == SU_BINARY,
                days and MO_BINARY == MO_BINARY,
                days and TU_BINARY == TU_BINARY,
                days and WE_BINARY == WE_BINARY,
                days and TH_BINARY == TH_BINARY,
                days and FR_BINARY == FR_BINARY,
                days and SA_BINARY == SA_BINARY)
    }

    override fun toString(): String {
        return "AlarmEntity(id=$id, hour=$hour, minute=$minute, nextTime=$nextTimeFormatted, " +
                "exactDate=$exactDate, ticksTime=$ticksTime, melodyUrl=$melodyUrl, canceled=$canceledNextAlarms, " +
                "complexity=$complexity, message=$message, isActive=$isActive, days=$days)"
    }

    val daysAsMap: Map<Int, Boolean>
        get() {
            val daysMap: MutableMap<Int, Boolean> = LinkedHashMap()
            daysMap[R.string.mo] = days and MO_BINARY == MO_BINARY
            daysMap[R.string.tu] = days and TU_BINARY == TU_BINARY
            daysMap[R.string.we] = days and WE_BINARY == WE_BINARY
            daysMap[R.string.th] = days and TH_BINARY == TH_BINARY
            daysMap[R.string.fr] = days and FR_BINARY == FR_BINARY
            daysMap[R.string.sa] = days and SA_BINARY == SA_BINARY
            daysMap[R.string.su] = days and SU_BINARY == SU_BINARY
            return daysMap
        }

    companion object {
        const val MO_BINARY = 128
        const val TU_BINARY = 64
        const val WE_BINARY = 32
        const val TH_BINARY = 16
        const val SA_BINARY = 4
        const val FR_BINARY = 8
        const val SU_BINARY = 2
        fun daysMarshaling(mo: Boolean, tu: Boolean, we: Boolean, th: Boolean, fr: Boolean, sa: Boolean, su: Boolean): Int {
            var d = 0
            if (mo) {
                d = d or MO_BINARY
            }
            if (tu) {
                d = d or TU_BINARY
            }
            if (we) {
                d = d or WE_BINARY
            }
            if (th) {
                d = d or TH_BINARY
            }
            if (fr) {
                d = d or FR_BINARY
            }
            if (sa) {
                d = d or SA_BINARY
            }
            if (su) {
                d = d or SU_BINARY
            }
            return d
        }
    }
}