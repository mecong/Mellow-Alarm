package com.mecong.tenderalarm.alarm

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.AlarmEntity
import java.util.*

class AlarmsListCursorAdapter constructor(private val mainActivity: MainAlarmFragment, c: Cursor?) : CursorAdapter(mainActivity.activity, c, 0) {
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context)
                .inflate(R.layout.alarm_row_item, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val entity = AlarmEntity(cursor)
        val alarmId = entity.id.toString()
        val time = view.findViewById<TextView>(R.id.textRow1)
        val textRow2 = view.findViewById<TextView>(R.id.textRow2)
        val textViewCanceled = view.findViewById<TextView>(R.id.textViewCanceled)
        val toggleButton = view.findViewById<ImageButton>(R.id.toggleButton)
        val btnDeleteAlarm = view.findViewById<ImageButton>(R.id.btnDeleteAlarm)

        view.setOnClickListener { mainActivity.editAlarm(alarmId) }
        btnDeleteAlarm.setOnClickListener { v ->
            val menu = PopupMenu(context, v)
            menu.menuInflater.inflate(R.menu.menu_media_element, menu.menu)
            menu.setOnMenuItemClickListener {
                mainActivity.deleteAlarm(alarmId)
                true
            }
            menu.show()
        }

        val popup = PopupMenu(context, toggleButton)
        popup.menuInflater.inflate(R.menu.menu_alarm_enable, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_turn_off_1_day && entity.canceledNextAlarms != 1) {
                mainActivity.cancelNextAlarms(alarmId, 1)
            } else if (item.itemId == R.id.action_turn_off_2_days && entity.canceledNextAlarms != 2) {
                mainActivity.cancelNextAlarms(alarmId, 2)
            } else if (item.itemId == R.id.action_turn_off_3_days && entity.canceledNextAlarms != 3) {
                mainActivity.cancelNextAlarms(alarmId, 3)
            } else if (item.itemId == R.id.action_turn_off_4_days && entity.canceledNextAlarms != 4) {
                mainActivity.cancelNextAlarms(alarmId, 4)
            } else if (item.itemId == R.id.action_turn_off_5_days && entity.canceledNextAlarms != 5) {
                mainActivity.cancelNextAlarms(alarmId, 5)
            } else if (item.itemId == R.id.turn_off_alarm) {
                mainActivity.setActive(alarmId, false)
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                toggleButton.tag = false
            } else if (item.itemId == R.id.turn_on_alarm) {
                mainActivity.setActive(alarmId, true)
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
                toggleButton.tag = true
            }
            true
        }

        if (entity.isActive) {
            toggleButton.setImageResource(R.drawable.ic_alarm_on)
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
                mainActivity.setActive(alarmId, true)
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
                toggleButton.tag = true
            }
        }

        val oneTimeAlarmSwitch = View.OnClickListener {
            val checked = toggleButton.tag as Boolean
            if (checked) {
                mainActivity.setActive(alarmId, false)
                toggleButton.setImageResource(R.drawable.ic_alarm_off)
                toggleButton.tag = false
            } else {
                mainActivity.setActive(alarmId, true)
                toggleButton.setImageResource(R.drawable.ic_alarm_on)
                toggleButton.tag = true
            }
        }

        toggleButton.setOnClickListener(
                if (entity.days > 0) recurrentAlarmSwitch else oneTimeAlarmSwitch)

        time.text = context.getString(R.string.alarm_time, entity.hour, entity.minute)

        textViewCanceled.text = if (entity.canceledNextAlarms > 0) context.getString(R.string.next_s_cancel, entity.canceledNextAlarms) else ""

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

        textRow2.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(daysMessage, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(daysMessage)
        }
    }

    private fun getDaysHtml(entity: AlarmEntity, context: Context): String {
        val builder = StringBuilder()
        for ((key, value) in entity.daysAsMap) {
            if (java.lang.Boolean.TRUE == value) {
                builder.append("<font color='#DDDDDD'>")
                        .append(context.getString(key).toUpperCase(Locale.getDefault()))
                        .append("</font>")
            } else {
                builder.append("<font color='#555555'>")
                        .append(context.getString(key).toUpperCase(Locale.getDefault()))
                        .append("</font>")
            }
            builder.append(" ")
        }
        return builder.toString()
    }

}