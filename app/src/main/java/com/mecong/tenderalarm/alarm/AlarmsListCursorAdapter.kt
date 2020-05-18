package com.mecong.tenderalarm.alarm

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.text.Html
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.AlarmEntity

class AlarmsListCursorAdapter constructor(private val activity: AlarmFragment, c: Cursor?) : CursorAdapter(activity.activity, c, 0) {
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context)
                .inflate(R.layout.alarm_row_item, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val entity = AlarmEntity(cursor)
        val alarmId = entity.id.toString()
        val textRowHours = view.findViewById<TextView>(R.id.textRowHours)
        val textRowMinutes = view.findViewById<TextView>(R.id.textRowMinutes)
        val textRowDate = view.findViewById<TextView>(R.id.textRowDate)
        val textRowAlarmTitle = view.findViewById<TextView>(R.id.textRowAlarmTitle)
        val textViewSkipped = view.findViewById<TextView>(R.id.textViewSkipped)
        val toggleButton = view.findViewById<ImageButton>(R.id.toggleButton)
        val alarmOnOff = view.findViewById<LinearLayout>(R.id.alarmOnOff)

        val btnDeleteAlarm = view.findViewById<ImageButton>(R.id.btnDeleteAlarm)

        view.setOnClickListener { activity.editAlarm(alarmId) }
        btnDeleteAlarm.setOnClickListener { v ->

            val wrapper = ContextThemeWrapper(context, R.style.MyPopupMenu)
            val menu = PopupMenu(wrapper, v)

            menu.menuInflater.inflate(R.menu.menu_media_element, menu.menu)
            menu.setOnMenuItemClickListener {
                activity.deleteAlarm(alarmId)
                true
            }
            menu.show()
        }

        val wrapper = ContextThemeWrapper(context, R.style.MyPopupMenu)
        val popup = PopupMenu(wrapper, toggleButton)

        popup.menuInflater.inflate(R.menu.menu_alarm_enable, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_turn_off_1_day && entity.canceledNextAlarms != 1) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                activity.cancelNextAlarms(alarmId, 1)
            } else if (item.itemId == R.id.action_turn_off_2_days && entity.canceledNextAlarms != 2) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                activity.cancelNextAlarms(alarmId, 2)
            } else if (item.itemId == R.id.action_turn_off_3_days && entity.canceledNextAlarms != 3) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                activity.cancelNextAlarms(alarmId, 3)
            } else if (item.itemId == R.id.action_turn_off_4_days && entity.canceledNextAlarms != 4) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                activity.cancelNextAlarms(alarmId, 4)
            } else if (item.itemId == R.id.action_turn_off_5_days && entity.canceledNextAlarms != 5) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                activity.cancelNextAlarms(alarmId, 5)
            } else if (item.itemId == R.id.turn_off_alarm) {
                activity.setActive(alarmId, false)
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                toggleButton.tag = false
            } else if (item.itemId == R.id.turn_on_alarm) {
                activity.setActive(alarmId, true)
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
                toggleButton.tag = true
            }
            true
        }

        if (entity.isActive) {
            if (entity.canceledNextAlarms > 0) {
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
            } else {
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
            }
            toggleButton.tag = true
        } else {
            toggleButton.setImageResource(R.drawable.ic_alarm_off)
            toggleButton.tag = false
        }

        val recurrentAlarmSwitch = View.OnClickListener {
            val checked = toggleButton.tag as Boolean
            if (checked) {
                popup.show()
            } else {
                activity.setActive(alarmId, true)
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
                toggleButton.tag = true
            }
        }

        val oneTimeAlarmSwitch = View.OnClickListener {
            val checked = toggleButton.tag as Boolean
            if (checked) {
                activity.setActive(alarmId, false)
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                toggleButton.tag = false
            } else {
                activity.setActive(alarmId, true)
                if (entity.canceledNextAlarms > 0) {
                    toggleButton.setImageResource(R.drawable.ic_alarm_off)
                } else {
                    toggleButton.setImageResource(R.drawable.ic_alarm_on)
                }
                toggleButton.tag = true
            }
        }

        alarmOnOff.setOnClickListener(
                if (entity.days > 0) recurrentAlarmSwitch else oneTimeAlarmSwitch)

        if (textRowMinutes == null) {
            textRowHours.text = context.getString(R.string.alarm_time, entity.hour, entity.minute)
        } else {
            textRowHours.text = context.getString(R.string.alarm_time_chunk, entity.hour)
            textRowMinutes.text = context.getString(R.string.alarm_time_chunk, entity.minute)
        }

        if (entity.canceledNextAlarms > 0) {
            textViewSkipped.text = entity.canceledNextAlarms.toString()
            textViewSkipped.visibility = VISIBLE
        } else {
            textViewSkipped.text = "0"
            textViewSkipped.visibility = INVISIBLE
        }


        val daysMessage = if (entity.nextTime == -1L) {
            context.getString(R.string.never)
        } else {
            when (entity.days) {
                254 -> {
                    context.getString(R.string.every_day)
                }
                248 -> {
                    context.getString(R.string.work_days)
                }
                0 -> {
                    context.getString(R.string.single_day, entity.nextTime)
                }
                else -> {
                    getDaysHtml(entity, context)
                }
            }
        }

        textRowDate.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(daysMessage, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(daysMessage)
        }

        if (BuildConfig.DEBUG) {
            textRowAlarmTitle.text = "$alarmId::${entity.message}"
        } else {
            textRowAlarmTitle.text = entity.message
        }
    }

    private fun getDaysHtml(entity: AlarmEntity, context: Context): String {
        val builder = StringBuilder()
        for ((key, value) in entity.daysAsMap) {
            if (java.lang.Boolean.TRUE == value) {
                builder.append("<font color='#E9DEDE'>")
                        .append(context.getString(key))
                        .append("</font>")
            } else {
                builder.append("<font color='#666666'>")
                        .append(context.getString(key))
                        .append("</font>")
            }
            builder.append(" ")
        }
        return builder.toString()
    }

}