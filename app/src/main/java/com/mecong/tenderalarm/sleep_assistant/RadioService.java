package com.mecong.tenderalarm.sleep_assistant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType;

import org.greenrobot.eventbus.EventBus;

import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;
import static com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType.ONLINE;
import static timber.log.Timber.i;

public class RadioService extends Service implements Player.EventListener, AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.mecong.myalarm.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.mecong.myalarm.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.mecong.myalarm.ACTION_STOP";
    public static final String IDLE = "PlaybackStatus_IDLE";
    public static final String LOADING = "PlaybackStatus_LOADING";
    public static final String PLAYING = "PlaybackStatus_PLAYING";
    public static final String PAUSED = "PlaybackStatus_PAUSED";
    public static final String STOPPED = "PlaybackStatus_STOPPED";
    public static final String ERROR = "PlaybackStatus_ERROR";

    final IBinder iBinder = new LocalBinder();
    SleepAssistantPlayListModel.PlayList playList;
    MediaNotificationManager notificationManager;
    SimpleExoPlayer exoPlayer;
    MediaSession mediaSession;
    MediaController.TransportControls transportControls;
    boolean onGoingCall = false;
    TelephonyManager telephonyManager;
    WifiManager.WifiLock wifiLock;
    AudioManager audioManager;
    String status;
    String currentTrackTitle = "Tender Alarm";
    String streamUrl;
    float audioVolume;
    DefaultBandwidthMeter bandwidthMeter;
    DataSource.Factory dataSourceFactory;
    PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK
                    || state == TelephonyManager.CALL_STATE_RINGING) {
                if (!isPlaying()) return;
                onGoingCall = true;
                stop();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (!onGoingCall) return;
                onGoingCall = false;
                resume();
            }
        }
    };


    private MediaSession.Callback mediasSessionCallback = new MediaSession.Callback() {
        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            resume();
        }
    };

    public void setAudioVolume(float audioVolume) {
        this.audioVolume = audioVolume;
        i("current position %d, buffered position %d, dff: %d",
                exoPlayer.getCurrentPosition(),
                exoPlayer.getBufferedPosition(),
                exoPlayer.getBufferedPosition() - exoPlayer.getCurrentPosition());
        exoPlayer.setVolume(audioVolume);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = new MediaNotificationManager(this);
        onGoingCall = false;
        audioVolume = 0.8f;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        mediaSession = new MediaSession(this, getClass().getSimpleName());

        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, getResources().getString(R.string.app_name))
                .putString(MediaMetadata.METADATA_KEY_TITLE, getResources().getString(R.string.app_name))
                .build());
        mediaSession.setCallback(mediasSessionCallback);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        bandwidthMeter = new DefaultBandwidthMeter.Builder(getApplicationContext()).build();

        dataSourceFactory = new DefaultDataSourceFactory(this, getUserAgent(), bandwidthMeter);

        LoadControl loadControl = new CustomLoadControl.Builder()
                .setBufferDurationsMs(1000 * 30,
                        1000 * 60 * 5,
                        1000 * 3,
                        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .createDefaultLoadControl();


        exoPlayer = new SimpleExoPlayer.Builder(getApplicationContext())
                .setLoadControl(loadControl)
                .build();
        exoPlayer.setHandleWakeLock(true);
        exoPlayer.setHandleAudioBecomingNoisy(true);

        exoPlayer.addListener(this);
        exoPlayer.addMetadataOutput(new MetadataOutput() {
            @Override
            public void onMetadata(@NonNull Metadata metadata) {
//                ICY: title="Oleg Byonic & Natalia Shapovalova - Breath of Eternity", url="null"
                HyperLog.v(TAG, "----metadata---->");
                for (int i = 0; i < metadata.length(); i++) {
                    String message = metadata.get(i).toString();
                    HyperLog.v(TAG, message);
                    if (message.startsWith("ICY: ")) {
                        String titleNotParsed = message.split(",")[0].split("=")[1];
                        currentTrackTitle = titleNotParsed.replaceAll("\"", " ").trim();

                        if (!currentTrackTitle.isEmpty()) {
                            SleepAssistantPlayListModel.Media playingMedia = new SleepAssistantPlayListModel.Media("", currentTrackTitle);
                            EventBus.getDefault().postSticky(playingMedia);
                        }

                        notificationManager.startNotify(status, currentTrackTitle);
                    }
                }
                HyperLog.v(TAG, "<----metadata----");
            }
        });

        status = IDLE;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        if (TextUtils.isEmpty(action))
            return START_NOT_STICKY;

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop();
            return START_NOT_STICKY;
        }

        if (ACTION_PLAY.equalsIgnoreCase(action)) {
            transportControls.play();
        } else if (ACTION_PAUSE.equalsIgnoreCase(action)) {
            transportControls.pause();
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            transportControls.stop();
        }

        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (status.equals(IDLE))
            stopSelf();

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stop();
        exoPlayer.release();
        exoPlayer.removeListener(this);

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        mediaSession.release();

        notificationManager.cancelNotify();
        super.onDestroy();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        HyperLog.v(TAG, "Focus changed: " + focusChange);

        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            resume();
            exoPlayer.setVolume(audioVolume);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS && isPlaying()) {
            stop();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT && isPlaying()) {
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && isPlaying()) {
            exoPlayer.setVolume(0.1f);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

//        if (!status.equals(IDLE))
//            notificationManager.startNotify(status);

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = LOADING;
                break;
            case Player.STATE_ENDED:
                status = STOPPED;
                break;
            case Player.STATE_READY:
                notificationManager.startNotify(status, currentTrackTitle);

                status = playWhenReady ? PLAYING : PAUSED;
                break;
            case Player.STATE_IDLE:
            default:
                status = IDLE;
                break;
        }

        EventBus.getDefault().postSticky(status);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        HyperLog.e(TAG, "Can't play: " + error);
        EventBus.getDefault().postSticky(ERROR);
    }

    public void play() {
        exoPlayer.setPlayWhenReady(true);
    }

    public void resume() {
        if (hasPlayList()) play();
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(this);
        notificationManager.cancelNotify();
        wifiLockRelease();
    }

    public void stop() {
        exoPlayer.stop();
        audioManager.abandonAudioFocus(this);
        notificationManager.cancelNotify();
        wifiLockRelease();
    }

    private void acquireWifiLock() {
        if (wifiLock == null) {
            final WifiManager wifiManager =
                    (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF, "mcScPAmpLock");
            }
        }

        if (!wifiLock.isHeld() && this.playList.getMediaType() == ONLINE) {
            wifiLock.acquire();
            HyperLog.v(TAG, "WiFi lock acquired");
        }
    }

    private void wifiLockRelease() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            HyperLog.v(TAG, "WiFi lock released");
        }
    }

    public void play(String streamUrl) {
        this.streamUrl = streamUrl;
        acquireWifiLock();

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(streamUrl));

        exoPlayer.prepare(mediaSource);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.setPlayWhenReady(true);
    }

    public void playMediaList(SleepAssistantPlayListModel.PlayList playList) {
        this.playList = playList;

        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
        for (SleepAssistantPlayListModel.Media media : this.playList.getMedia()) {
            concatenatingMediaSource.addMediaSource(
                    new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(media.getUrl())));
        }
        exoPlayer.prepare(concatenatingMediaSource);
        if (this.playList.getMediaType() == SleepMediaType.LOCAL) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        } else if (this.playList.getMediaType() == ONLINE) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        } else if (this.playList.getMediaType() == SleepMediaType.NOISE) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }

        acquireWifiLock();

        exoPlayer.seekTo(playList.getIndex(), 0);
        exoPlayer.setPlayWhenReady(true);
    }


    @Deprecated
    public void playOrPause(String url) {

        if (streamUrl != null && streamUrl.equals(url)) {
            if (!isPlaying()) {
                play(streamUrl);
            } else {
                pause();
            }
        } else {
            if (isPlaying()) {
                pause();
            }

            play(url);
        }
    }

    public String getStatus() {
        return status;
    }

    private String getUserAgent() {
        return Util.getUserAgent(this, getClass().getSimpleName());
    }

    public boolean isPlaying() {
        return this.status.equals(PLAYING);
    }


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
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        HyperLog.v(TAG, ">>>>>>onTracksChanged>>>>>>");
//        IcyHeaders: name="Enigmatic robot", genre="music", bitrate=256000, metadataInterval=16000

        for (int i = 0; i < trackGroups.length; i++) {
            TrackGroup trackGroup = trackGroups.get(i);
            for (int j = 0; j < trackGroup.length; j++) {
                Metadata trackMetadata = trackGroup.getFormat(j).metadata;
                if (trackMetadata != null) {
                    for (int k = 0; k < trackMetadata.length(); k++) {
                        HyperLog.v(TAG, trackMetadata.get(k).toString());
                    }
                } else {
                    HyperLog.v(TAG, "|||Metadata not found|||");
                }
            }
        }
    }

    public boolean hasPlayList() {
        return this.playList != null && !this.playList.getMedia().isEmpty();
    }


    /*
    Reasons for position discontinuities. One of DISCONTINUITY_REASON_PERIOD_TRANSITION,
    DISCONTINUITY_REASON_SEEK, DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
    DISCONTINUITY_REASON_AD_INSERTION or DISCONTINUITY_REASON_INTERNAL.
     */
    @Override
    public void onPositionDiscontinuity(int reason) {
        HyperLog.v(TAG, "Playing new media > reason: " + reason);
        if (hasPlayList()) {
            HyperLog.v(TAG, this.playList.getMedia().get(exoPlayer.getCurrentWindowIndex()).getTitle());
            SleepAssistantPlayListModel.Media playingMedia = this.playList.getMedia().get(exoPlayer.getCurrentWindowIndex());
            EventBus.getDefault().postSticky(playingMedia);
            currentTrackTitle = playingMedia.getTitle();
            notificationManager.startNotify(status, currentTrackTitle);
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    class LocalBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }
}
