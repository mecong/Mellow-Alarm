package com.mecong.myalarm.sleep_assistant;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.R;
import com.mecong.myalarm.sleep_assistant.media_selection.SleepMediaType;

import org.greenrobot.eventbus.EventBus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import static com.mecong.myalarm.alarm.AlarmUtils.TAG;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
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
    SleepAssistantViewModel.PlayList playList;
    MediaNotificationManager notificationManager;
    SimpleExoPlayer exoPlayer;
    MediaSession mediaSession;
    MediaController.TransportControls transportControls;
    boolean onGoingCall = false;
    TelephonyManager telephonyManager;
    WifiManager.WifiLock wifiLock;
    AudioManager audioManager;
    String status;
    String streamUrl;
    BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };
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

        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");

        mediaSession = new MediaSession(this, getClass().getSimpleName());

        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, getResources().getString(R.string.app_name))
                .putString(MediaMetadata.METADATA_KEY_TITLE, "On Air")
                .build());
        mediaSession.setCallback(mediasSessionCallback);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        bandwidthMeter = new DefaultBandwidthMeter.Builder(getApplicationContext()).build();

        dataSourceFactory = new DefaultDataSourceFactory(this, getUserAgent(), bandwidthMeter);


        exoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext());
        exoPlayer.addListener(this);
        exoPlayer.addMetadataOutput(new MetadataOutput() {
            @Override
            public void onMetadata(Metadata metadata) {
                HyperLog.i(TAG, "----metadata---->");
                for (int i = 0; i < metadata.length(); i++) {
                    HyperLog.i(TAG, metadata.get(i).toString());
                    HyperLog.i(TAG, "<----metadata----");
                }
            }
        });

        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

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

        unregisterReceiver(becomingNoisyReceiver);

        notificationManager.cancelNotify();
        super.onDestroy();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        HyperLog.i(TAG, "Focus changed: " + focusChange);

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(audioVolume);
                resume();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying()) exoPlayer.setVolume(0.1f);
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        if (!status.equals(IDLE))
            notificationManager.startNotify(status);

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = LOADING;
                break;
            case Player.STATE_ENDED:
                status = STOPPED;
                break;
            case Player.STATE_IDLE:
                status = IDLE;
                break;
            case Player.STATE_READY:
                status = playWhenReady ? PLAYING : PAUSED;
                break;
            default:
                status = IDLE;
                break;
        }

        EventBus.getDefault().post(status);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        EventBus.getDefault().post(ERROR);
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

    public void play(String streamUrl) {
        this.streamUrl = streamUrl;
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(streamUrl));

        exoPlayer.prepare(mediaSource);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        exoPlayer.setPlayWhenReady(true);
    }

    public void playMediaList(SleepAssistantViewModel.PlayList playList) {
        this.playList = playList;

        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
        for (String uri : this.playList.getUrls()) {
            concatenatingMediaSource.addMediaSource(
                    new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uri)));
        }
        exoPlayer.prepare(concatenatingMediaSource);
        if (this.playList.getMediaType() == SleepMediaType.LOCAL) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        } else if (this.playList.getMediaType() == SleepMediaType.ONLINE) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        } else if (this.playList.getMediaType() == SleepMediaType.NOISE) {
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
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

    private void wifiLockRelease() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private String getUserAgent() {
        return Util.getUserAgent(this, getClass().getSimpleName());
    }

    public boolean isPlaying() {
        return this.status.equals(PLAYING);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray
            trackSelections) {
        HyperLog.i(TAG, ">>>>>>onTracksChanged>>>>>>");
        for (int i = 0; i < trackGroups.length; i++) {
            for (int j = 0; j < trackGroups.get(i).length; j++) {

                if (trackGroups.get(i).getFormat(j).metadata != null) {
                    for (int k = 0; k < trackGroups.get(i).getFormat(j).metadata.length(); k++) {
                        HyperLog.i(TAG, trackGroups.get(i).getFormat(j).metadata.get(k).toString());
                    }
                }
            }
        }

    }

    public boolean hasPlayList() {
        return this.playList != null && !this.playList.getUrls().isEmpty();
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        HyperLog.i(TAG, "Playing new media > reason: " + reason);
        if (hasPlayList()) {
            HyperLog.i(TAG, this.playList.getUrls().get(exoPlayer.getCurrentWindowIndex()));
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
