package com.mecong.tenderalarm.alarm

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.snoozeAlarmNotification
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import kotlinx.android.synthetic.main.activity_alarm_receiver.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt
import kotlin.system.exitProcess

class AlarmReceiverActivity : FragmentActivity(), SensorEventListener {
    private var shareThreshold = 7f // m/S**2

    private var mLastShakeTime: Long = 0
    private var shakeCount = 0
    var snoozedMinutes: Long = 0
    private fun turnScreenOnThroughKeyguard() {
        usePowerManagerWakeup()
        useWindowFlags()
        useActivityScreenMethods()
    }

    private fun useWindowFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
    }

    private fun useActivityScreenMethods() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                //HyperLog.i(TAG, "useActivityScreenMethods")
                setTurnScreenOn(true)
                setShowWhenLocked(true)
            } catch (e: NoSuchMethodError) {
                //HyperLog.e(TAG, "Enable setTurnScreenOn and setShowWhenLocked is not present on device!", e)
            }
        }
    }

    private fun usePowerManagerWakeup() {
        //HyperLog.i(TAG, "alarm receiver PowerManagerWakeup")
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.localClassName)
        wakeLock.acquire(TimeUnit.SECONDS.toMillis(10))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = Color.TRANSPARENT
    }

    @Subscribe
    fun messageReceived(message: AlarmMessage) {
        //HyperLog.i(TAG, "Stop Activity with message: $message")
        if (message === AlarmMessage.STOP_ALARM) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
//            HyperLog.initialize(this)
//            HyperLog.setLogLevel(Log.ERROR)
//            EventBus.getDefault().register(this)
            turnScreenOnThroughKeyguard()

//            unlockScreen(this)

            // Close dialogs and window shade, so this is fully visible
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

            // Honor rotation on tablets; fix the orientation on phones.
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
            setContentView(R.layout.activity_alarm_receiver)
            val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
            //HyperLog.i(TAG, "Running alarm with extras: " + intent.extras)
            //HyperLog.i(TAG, "Running alarm with id: $alarmId")
            val context = applicationContext
            if (alarmId == null) {
                //HyperLog.e(TAG, "Alarm id is NULL")
                exitProcess(0)
                //                alarmId = "1";
            }

            initializeShaker()
            sleepTimer.visibility = View.GONE

            val entity = sqLiteDBHelper(context)!!.getAlarmById(alarmId)
            alarm_info.text = entity!!.message
            //HyperLog.i(TAG, "Running alarm: $entity")
            val complexity = entity.complexity
            shakeCount = complexity * 2

            taskNote.text = this.resources.getQuantityString(R.plurals.alarm_turn_off_prompt, shakeCount, shakeCount)

            turnOffComponent.complexity = complexity

            val snoozeOnClickListener = View.OnClickListener { v ->
                val time = v.tag.toString().toInt()
                snoozedMinutes += time.toLong()
                when (time) {
                    2 -> {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE2M)
                    }
                    3 -> {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE3M)
                    }
                    else -> {
                        EventBus.getDefault().post(AlarmMessage.SNOOZE5M)
                    }
                }

                btnSnooze2m.visibility = View.GONE
                btnSnooze3m.visibility = View.GONE
                btnSnooze5m.visibility = View.GONE
                sleepTimer.visibility = View.VISIBLE

                val sleepText = Date(time * 60000L)
                val handlerSleepTime = Handler()
                sleepTimer.text = context.getString(R.string.sleep_timer, sleepText)

                val sleepTimeClock: Runnable = object : Runnable {
                    override fun run() {
                        val newTime = sleepText.time - 1000
                        sleepText.time = newTime
                        sleepTimer.text = context.getString(R.string.sleep_timer, sleepText)
                        if (newTime >= 0) {
                            handlerSleepTime.postDelayed(this, 1000)
                        } else {
                            sleepTimer!!.visibility = View.GONE

                            btnSnooze2m!!.visibility = View.VISIBLE
                            btnSnooze3m!!.visibility = View.VISIBLE
                            btnSnooze5m!!.visibility = View.VISIBLE

                            val snoozedMinutesLeft = entity.snoozeMaxTimes - snoozedMinutes
                            if (snoozedMinutesLeft < 1) {
                                btnSnooze2m!!.visibility = View.GONE
                            }

                            if (snoozedMinutesLeft < 3) {
                                btnSnooze3m!!.visibility = View.GONE
                            }

                            if (snoozedMinutesLeft < 5) {
                                btnSnooze5m!!.visibility = View.GONE
                            }
                        }
                    }
                }
                handlerSleepTime.post(sleepTimeClock)
                //HyperLog.d(TAG, "Snoozed Minutes: " + snoozedMinutes + " max: " + entity.snoozeMaxTimes)
                snoozeAlarmNotification(time, entity, context)
            }

            if (entity.snoozeMaxTimes < 1) {
                btnSnooze2m!!.visibility = View.GONE
            }

            if (entity.snoozeMaxTimes < 3) {
                btnSnooze3m!!.visibility = View.GONE
            }

            if (entity.snoozeMaxTimes < 5) {
                btnSnooze5m!!.visibility = View.GONE
            }

            btnSnooze2m.setOnClickListener(snoozeOnClickListener)
            btnSnooze3m.setOnClickListener(snoozeOnClickListener)
            btnSnooze5m.setOnClickListener(snoozeOnClickListener)

        } catch (ex: Exception) {
            //HyperLog.e(TAG, "Exception in Alarm receiver: $ex")
        }
    }

    private fun cancelVolumeIncreasing() {
        EventBus.getDefault().post(AlarmMessage.CANCEL_VOLUME_INCREASE)
    }

    private fun turnOffAlarm() {
        EventBus.getDefault().post(AlarmMessage.STOP_ALARM)
    }

    private fun initializeShaker() {
        // Get a sensor manager to listen for shakes
        val mSensorMgr = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Listen for shakes
        val accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            val supported = mSensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            if (!supported) {
                mSensorMgr.unregisterListener(this, accelerometer)
                throw UnsupportedOperationException("Accelerometer not supported")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val curTime = System.currentTimeMillis()
            if (curTime - mLastShakeTime > MIN_TIME_BETWEEN_SHAKES_MILLISECONDS) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val acceleration = sqrt(x * x + y * y + (z * z).toDouble()) - SensorManager.GRAVITY_EARTH
                //                Log.d(TAG, "Acceleration is " + acceleration + "m/s^2");
                if (acceleration > shareThreshold) {
                    mLastShakeTime = curTime
                    //HyperLog.d(TAG, "Shake, Rattle, and Roll")
                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200,
                                VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(200)
                    }

                    cancelVolumeIncreasing()
                    if (--shakeCount <= 0) {
                        //HyperLog.d(TAG, "Alarm stopped by accelerometer")
                        turnOffAlarm()
                    }
                    taskNote!!.text = this.resources.getQuantityString(R.plurals.alarm_turn_off_prompt, shakeCount, shakeCount)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        //HyperLog.i(TAG, "Sensor accuracy: $accuracy")
        shareThreshold = 10f - accuracy
    }

    companion object {

        private const val MIN_TIME_BETWEEN_SHAKES_MILLISECONDS = 1000
        fun unlockScreen(activity: AlarmReceiverActivity) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // in addition to flags
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            } else {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
//                keyguardManager.requestDismissKeyguard(activity, object : KeyguardDismissCallback() {
//                    override fun onDismissError() {
//                        super.onDismissError()
//                        //HyperLog.i(TAG, "Keyguard Dismiss Error")
//                    }
//
//                    override fun onDismissSucceeded() {
//                        super.onDismissSucceeded()
//                        //HyperLog.i(TAG, "Keyguard Dismiss Success")
//                    }
//
//                    override fun onDismissCancelled() {
//                        super.onDismissCancelled()
//                        //HyperLog.i(TAG, "Keyguard Dismiss Cancelled")
//                    }
//                })
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

            } else {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        }
    }
}