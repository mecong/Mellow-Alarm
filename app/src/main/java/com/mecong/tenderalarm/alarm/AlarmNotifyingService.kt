package com.mecong.tenderalarm.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.os.Process.killProcess
import android.os.Process.myPid
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_ALARM
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM_SAME_ID
import com.mecong.tenderalarm.alarm.AlarmUtils.TAG
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AlarmNotifyingService : Service() {
    private lateinit var runnableVolume: Runnable
    private lateinit var runnableRealAlarm: Runnable

    var handlerVolume: Handler? = null
    var random = Random()
    var handlerTicks: Handler? = null
    private lateinit var exoPlayer: SimpleExoPlayer

    private fun initExoPlayer(streamUrl: Uri, repeatMode: Int) {
        exoPlayer = SimpleExoPlayer.Builder(applicationContext)
                .build()
                .apply {
                    setWakeMode(C.WAKE_MODE_LOCAL)
                    setHandleAudioBecomingNoisy(true)
                }

        val audioAttributesAlarm = com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setUsage(USAGE_ALARM)
                .build()

        exoPlayer.setAudioAttributes(audioAttributesAlarm, false)

        val dataSourceFactory = DefaultDataSourceFactory(this.baseContext, "Tender Alarm")
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(streamUrl)
        exoPlayer.prepare(mediaSource)
        exoPlayer.repeatMode = repeatMode
    }

    override fun onCreate() {
        EventBus.getDefault().register(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
        val sameId = intent.getBooleanExtra(ALARM_ID_PARAM_SAME_ID, false)
        val entity = sqLiteDBHelper(this)!!.getAlarmById(alarmId)
        HyperLog.i(TAG, "Running alarm: $entity")
        usePowerManagerWakeup()
        stopForeground(true)
        startAlarmNotification(this, entity)

        if (sameId)
            startSnoozedSound(entity)
        else
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

        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
            exoPlayer.seekTo(0)
        }
    }

    private fun cancelVolumeIncreasing() {
        handlerVolume!!.removeCallbacks(runnableVolume)
    }

    private fun startSnoozedSound(entity: AlarmEntity?) {
        handlerTicks!!.post(runnableRealAlarm)
    }

    private fun startSound(entity: AlarmEntity?) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0)

        val defaultVolume = 0.01f
        val volumeIncreaseDelayMs = 2000L
        val ticksTimeMs = entity!!.ticksTime * 60 * 1000
        val amountIncreases = ticksTimeMs / volumeIncreaseDelayMs
        var volumeIncreaseStep = if (amountIncreases.compareTo(0) == 0) 0.01f else (0.6f - defaultVolume) / amountIncreases

        val tick = if (entity.ticksType == 0) R.raw.tick else R.raw.ding1

        // Create the Handler object (on the main thread by default)
        handlerTicks = Handler()

        var volumeCounter = defaultVolume
        try {
            initExoPlayer(RawResourceDataSource.buildRawResourceUri(tick), Player.REPEAT_MODE_OFF)
            exoPlayer.volume = volumeCounter
        } catch (e: IOException) {
            //TODO: make correct reaction
            e.printStackTrace()
        }

        val predAlarm = object : Runnable {
            override fun run() {
                try {
                    if (!exoPlayer.isPlaying) {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                    }

                    HyperLog.i(TAG, "Tick!")
                } catch (e: Exception) {
                    HyperLog.e(TAG, "Exception: " + e.message, e)
                }
                // Repeat this the same runnable code block again another 20 seconds
                // 'this' is referencing the Runnable object
                val twentySeconds = 20000
                handlerTicks!!.postDelayed(this, random.nextInt(twentySeconds).toLong())
            }
        }

        runnableRealAlarm = Runnable {
            try {
                // Removes pending code execution
                handlerTicks!!.removeCallbacksAndMessages(predAlarm)

                if (exoPlayer.isPlaying) {
                    exoPlayer.stop()
                    exoPlayer.release()
                }

                initExoPlayer(getMelody(entity), Player.REPEAT_MODE_ONE)
                volumeCounter = defaultVolume
                exoPlayer.volume = volumeCounter

                val twoMinutes = 2 * 60000
                volumeIncreaseStep = (1 - defaultVolume) / (twoMinutes / volumeIncreaseDelayMs)

                exoPlayer.playWhenReady = true

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
                    volumeCounter += volumeIncreaseStep
                    HyperLog.v(TAG, "New alarm volume: $volumeCounter")

                    if (exoPlayer.isPlaying) {
                        exoPlayer.volume = volumeCounter
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (volumeCounter < 1) handlerVolume!!.postDelayed(this, volumeIncreaseDelayMs)
            }
        }

        if (ticksTimeMs > 0) {
            handlerTicks!!.post(predAlarm)
            handlerVolume!!.post(runnableVolume)
        }

        handlerTicks!!.postDelayed(runnableRealAlarm, ticksTimeMs.toLong())
    }

    private fun getMelody(entity: AlarmEntity?): Uri {
        return if (entity!!.melodyUrl != null) {
            Uri.parse(entity.melodyUrl)
        } else {
            RawResourceDataSource.buildRawResourceUri(R.raw.default_alarm_sound)
        }
    }

    private fun stopAlarmNotification() {
        HyperLog.i(TAG, "Stop Alarm notification")
        handlerTicks!!.removeCallbacksAndMessages(null)
        stopAlarmSound()
        cancelVolumeIncreasing()
        alarmEndVibration()
        stopForeground(true)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancelAll()
        ALARM_PLAYING = null
        stopSelf()
        killProcess(myPid())
        EventBus.getDefault().unregister(this)
    }

    private fun stopAlarmSound() {
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
            exoPlayer.release()
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
                .setContentTitle(context.getString(R.string.alarm_ringing))
                .setContentText(entity.message)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.launcher))
                .setSmallIcon(R.drawable.alarm_active)
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

        startForeground(43, alarmNotification.build())
    }

    companion object {
        var ALARM_PLAYING: Int? = null
        private val CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/alarms")
    }
}