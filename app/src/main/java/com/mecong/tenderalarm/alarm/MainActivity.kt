package com.mecong.tenderalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.RadioServiceStatus
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import it.sephiroth.android.library.xtooltip.ClosePolicy.Companion.TOUCH_ANYWHERE_NO_CONSUME
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber

enum class MainActivityMessages {
    ADD_ALARM,
    SWITCH_PLAYBACK
}

private var currentFragment = MainActivity.ALARM_FRAGMENT
private var sleepAssistantStatus = RadioServiceStatus.IDLE

class MainActivity : AppCompatActivity() {


    private val sleepAssistantFragmentOpenListener: (v: View) -> Unit = {
        Timber.v("Open Sleep Assistant button clicked")
        val alarmFragment: Fragment = alarmFragmentInstance(supportFragmentManager, null, true)!!
        val sleepFragment: Fragment = sleepFragmentInstance(supportFragmentManager, null, true)!!
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
        fragmentTransaction.hide(alarmFragment)
        fragmentTransaction.show(sleepFragment)

        currentFragment = SLEEP_FRAGMENT

        fragmentTransaction.commit()
        setButtons()

    }

    private val alarmFragmentOpenListener: (v: View) -> Unit = {
        Timber.v("Open Alarm button clicked")
        val alarmFragment: Fragment = alarmFragmentInstance(supportFragmentManager, null, true)!!
        val sleepFragment: Fragment = sleepFragmentInstance(supportFragmentManager, null, true)!!
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
        fragmentTransaction.hide(sleepFragment)
        fragmentTransaction.show(alarmFragment)

        currentFragment = ALARM_FRAGMENT

        fragmentTransaction.commit()
        setButtons()

    }

    private val addAlarmListener: (v: View) -> Unit = {
        EventBus.getDefault().post(MainActivityMessages.ADD_ALARM)
    }

    private val switchPlayStateListener: (v: View) -> Unit = {
        EventBus.getDefault().post(MainActivityMessages.SWITCH_PLAYBACK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannels(this)

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)


        val supportFragmentManager = this@MainActivity.supportFragmentManager
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        //        fragmentTransaction.addToBackStack("Init");


        val desiredFragment = intent.getStringExtra(FRAGMENT_NAME_PARAM)
//        val desiredFragment = if (BuildConfig.DEBUG) {
//            ASSISTANT_FRAGMENT
//        } else {
//            intent.getStringExtra(FRAGMENT_NAME_PARAM)
//        }


        currentFragment = if (ASSISTANT_FRAGMENT == desiredFragment) {
            val alarmFragment: Fragment? = alarmFragmentInstance(supportFragmentManager, fragmentTransaction, false)
            if (alarmFragment != null) {
                fragmentTransaction.hide(alarmFragment)
            }

            val sleepFragment: Fragment = sleepFragmentInstance(supportFragmentManager, fragmentTransaction, true)!!
            fragmentTransaction.show(sleepFragment)

            Timber.d("alarmFragment hide $sleepFragment")
            Timber.d("sleepFragment show $sleepFragment")
            SLEEP_FRAGMENT
        } else {
            val sleepFragment: Fragment? = sleepFragmentInstance(supportFragmentManager, fragmentTransaction, false)
            if (sleepFragment != null) {
                fragmentTransaction.hide(sleepFragment)
            }

            val alarmFragment: Fragment = alarmFragmentInstance(supportFragmentManager, fragmentTransaction, true)!!
            fragmentTransaction.show(alarmFragment)

            Timber.d("sleepFragment hide $sleepFragment")
            Timber.d("alarmFragment show $sleepFragment")
            ALARM_FRAGMENT
        }

        fragmentTransaction.commit()
        setButtons()
    }


    private fun alarmFragmentInstance(supportFragmentManager: FragmentManager,
                                      fragmentTransaction: FragmentTransaction?, mandatory: Boolean): Fragment? {
        var alarmFragment: Fragment? = supportFragmentManager.findFragmentByTag(ALARM_FRAGMENT)
        if (alarmFragment == null && mandatory) {
            alarmFragment = AlarmFragment()
            val fragmentTransaction1 = supportFragmentManager.beginTransaction()

            fragmentTransaction1.add(R.id.container, alarmFragment, ALARM_FRAGMENT)
            fragmentTransaction1.commit()
        }
        return alarmFragment
    }

