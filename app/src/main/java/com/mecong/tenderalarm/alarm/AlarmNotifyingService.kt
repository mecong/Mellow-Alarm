package com.mecong.tenderalarm.alarm

import android.app.AlarmManager
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
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.mecong.tenderalarm.BuildConfig
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM
import com.mecong.tenderalarm.alarm.AlarmUtils.ALARM_ID_PARAM_SAME_ID
import com.mecong.tenderalarm.model.AlarmEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AlarmNotifyingService : Service(), Player.EventListener {
    private lateinit var runnableVolume: Runnable
    private lateinit var runnableRealAlarm: Runnable
    private lateinit var runnableVibro: Runnable

    private var random = Random()
    private var handlerVolume: Handler = Handler()
    private var handlerTicks: Handler = Handler()
    private var handlerVibration: Handler = Handler()
    private lateinit var exoPlayer: SimpleExoPlayer

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        initExoPlayer(RawResourceDataSource.buildRawResourceUri(R.raw.dance_of_kaschey), Player.REPEAT_MODE_ONE)
        exoPlayer.playWhenReady = true
    }

    private fun initExoPlayer(streamUrl: Uri, repeatMode: Int) {
        exoPlayer = SimpleExoPlayer.Builder(applicationContext)
                .build()
                .apply {
                    setWakeMode(C.WAKE_MODE_LOCAL)
                    setHandleAudioBecomingNoisy(true)
                    addListener(this@AlarmNotifyingService)
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
        val action = intent.action
        if (action.equals(ACTION_STOP)) {
            stopAlarmNotification()
        } else {
            val alarmId = intent.getStringExtra(ALARM_ID_PARAM)
            val sameId = intent.getBooleanExtra(ALARM_ID_PARAM_SAME_ID, false)
            val entity = sqLiteDBHelper(this)!!.getAlarmById(alarmId)
            Timber.i("Alarm notification service start: $entity")
            usePowerManagerWakeup()

            if (sameId) {
                val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn = pm.isInteractive
                Timber.i("SameId received; Notification activity shown: ${AlarmReceiverActivity.IS_SHOWN}; Screen on: $isScreenOn")
                if (!AlarmReceiverActivity.IS_SHOWN || !isScreenOn) {
                    startAlarmNotification(this, entity)
                }
                startSnoozedSound(entity)
            } else {
                startSound(entity)
                startAlarmNotification(this, entity)
            }
        }
        return START_NOT_STICKY
    }

    private fun usePowerManagerWakeup() {
        Timber.v("Alarm Notifying service usePowerManagerWakeup")
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
        Timber.d("Snooze for $minutes min")
        handlerTicks.removeCallbacksAndMessages(null)
        cancelVolumeIncreasing()

        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
            exoPlayer.seekTo(0)
        }
    }

    private fun cancelVolumeIncreasing() {
        handlerVolume.removeCallbacksAndMessages(null)
        handlerVibration.removeCallbacksAndMessages(null)
    }

    private fun startSnoozedSound(entity: AlarmEntity?) {
        handlerTicks.post(runnableRealAlarm)
    }

    private fun startSound(entity: AlarmEntity?) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamMaxVolume, 0)

        val defaultVolume = 0.1f
        val volumeIncreaseDelayMs = 2000L
        val ticksTimeMs = entity!!.ticksTime * 60 * 1000
        val amountIncreases = ticksTimeMs / volumeIncreaseDelayMs
        var volumeIncreaseStep = if (amountIncreases.compareTo(0) == 0) 0.01f else (0.7f - defaultVolume) / amountIncreases

        val tickSound = if (entity.ticksType == 0) R.raw.tick else R.raw.ding1

        var volumeCounter = defaultVolume
        try {
            initExoPlayer(RawResourceDataSource.buildRawResourceUri(tickSound), Player.REPEAT_MODE_OFF)
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

                    Timber.v("Tick!")
                } catch (e: Exception) {
                    //HyperLog.e(TAG, "Exception: " + e.message, e)
                }
                // Repeat this the same runnable code block again another 20 seconds
                // 'this' is referencing the Runnable object
                val twentySeconds = 20000
                handlerTicks.postDelayed(this, random.nextInt(twentySeconds).toLong())
            }
        }

        runnableRealAlarm = Runnable {
            try {
                // Removes pending code execution
                handlerTicks.removeCallbacksAndMessages(predAlarm)

                if (exoPlayer.isPlaying) {
                    exoPlayer.stop()
                    exoPlayer.release()
                }

                initExoPlayer(getMelody(entity), Player.REPEAT_MODE_ONE)

                exoPlayer.volume = if (entity.increaseVolume > 0) {
                    val increaseVolumeMinutes = entity.increaseVolume * 60000
                    volumeIncreaseStep = (1 - defaultVolume) / (increaseVolumeMinutes / volumeIncreaseDelayMs)
                    handlerVolume.removeCallbacks(runnableVolume)
                    handlerVolume.post(runnableVolume)
                    volumeCounter = defaultVolume
                    volumeCounter
                } else {
                    1f
                }

                exoPlayer.playWhenReady = true

                if (entity.vibrationType != null) {
                    handlerVibration.post(runnableVibro)
                }

                Timber.i("Real alarm started!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        runnableVolume = object : Runnable {
            override fun run() {
                try {
                    volumeCounter += volumeIncreaseStep
                    Timber.v("New alarm volume: $volumeCounter")

                    if (exoPlayer.isPlaying) {
                        exoPlayer.volume = volumeCounter
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (volumeCounter < 1) handlerVolume.postDelayed(this, volumeIncreaseDelayMs)
            }
        }

        if (ticksTimeMs > 0) {
            handlerTicks.post(predAlarm)
            handlerVolume.post(runnableVolume)
        }

        handlerTicks.postDelayed(runnableRealAlarm, ticksTimeMs.toLong())

        runnableVibro = object : Runnable {
            override fun run() {
                alarmNotificationVibration(-1)
                handlerVibration.postDelayed(this, 10000)
            }
        }
    }

    private fun getMelody(entity: AlarmEntity?): Uri {
        return if (entity!!.melodyUrl != null) {
            Uri.parse(entity.melodyUrl)
        } else {
            RawResourceDataSource.buildRawResourceUri(R.raw.dance_of_kaschey)
        }
    }

    private fun stopAlarmNotification() {
        Timber.i("Stop Alarm notification")
        if (ALARM_PLAYING != null) {
            val entity = sqLiteDBHelper(this)!!.getAlarmById(ALARM_PLAYING)
            if (entity != null) {
                val alarmMgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                AlarmUtils.turnOffSnoozeAlarm(entity, alarmMgr, this)
            }
        }

        handlerTicks.removeCallbacksAndMessages(null)
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

    private fun alarmNotificationVibration(repeat: Int) {
        val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val mVibratePattern = longArrayOf(400, 400, 1000, 600, 600, 800, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(mVibratePattern, repeat))
        } else {
            vibrator.vibrate(mVibratePattern, repeat)
        }
    }

    private fun startAlarmNotification(context: Context, entity: AlarmEntity?) {
        val alarmId = entity!!.id.toString()

        val stopIntent = Intent(context, AlarmNotifyingService::class.java)
        stopIntent.action = ACTION_STOP
        val stopAction = PendingIntent.getService(context, 3, stopIntent, 0)

        Timber.i("Start alarm notification: %s", entity)

        val startAlarmIntent = Intent(context, AlarmReceiverActivity::class.java)
                .setData(ContentUris.withAppendedId(CONTENT_URI, alarmId.toLong()))
        startAlarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startAlarmIntent.putExtra(ALARM_ID_PARAM, alarmId)

        val pendingIntent = PendingIntent.getActivity(context, entity.fullScreenIntentCode,
                startAlarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val alarmNotification = NotificationCompat.Builder(context, MainActivity.ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.cat_sleep)
                .setContentTitle(context.getString(R.string.alarm_ringing))
                .setContentText(entity.message)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.cat_banjo))
                .setSmallIcon(R.drawable.alarm_active)
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(0)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.stop), stopAction)
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
        const val ACTION_STOP = "com.mecong.myalarm.ACTION_STOP"
        var ALARM_PLAYING: String? = null
        private val CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/alarms")
    }
}