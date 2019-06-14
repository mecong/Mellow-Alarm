package com.mecong.myalarm.sleep_assistant;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
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
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.SQLiteDBHelper;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import static com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS;
import static com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS;
import static com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS;
import static com.mecong.myalarm.alarm.AlarmUtils.TAG;

public class RadioService extends Service implements Player.EventListener, AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.mecong.myalarm.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.mecong.myalarm.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.mecong.myalarm.ACTION_STOP";

    private final IBinder iBinder = new LocalBinder();

    private SimpleExoPlayer exoPlayer;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    private boolean onGoingCall = false;
    private TelephonyManager telephonyManager;

    private WifiManager.WifiLock wifiLock;

    private AudioManager audioManager;

    private String status;

    private String streamUrl;
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };
    private float audioVolume;
    private DefaultBandwidthMeter bandwidthMeter;
    private SimpleCache cache;
    private DataSource.Factory cacheDataSourceFactory;
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

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
    private MediaSessionCompat.Callback mediasSessionCallback = new MediaSessionCompat.Callback() {
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

        onGoingCall = false;
        audioVolume = 0.8f;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");

        mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());

        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getResources().getString(R.string.app_name))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "On Air")
                .build());
        mediaSession.setCallback(mediasSessionCallback);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        File cacheFolder = new File(getApplicationContext().getCacheDir(), "tender_media");
        HyperLog.i(TAG, "cacheFolder -- " + cacheFolder);

        LeastRecentlyUsedCacheEvictor cacheEvictor = new LeastRecentlyUsedCacheEvictor(10 * 1024 * 1024);
        cache = new SimpleCache(cacheFolder, cacheEvictor, SQLiteDBHelper.getInstance(getApplicationContext()));
//
        DefaultDataSourceFactory defaultDataSourceFactory =
                new DefaultDataSourceFactory(this, getUserAgent(), bandwidthMeter);

        cacheDataSourceFactory = new CacheDataSourceFactory(cache,
                defaultDataSourceFactory, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);


//        bandwidthMeter = new DefaultBandwidthMeter();
//        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);

        bandwidthMeter = new DefaultBandwidthMeter.Builder(getApplicationContext()).build();

        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(
                DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS * 20,
                DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS * 20,
                DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS * 2,
                0.5f
        );


        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);
        exoPlayer.addListener(this);

        HyperLog.v(TAG, "on create-------: " + exoPlayer);


        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        status = PlaybackStatus.IDLE;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HyperLog.v(TAG, "!!!!!!!!! onStartCommand !!!!!!!!" + intent);

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
        HyperLog.v(TAG, "on unbind: " + intent);
        if (status.equals(PlaybackStatus.IDLE))
            stopSelf();

        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(final Intent intent) {
        HyperLog.i(TAG, "Rebind: " + intent);
    }

    @Override
    public void onDestroy() {
        HyperLog.v(TAG, "on destroy ");

        stop();
        exoPlayer.release();
        exoPlayer.removeListener(this);

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        mediaSession.release();

        unregisterReceiver(becomingNoisyReceiver);

        cache.release();
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

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = PlaybackStatus.LOADING;
                break;
            case Player.STATE_ENDED:
                status = PlaybackStatus.STOPPED;
                break;
            case Player.STATE_IDLE:
                status = PlaybackStatus.IDLE;
                break;
            case Player.STATE_READY:
                status = playWhenReady ? PlaybackStatus.PLAYING : PlaybackStatus.PAUSED;
                break;
            default:
                status = PlaybackStatus.IDLE;
                break;
        }

        EventBus.getDefault().post(status);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        EventBus.getDefault().post(PlaybackStatus.ERROR);
    }

    public void play(String streamUrl) {
        this.streamUrl = streamUrl;
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .setContinueLoadingCheckIntervalBytes(1024 * 1024 * 10)
                .createMediaSource(Uri.parse(streamUrl));

//        ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(cacheDataSourceFactory)
//                .setExtractorsFactory(new DefaultExtractorsFactory())
//                .createMediaSource(Uri.parse(streamUrl));

        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    public void resume() {
        if (streamUrl != null) play(streamUrl);
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

    public void stop() {
        exoPlayer.stop();
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

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
        return this.status.equals(PlaybackStatus.PLAYING);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

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