    private fun sleepFragmentInstance(supportFragmentManager: FragmentManager,
                                      fragmentTransaction: FragmentTransaction?, mandatory: Boolean): Fragment? {
        var sleepFragment: Fragment? = supportFragmentManager.findFragmentByTag(SLEEP_FRAGMENT)
        if (sleepFragment == null && mandatory) {
            sleepFragment = SleepAssistantFragment()
            val fragmentTransaction1 = supportFragmentManager.beginTransaction()

            fragmentTransaction1.add(R.id.container, sleepFragment, SLEEP_FRAGMENT)
            fragmentTransaction1.commit()

        }
        return sleepFragment
    }


    override fun onResume() {
        super.onResume()

        val sqLiteDBHelper = SQLiteDBHelper.sqLiteDBHelper(this)!!
        val firstAlarmAdded = sqLiteDBHelper.getPropertyInt(PropertyName.FIRST_ALARM_ADDED) ?: 0

        if (firstAlarmAdded == 0) {
            val tooltip = Tooltip.Builder(this)
                    .anchor(ibOpenAlarm, 0, 0, false)
                    .text(this.getString(R.string.first_alarm_prompt))
                    .arrow(true)
                    .floatingAnimation(Tooltip.Animation.SLOW)
                    .closePolicy(TOUCH_ANYWHERE_NO_CONSUME)
                    .showDuration(10 * 1000)
                    .create()

            ibOpenAlarm.post { tooltip.show(ibOpenAlarm, Tooltip.Gravity.TOP, true) }

//            val tooltip2 = Tooltip.Builder(this)
//                    .anchor(ibOpenSleepAssistant, 0, 0, false)
//                    .text("Try Sleep Assistant")
//                    .arrow(true)
//                    .floatingAnimation(Tooltip.Animation.SLOW)
//                    .closePolicy(TOUCH_ANYWHERE_NO_CONSUME)
//                    .showDuration(15 * 1000)
//                    .create()
//
//            ibOpenSleepAssistant.post { tooltip2.show(ibOpenSleepAssistant, Tooltip.Gravity.TOP, true) }
        }
    }

    @Subscribe
    fun onEvent(status: RadioServiceStatus) {
        sleepAssistantStatus = status
        setButtons()
    }

    private fun setButtons() {
        if (currentFragment == SLEEP_FRAGMENT) {

            when (sleepAssistantStatus) {
                RadioServiceStatus.LOADING -> {
                    ibOpenSleepAssistant.setImageResource(R.drawable.ic_hour_glass)
                }
                RadioServiceStatus.ERROR -> {
                    ibOpenSleepAssistant.setImageResource(R.drawable.play_btn)
                }
                RadioServiceStatus.PLAYING -> {
                    ibOpenSleepAssistant.setImageResource(R.drawable.pause_btn)
                }
                else -> {
                    ibOpenSleepAssistant.setImageResource(R.drawable.play_btn)
                }
            }

            ibOpenAlarm.setImageResource(R.drawable.alarm_active)
            val paddingInDp = 10 // 10 dps
            val scale = resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
            ibOpenAlarm.setPadding(0, paddingInPx, 0, paddingInPx)


//            ibOpenSleepAssistant.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            ibOpenAlarm.setOnClickListener(alarmFragmentOpenListener)
            ibOpenSleepAssistant.setOnClickListener(switchPlayStateListener)


//            ibOpenAlarm.clearColorFilter()
        } else {
            ibOpenAlarm.setImageResource(R.drawable.ic_round_add_alarm_24)
            val paddingInDp = 7 // 7 dps
            val scale = resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
            ibOpenAlarm.setPadding(0, paddingInPx, 0, paddingInPx)
//            ibOpenAlarm.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
            ibOpenAlarm.setOnClickListener(addAlarmListener)
            ibOpenSleepAssistant.setOnClickListener(sleepAssistantFragmentOpenListener)

            ibOpenSleepAssistant.setImageResource(R.drawable.sleep_active)
//            ibOpenSleepAssistant.clearColorFilter()
        }
    }

//   private fun createDebugAlarm() {
//        val alarmEntity = AlarmEntity()
//                .apply {
//                    val calendar = Calendar.getInstance()
//                    hour = calendar[Calendar.HOUR_OF_DAY]
//                    minute = calendar[Calendar.MINUTE] + 2
//                    complexity = 1
//                    snoozeMaxTimes = 10
//                    ticksTime = 1
//                    isHeadsUp = true
//                    val instance = sqLiteDBHelper(this@MainActivity)
//                    val newId = instance!!.addOrUpdateAlarm(this)
//                    id = newId
//                }
//        setUpNextAlarm(alarmEntity, this, true)
//        AlarmUtils.setUpNextSleepTimeNotification(this)

//    }

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