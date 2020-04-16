package com.mecong.tenderalarm.sleep_assistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType
import org.greenrobot.eventbus.EventBus

class RadioService : Service(), Player.EventListener, OnAudioFocusChangeListener {
    private val iBinder: IBinder = LocalBinder()
    private lateinit var sleepAssistantPlayList: SleepAssistantPlayList
    private lateinit var notificationManager: MediaNotificationManager
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var transportControls: MediaController.TransportControls
    var onGoingCall = false
    private var telephonyManager: TelephonyManager? = null
    private var wifiLock: WifiLock? = null
    private var audioManager: AudioManager? = null
    var status = IDLE
    private var currentTrackTitle = "Tender Alarm"
    private var streamUrl: String? = null
    private var bandwidthMeter: DefaultBandwidthMeter? = null
    private var dataSourceFactory: DataSource.Factory? = null

    var audioVolume = 0f
        set(value) {
            field = value
            exoPlayer.volume = value
        }

    private var phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            if ((state == TelephonyManager.CALL_STATE_OFFHOOK
                            || state == TelephonyManager.CALL_STATE_RINGING)) {
                if (!isPlaying) return
                onGoingCall = true
                stop()
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (!onGoingCall) return
                onGoingCall = false
                resume()
            }
        }
    }

    private val mediasSessionCallback: MediaSession.Callback = object : MediaSession.Callback() {
        override fun onPause() {
            super.onPause()
            pause()
        }

        override fun onStop() {
            super.onStop()
            stop()
        }

        override fun onPlay() {
            super.onPlay()
            resume()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return iBinder
    }

    override fun onCreate() {
        super.onCreate()
        val loadControl: LoadControl = CustomLoadControl.Builder()
                .setBufferDurationsMs(1000 * 30,
                        1000 * 60 * 5,
                        1000 * 10,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .createDefaultLoadControl()

        exoPlayer = SimpleExoPlayer.Builder(applicationContext)
//                .setLoadControl(loadControl)
                .build()
                .apply {
                    setHandleWakeLock(true)
                    setHandleAudioBecomingNoisy(true)
                    addListener(this@RadioService)

                    addMetadataOutput(MetadataOutput { metadata ->
                        //                ICY: title="Oleg Byonic & Natalia Shapovalova - Breath of Eternity", url="null"
                        HyperLog.v(AlarmUtils.TAG, "----metadata---->")
                        for (i in 0 until metadata.length()) {
                            val message = metadata[i].toString()
                            HyperLog.v(AlarmUtils.TAG, message)
                            if (message.startsWith("ICY: ")) {
                                val titleNotParsed = message.split(",").toTypedArray()[0].split("=").toTypedArray()[1]
                                currentTrackTitle = titleNotParsed.replace("\"".toRegex(), " ").trim { it <= ' ' }
                                if (!currentTrackTitle.isEmpty()) {
                                    val playingMedia = Media("", currentTrackTitle)
                                    EventBus.getDefault().postSticky(playingMedia)
                                }
                                notificationManager.startNotify(status, currentTrackTitle)
                            }
                        }
                        HyperLog.v(AlarmUtils.TAG, "<----metadata----")
                    })
                }
        notificationManager = MediaNotificationManager(this)

        mediaSession = MediaSession(this, javaClass.simpleName).apply {
            isActive = true
            setMetadata(MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "...")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, resources.getString(R.string.app_name))
                    .putString(MediaMetadata.METADATA_KEY_TITLE, resources.getString(R.string.app_name))
                    .build())
            setCallback(mediasSessionCallback)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        transportControls = mediaSession.controller.transportControls

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        bandwidthMeter = DefaultBandwidthMeter.Builder(applicationContext).build()
        dataSourceFactory = DefaultDataSourceFactory(this, userAgent, bandwidthMeter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (TextUtils.isEmpty(action)) return START_NOT_STICKY

        val result =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val mPlaybackAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    val focusRequest: AudioFocusRequest = AudioFocusRequest
                            .Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAcceptsDelayedFocusGain(true)
                            .setAudioAttributes(mPlaybackAttributes)
                            .build()
                    audioManager!!.requestAudioFocus(focusRequest)
                } else {
                    audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                }

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop()
            return START_NOT_STICKY
        }

        when {
            ACTION_PLAY.equals(action, ignoreCase = true) -> {
                transportControls.play()
            }
            ACTION_PAUSE.equals(action, ignoreCase = true) -> {
                transportControls.pause()
            }
            ACTION_STOP.equals(action, ignoreCase = true) -> {
                transportControls.stop()
            }
        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent): Boolean {
        if ((status == IDLE)) stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stop()
        exoPlayer.release()
        exoPlayer.removeListener(this)
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        mediaSession.release()
        notificationManager.cancelNotify()
        super.onDestroy()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        HyperLog.v(AlarmUtils.TAG, "Focus changed: $focusChange")
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            resume()
            exoPlayer.volume = audioVolume
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS && isPlaying) {
            stop()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && isPlaying) {
            pause()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && isPlaying) {
            exoPlayer.volume = 0.1f
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) { //        if (!status.equals(IDLE))
//            notificationManager.startNotify(status);
        status = when (playbackState) {
            Player.STATE_BUFFERING -> LOADING
            Player.STATE_ENDED -> STOPPED
            Player.STATE_READY -> {
                notificationManager.startNotify(status, currentTrackTitle)
                if (playWhenReady) PLAYING else PAUSED
            }
            Player.STATE_IDLE -> IDLE
            else -> IDLE
        }
        EventBus.getDefault().postSticky(status)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        HyperLog.e(AlarmUtils.TAG, "Can't play: $error")
        EventBus.getDefault().postSticky(ERROR)
    }

    fun resume() {
        if (hasPlayList()) play()
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        audioManager!!.abandonAudioFocus(this)
        notificationManager.cancelNotify()
        wifiLockRelease()
    }

    fun stop() {
        exoPlayer.stop()
        audioManager!!.abandonAudioFocus(this)
        notificationManager.cancelNotify()
        wifiLockRelease()
    }

    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager: WifiManager? = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF, "mcScPAmpLock")
            }
        }
        if (!wifiLock!!.isHeld && sleepAssistantPlayList.mediaType === SleepMediaType.ONLINE) {
            wifiLock!!.acquire()
            HyperLog.v(AlarmUtils.TAG, "WiFi lock acquired")
        }
    }

    private fun wifiLockRelease() {
        if (wifiLock != null && wifiLock!!.isHeld) {
            wifiLock!!.release()
            HyperLog.v(AlarmUtils.TAG, "WiFi lock released")
        }
    }

    fun play() {
        exoPlayer.playWhenReady = true
    }

    fun play(streamUrl: String?) {
        this.streamUrl = streamUrl
        acquireWifiLock()
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(streamUrl))
        exoPlayer.prepare(mediaSource)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.playWhenReady = true
    }

    fun setMediaList(sleepAssistantPlayList: SleepAssistantPlayList) {
        this.sleepAssistantPlayList = sleepAssistantPlayList
        val concatenatingMediaSource = ConcatenatingMediaSource()
        for (media: Media in this.sleepAssistantPlayList.media) {
            concatenatingMediaSource.addMediaSource(
                    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(media.url)))
        }

        exoPlayer.prepare(concatenatingMediaSource)

        when (this.sleepAssistantPlayList.mediaType) {
            SleepMediaType.LOCAL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            SleepMediaType.ONLINE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
            SleepMediaType.NOISE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
        acquireWifiLock()
        exoPlayer.seekTo(sleepAssistantPlayList.index, 0)
    }

    @Deprecated("")
    fun playOrPause(url: String) {
        if (streamUrl != null && (streamUrl == url)) {
            if (!isPlaying) {
                play(streamUrl)
            } else {
                pause()
            }
        } else {
            if (isPlaying) {
                pause()
            }
            play(url)
        }
    }

    private val userAgent: String
        get() = Util.getUserAgent(this, javaClass.simpleName)

    fun hasPlayList(): Boolean {
        return sleepAssistantPlayList != null && sleepAssistantPlayList.media.isNotEmpty()
    }

    val isPlaying: Boolean
        get() = (status == PLAYING)

    /*
    2019-08-18 21:18:05.102 I/A.L.A.R.M.A: >>>>>>onTracksChanged>>>>>>
2019-08-18 21:18:05.107 I/A.L.A.R.M.A: TPE2: value=Various Artists
2019-08-18 21:18:05.112 I/A.L.A.R.M.A: TIT2: value=Aman Aman
2019-08-18 21:18:05.116 I/A.L.A.R.M.A: TALB: value=Buddha-Bar XVI
2019-08-18 21:18:05.121 I/A.L.A.R.M.A: TYER: value=2014
2019-08-18 21:18:05.125 I/A.L.A.R.M.A: COMM: language=rus, description=
2019-08-18 21:18:05.129 I/A.L.A.R.M.A: TCON: value=Chillout, Lounge, Downtempo
2019-08-18 21:18:05.133 I/A.L.A.R.M.A: TRCK: value=07/16
2019-08-18 21:18:05.137 I/A.L.A.R.M.A: TPOS: value=1/2
2019-08-18 21:18:05.141 I/A.L.A.R.M.A: TENC: value=Dead Angel
2019-08-18 21:18:05.145 I/A.L.A.R.M.A: APIC: mimeType=image/jpeg, description=
2019-08-18 21:18:05.149 I/A.L.A.R.M.A: POPM
2019-08-18 21:18:05.152 I/A.L.A.R.M.A: PRIV: owner=PeakValue
2019-08-18 21:18:05.155 I/A.L.A.R.M.A: PRIV: owner=AverageLevel
2019-08-18 21:18:05.159 I/A.L.A.R.M.A: TPE1: value=Cambis & Florzinho


2019-08-18 21:27:18.795 I/A.L.A.R.M.A: 03. Ambray - Who We Are.mp3

2019-08-18 21:27:18.799 I/A.L.A.R.M.A: >>>>>>onTracksChanged>>>>>>
2019-08-18 21:27:18.801 I/A.L.A.R.M.A: TPE2: value=Various Artists
2019-08-18 21:27:18.804 I/A.L.A.R.M.A: TIT2: value=Who We Are
2019-08-18 21:27:18.807 I/A.L.A.R.M.A: TALB: value=Buddha-Bar XVI
2019-08-18 21:27:18.810 I/A.L.A.R.M.A: TYER: value=2014
2019-08-18 21:27:18.812 I/A.L.A.R.M.A: COMM: language=rus, description=
2019-08-18 21:27:18.815 I/A.L.A.R.M.A: TCON: value=Chillout, Lounge, Downtempo
2019-08-18 21:27:18.817 I/A.L.A.R.M.A: TRCK: value=03/16
2019-08-18 21:27:18.820 I/A.L.A.R.M.A: TPOS: value=1/2
2019-08-18 21:27:18.822 I/A.L.A.R.M.A: TENC: value=Dead Angel
2019-08-18 21:27:18.824 I/A.L.A.R.M.A: APIC: mimeType=image/jpeg, description=
2019-08-18 21:27:18.826 I/A.L.A.R.M.A: POPM
2019-08-18 21:27:18.828 I/A.L.A.R.M.A: PRIV: owner=PeakValue
2019-08-18 21:27:18.830 I/A.L.A.R.M.A: PRIV: owner=AverageLevel
2019-08-18 21:27:18.832 I/A.L.A.R.M.A: TPE1: value=Ambray

     */
    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        HyperLog.v(AlarmUtils.TAG, ">>>>>>onTracksChanged>>>>>>")
        //        IcyHeaders: name="Enigmatic robot", genre="music", bitrate=256000, metadataInterval=16000
        for (i in 0 until trackGroups.length) {
            val trackGroup = trackGroups[i]
            for (j in 0 until trackGroup.length) {
                val trackMetadata = trackGroup.getFormat(j).metadata
                if (trackMetadata != null) {
                    for (k in 0 until trackMetadata.length()) {
                        HyperLog.v(AlarmUtils.TAG, trackMetadata[k].toString())
                    }
                } else {
                    HyperLog.v(AlarmUtils.TAG, "|||Metadata not found|||")
                }
            }
        }
    }

    /*
    Reasons for position discontinuities. One of DISCONTINUITY_REASON_PERIOD_TRANSITION,
    DISCONTINUITY_REASON_SEEK, DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
    DISCONTINUITY_REASON_AD_INSERTION or DISCONTINUITY_REASON_INTERNAL.
     */
    override fun onPositionDiscontinuity(reason: Int) {
        HyperLog.v(AlarmUtils.TAG, "Playing new media > reason: $reason")
        if (hasPlayList()) {
            sleepAssistantPlayList.index = exoPlayer.currentWindowIndex
            HyperLog.v(AlarmUtils.TAG, sleepAssistantPlayList.media[exoPlayer.currentWindowIndex].title)
            val playingMedia = sleepAssistantPlayList.media[exoPlayer.currentWindowIndex]
            EventBus.getDefault().postSticky(playingMedia)
            EventBus.getDefault().post(
                    SleepAssistantPlayList(sleepAssistantPlayList.index, sleepAssistantPlayList.media, sleepAssistantPlayList.mediaType, sleepAssistantPlayList.playListId))

            currentTrackTitle = playingMedia.title!!
            notificationManager.startNotify(status, currentTrackTitle)
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
    override fun onSeekProcessed() {}
    internal inner class LocalBinder : Binder() {
        val service: RadioService
            get() = this@RadioService
    }

    companion object {
        const val ACTION_PLAY = "com.mecong.myalarm.ACTION_PLAY"
        const val ACTION_PAUSE = "com.mecong.myalarm.ACTION_PAUSE"
        const val ACTION_STOP = "com.mecong.myalarm.ACTION_STOP"

        const val IDLE = "PlaybackStatus_IDLE"
        const val LOADING = "PlaybackStatus_LOADING"
        const val PLAYING = "PlaybackStatus_PLAYING"
        const val PAUSED = "PlaybackStatus_PAUSED"
        const val STOPPED = "PlaybackStatus_STOPPED"
        const val ERROR = "PlaybackStatus_ERROR"
    }
}