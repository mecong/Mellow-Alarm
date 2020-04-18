package com.mecong.tenderalarm.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.TAG
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AlarmNotifyingService : Service() {
    private lateinit var runnableVolume: Runnable
    private lateinit var runnableRealAlarm: Runnable

    var alarmMediaPlayer: MediaPlayer? = null
    var handlerVolume: Handler? = null
    var random = Random()
    var handlerTicks: Handler? = null
    var ticksMediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        EventBus.getDefault().register(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
        val entity = sqLiteDBHelper(this)!!.getAlarmById(alarmId)
        HyperLog.i(TAG, "Running alarm: $entity")
        usePowerManagerWakeup()
        stopForeground(true)
        startAlarmNotification(this, entity)
        startSound(entity)
        return START_NOT_STICKY
    }

    private fun usePowerManagerWakeup() {
        HyperLog.v(TAG, "Alarm Notifying service usePowerManagerWakeup")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.javaClass.canonicalName)
        wakeLock.acquire(TimeUnit.SECONDS.toMillis(10))
    }

    @Subscribe
    fun messageReceived(message: AlarmMessage) {
        when {
            message === AlarmMessage.CANCEL_VOLUME_INCREASE -> {
                cancelVolumeIncreasing()
            }
            message === AlarmMessage.STOP_ALARM -> {
                stopAlarmNotification()
            }
            message === AlarmMessage.SNOOZE2M -> {
                snooze(2)
            }
            message === AlarmMessage.SNOOZE3M -> {
                snooze(3)
            }
            message === AlarmMessage.SNOOZE5M -> {
                snooze(5)
            }
        }
    }

    private fun snooze(minutes: Int) {
        HyperLog.d(TAG, "Snooze for $minutes min")
        handlerTicks!!.removeCallbacksAndMessages(null)
        cancelVolumeIncreasing()
        if (ticksMediaPlayer!!.isPlaying) {
            ticksMediaPlayer!!.pause()
            ticksMediaPlayer!!.seekTo(0)
        }
        if (alarmMediaPlayer!!.isPlaying) {
            alarmMediaPlayer!!.pause()
            alarmMediaPlayer!!.seekTo(0)
        }
        //        handlerTicks.postDelayed(runnableRealAlarm, minutes * 60 * 1000);
//        handlerVolume.postDelayed(runnableVolume, minutes * 60 * 1000);
    }

    private fun cancelVolumeIncreasing() {
        handlerVolume!!.removeCallbacks(runnableVolume)
    }

    private fun startSound(entity: AlarmEntity?) {
        val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0)

        val tick = if (entity?.ticksType == 0) R.raw.tick else R.raw.ding1

        // Create the Handler object (on the main thread by default)
        handlerTicks = Handler()
        val volume = floatArrayOf(0.01f)
        try {
            ticksMediaPlayer = MediaPlayer()
            ticksMediaPlayer!!.setAudioAttributes(audioAttributesAlarm)
            ticksMediaPlayer!!.setDataSource(this, Uri.parse("android.resource://"
                    + packageName + "/" + tick))
            ticksMediaPlayer!!.prepare()
            ticksMediaPlayer!!.setVolume(volume[0], volume[0])
        } catch (e: IOException) {
            //TODO: make correct reaction
            e.printStackTrace()
        }
        try {
            alarmMediaPlayer = MediaPlayer()
            alarmMediaPlayer!!.setAudioAttributes(audioAttributesAlarm)
            val melody = getMelody(this, entity)
            alarmMediaPlayer!!.setDataSource(this, melody)
            alarmMediaPlayer!!.prepare()
            alarmMediaPlayer!!.isLooping = true
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        val predAlarm: Runnable = object : Runnable {
            override fun run() {
                try {
                    if (!ticksMediaPlayer!!.isPlaying) {
                        ticksMediaPlayer!!.start()
                    }
                    HyperLog.v(TAG, "Tick!")
                } catch (e: Exception) {
                    HyperLog.e(TAG, "Exception: " + e.message, e)
                }
                // Repeat this the same runnable code block again another 20 seconds
                // 'this' is referencing the Runnable object
                handlerTicks!!.postDelayed(this, random.nextInt(20000).toLong())
            }
        }
        runnableRealAlarm = Runnable {
            try {
                // Removes pending code execution
                handlerTicks!!.removeCallbacks(predAlarm)
                if (ticksMediaPlayer!!.isPlaying) {
                    ticksMediaPlayer!!.stop()
                }
                volume[0] = 0.01f
                alarmMediaPlayer!!.setVolume(volume[0], volume[0])
                alarmMediaPlayer!!.start()
                handlerVolume!!.removeCallbacks(runnableVolume)
                handlerVolume!!.post(runnableVolume)
                HyperLog.i(TAG, "Real alarm started!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        handlerVolume = Handler()
        runnableVolume = object : Runnable {
            override fun run() {
                try {
                    volume[0] += 0.001f
                    HyperLog.v(TAG, "New alarm volume: " + volume[0])
                    if (alarmMediaPlayer!!.isPlaying) {
                        alarmMediaPlayer!!.setVolume(volume[0], volume[0])
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (volume[0] < 1) handlerVolume!!.postDelayed(this, 2000)
            }
        }
        if (entity!!.ticksTime > 0) {
            handlerTicks!!.post(predAlarm)
        }
        handlerTicks!!.postDelayed(runnableRealAlarm, entity.ticksTime * 60 * 1000.toLong())
    }

    private fun getMelody(context: Context, entity: AlarmEntity?): Uri {
        return if (entity!!.melodyUrl != null) {
            Uri.parse(entity.melodyUrl)
        } else {
            Uri.parse(String.format(Locale.ENGLISH, "android.resource://%s/%d",
                    context.packageName, R.raw.long_music))
        }
    }

    private fun stopAlarmNotification() {
        HyperLog.i(TAG, "Stop Alarm notification")
        handlerTicks!!.removeCallbacksAndMessages(null)
        stopTicksAlarm()
        stopAlarmMediaPlayer()
        alarmEndVibration()
        cancelVolumeIncreasing()
        stopForeground(true)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancelAll()
        stopSelf()
    }

    private fun stopTicksAlarm() {
        if (ticksMediaPlayer!!.isPlaying) {
            ticksMediaPlayer!!.stop()
            ticksMediaPlayer!!.reset()
            ticksMediaPlayer!!.release()
        }
    }

    private fun stopAlarmMediaPlayer() {
        if (alarmMediaPlayer!!.isPlaying) {
            alarmMediaPlayer!!.stop()
            alarmMediaPlayer!!.reset()
            alarmMediaPlayer!!.release()
        }
    }

    private fun alarmEndVibration() {
        val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000,
                    VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(1000)
        }

    }

    private fun startAlarmNotification(context: Context, entity: AlarmEntity?) {
        val alarmId = entity!!.id.toString()
        val startAlarmIntent = Intent(context, AlarmReceiverActivity::class.java)
                .setData(ContentUris.withAppendedId(CONTENT_URI, alarmId.toLong()))
        startAlarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId)
        val pendingIntent = PendingIntent.getActivity(context, 42,
                startAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmNotification = NotificationCompat.Builder(context, MainActivity.ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle("Alarm")
                .setContentText(entity.message)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(0)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(false)
                .setDefaults(NotificationCompat.FLAG_SHOW_LIGHTS or NotificationCompat.FLAG_ONGOING_EVENT or NotificationCompat.FLAG_NO_CLEAR)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
        startForeground(42, alarmNotification.build())
    }

    companion object {
        private val CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/alarms")
    }
}