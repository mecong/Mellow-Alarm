package com.mecong.tenderalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.TAG
import com.mecong.tenderalarm.alarm.AlarmUtils.setUpNextAlarm
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HyperLog.initialize(this)
        HyperLog.setLogLevel(Log.INFO)
        setContentView(R.layout.activity_main)
        createNotificationChannels(this)
        val supportFragmentManager = this@MainActivity.supportFragmentManager
        ibOpenSleepAssistant!!.setOnClickListener {
            HyperLog.i(TAG, "Open Sleep Assistant button clicked")
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
            val alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)

            fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            HyperLog.i(TAG, "Found sleep fragment: $sleepFragment")
            HyperLog.i(TAG, "Found alarm fragment: $alarmFragment")
            fragmentTransaction.hide(alarmFragment!!)
            HyperLog.i(TAG, "alarmFragment hide $sleepFragment")
            fragmentTransaction.show(sleepFragment!!)
            HyperLog.i(TAG, "sleepFragment show $sleepFragment")

//            val audioManager = this@MainActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
//            val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//            var volumeCoefficient = systemVolume.toFloat() / streamMaxVolume
//            if (volumeCoefficient < 0.3f || volumeCoefficient > 0.4f) {
//                volumeCoefficient = 0.35f
//                audioManager.setStreamVolume(
//                        AudioManager.STREAM_MUSIC, (streamMaxVolume * volumeCoefficient).toInt(), 0)
//                Toast.makeText(this@MainActivity, "System volume set to 30%", Toast.LENGTH_SHORT).show()
//            }

            ibOpenSleepAssistant!!.setImageResource(R.drawable.sleep_active)
            ibOpenAlarm!!.setImageResource(R.drawable.alarm_inactive)
            fragmentTransaction.commit()
        }
        ibOpenAlarm!!.setOnClickListener {
            HyperLog.i(TAG, "Open Alarm button clicked")
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val sleepFragment = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
            val alarmFragment = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)
            //                fragmentTransaction.addToBackStack("Back to Sleep assistant");
            fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            HyperLog.i(TAG, "Found sleep fragment: $sleepFragment")
            HyperLog.i(TAG, "Found alarm fragment: $alarmFragment")
            fragmentTransaction.hide(sleepFragment!!)
            HyperLog.i(TAG, "sleepFragment hide $sleepFragment")
            fragmentTransaction.show(alarmFragment!!)
            HyperLog.i(TAG, "alarmFragment show $sleepFragment")
            ibOpenAlarm!!.setImageResource(R.drawable.alarm_active)
            ibOpenSleepAssistant!!.setImageResource(R.drawable.sleep_inactive)
            fragmentTransaction.commit()
        }
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        //        fragmentTransaction.addToBackStack("Init");
        val sleepFragment: Fragment = SleepAssistantFragment()
        val alarmFragment: Fragment = MainAlarmFragment()
        fragmentTransaction.add(R.id.container, sleepFragment, SLEEP_FRAGMENT)
        fragmentTransaction.add(R.id.container, alarmFragment, ALARM_FRAGMENT)
        val desiredFragment = intent.getStringExtra(FRAGMENT_NAME_PARAM)
        if (ASSISTANT_FRAGMENT == desiredFragment) {
            fragmentTransaction.hide(alarmFragment)
            HyperLog.i(TAG, "alarmFragment hide $sleepFragment")
            fragmentTransaction.show(sleepFragment)
            HyperLog.i(TAG, "sleepFragment show $sleepFragment")
            ibOpenSleepAssistant!!.setImageResource(R.drawable.sleep_active)
        } else {
            fragmentTransaction.hide(sleepFragment)
            HyperLog.i(TAG, "sleepFragment hide $sleepFragment")
            fragmentTransaction.show(alarmFragment)
            HyperLog.i(TAG, "alarmFragment show $sleepFragment")
            ibOpenAlarm!!.setImageResource(R.drawable.alarm_active)
        }
        fragmentTransaction.commit()
        //        createDebugAlarm();
    }

    private fun createDebugAlarm() {
        val alarmEntity = AlarmEntity()
                .apply {
                    val calendar = Calendar.getInstance()
                    hour = calendar[Calendar.HOUR_OF_DAY]
                    minute = calendar[Calendar.MINUTE] + 2
                    complexity = 1
                    snoozeMaxTimes = 10
                    ticksTime = 1
                    isHeadsUp = true
                    val instance = sqLiteDBHelper(this@MainActivity)
                    val newId = instance!!.addOrUpdateAlarm(this)
                    id = newId

                }
        setUpNextAlarm(alarmEntity, this, true)
    }

    companion object {
        const val TIME_TO_SLEEP_CHANNEL_ID = "TA_TIME_TO_SLEEP_CHANNEL"
        const val BEFORE_ALARM_CHANNEL_ID = "TA_BEFORE_ALARM_CHANNEL"
        const val ALARM_CHANNEL_ID = "TA_BUZZER_CHANNEL"
        const val SLEEP_ASSISTANT_MEDIA_CHANNEL_ID = "TA_SLEEP_ASSISTANT_CHANNEL"
        const val FRAGMENT_NAME_PARAM = BuildConfig.APPLICATION_ID + ".fragment_name"
        const val ASSISTANT_FRAGMENT = "assistant_fragment"
        const val SLEEP_FRAGMENT = "SLEEP_FRAGMENT"
        const val ALARM_FRAGMENT = "ALARM_FRAGMENT"
        fun createNotificationChannels(context: Context) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timeToSleepChannel = NotificationChannel(
                        TIME_TO_SLEEP_CHANNEL_ID,
                        context.getString(R.string.time_to_sleep_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            description = context.getString(R.string.time_to_sleep_channel_description)
                        }

                val beforeAlarmChannel = NotificationChannel(
                        BEFORE_ALARM_CHANNEL_ID,
                        context.getString(R.string.upcoming_alarm_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            description = context.getString(R.string.upcoming_alarm_channel_description)
                        }

                val sleepAssistantChannel = NotificationChannel(
                        SLEEP_ASSISTANT_MEDIA_CHANNEL_ID,
                        context.getString(R.string.sleep_assistant_media_channel_name),
                        NotificationManager.IMPORTANCE_LOW)
                        .apply {
                            setShowBadge(false)
                            description = context.getString(R.string.sleep_assistant_media_channel_description)
                        }


                val alarmChannel = NotificationChannel(
                        ALARM_CHANNEL_ID,
                        context.getString(R.string.buzzer_channel_description),
                        NotificationManager.IMPORTANCE_HIGH)
                        .apply {
                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                            setShowBadge(false)
                            setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
                            description = context.getString(R.string.buzzer_channel_name)
                        }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.apply {
                    createNotificationChannel(timeToSleepChannel)
                    createNotificationChannel(beforeAlarmChannel)
                    createNotificationChannel(alarmChannel)
                    createNotificationChannel(sleepAssistantChannel)
                }

            }
        }
    }
}