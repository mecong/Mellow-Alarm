package com.mecong.myalarm.sleep_assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mecong.myalarm.R;
import com.mecong.myalarm.alarm.AlarmUtils;
import com.mecong.myalarm.sleep_assistant.media_selection.SleepMediaType;
import com.mecong.myalarm.sleep_assistant.media_selection.SleepNoise;
import com.mecong.myalarm.sleep_assistant.media_selection.SoundListsPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


@FieldDefaults(level = AccessLevel.PUBLIC)
public class SleepAssistantActivity extends AppCompatActivity {

    public static final long STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static RadioService service;

    @BindView(R.id.play)
    Button playButton;

    @BindView(R.id.textViewTime)
    TextView textViewTime;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tabs)
    TabLayout tabs;

    @BindView(R.id.textViewMinutes)
    TextView textViewMinutes;

    @BindView(R.id.textViewVolumePercent)
    TextView textViewVolumePercent;

    @BindView(R.id.slliderSleepTime)
    HourglassComponent sliderSleepTime;

    @BindView(R.id.sliderVolume)
    HourglassComponent sliderVolume;


    int loadingCount = 0;
    long timeMinutes;
    long timeMs;
    float volume;
    Handler handler;
    Runnable runnable;
    float volumeStep;
    PowerManager.WakeLock wakeLock;
    boolean serviceBound;
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            service = ((RadioService.LocalBinder) binder).getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    SleepAssistantViewModel model;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(this).get(SleepAssistantViewModel.class);
        if (model.getPlaylist().getValue() == null) {
            SleepAssistantViewModel.PlayList defaultPlayList =
                    new SleepAssistantViewModel.PlayList(SleepNoise.Companion.retrieveNoises().get(0).getUrl(), SleepMediaType.NOISE);
            model.setPlaylist(defaultPlayList);
        }

        model.getPlaylist().observe(this, new Observer<SleepAssistantViewModel.PlayList>() {
            @Override
            public void onChanged(SleepAssistantViewModel.PlayList playList) {
                service.playMediaList(playList);
            }
        });

        setContentView(R.layout.activity_sleep_assistant);
        ButterKnife.bind(this);
        final Context context = getApplicationContext();

        SoundListsPagerAdapter soundListsPagerAdapter =
                new SoundListsPagerAdapter(getSupportFragmentManager(), context);
        viewPager.setAdapter(soundListsPagerAdapter);

        viewPager.setCurrentItem(2);
        tabs.setupWithViewPager(viewPager);

        timeMinutes = 30;

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        float volumeCoefficient = (float) systemVolume / streamMaxVolume;

        if (volumeCoefficient < 0.3f) {
            volumeCoefficient = 0.3f;
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC, (int) (streamMaxVolume * volumeCoefficient), 0);
            Toast.makeText(context, "System volume set to 30%", Toast.LENGTH_SHORT).show();
        }
        volume = 120 - 100 * volumeCoefficient;


        sliderSleepTime.addListener(new HourglassComponent.SleepTimerViewValueListener() {
            @Override
            public void onValueChanged(long newValue) {
                textViewMinutes.setText(context.getString(R.string.sleep_minutes, newValue));
                timeMinutes = newValue;
                timeMs = TimeUnit.MINUTES.toMillis(timeMinutes);
                volumeStep = volume * STEP_MILLIS / timeMs;
            }
        });

        sliderVolume.addListener(new HourglassComponent.SleepTimerViewValueListener() {
            @Override
            public void onValueChanged(long newValue) {
                textViewVolumePercent.setText(context.getString(R.string.volume_percent, newValue));
                volume = newValue;
                service.setAudioVolume(volume / 100f);
                volumeStep = volume * STEP_MILLIS / timeMs;
            }
        });

        sliderSleepTime.setCurrentValue(timeMinutes);
        sliderVolume.setCurrentValue((long) volume);
        timeMs = TimeUnit.MINUTES.toMillis(timeMinutes);
        volumeStep = volume * STEP_MILLIS / timeMs;

        textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));
        textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));


        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                timeMs -= STEP_MILLIS;
                if (timeMs <= 0) {
                    handler.removeCallbacks(runnable);
                    releaseWakeLock();

                    if (service.isPlaying()) {
                        service.stop();
                    }
                } else {
                    timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
                    sliderSleepTime.setCurrentValue(timeMinutes);

                    volume -= volumeStep;
                    Log.v(AlarmUtils.TAG, "timems=" + timeMs + " volume=" + volume / 100f);
                    service.setAudioVolume(volume / 100f);
                    sliderVolume.setCurrentValue((long) volume);

                    textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));
                    textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

                    handler.postDelayed(this, STEP_MILLIS);
                }
            }
        };
    }

    @OnClick(R.id.play)
    public void onPlayPauseButtonsClicked() {
        if (service.isPlaying()) {
            service.pause();
        } else {
            service.resume();
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "TenderAlarm::SleepAssistantWakelockTag");
            wakeLock.acquire(timeMs);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Subscribe
    public void onEvent(String status) {

        playButton.setEnabled(true);
        switch (status) {
            case RadioService.LOADING:
                playButton.setText("Loading");
                loadingCount++;
                textViewTime.setText(String.valueOf(loadingCount));
                playButton.setEnabled(false);
                break;
            case RadioService.ERROR:
                Toast.makeText(this, "Can not stream", Toast.LENGTH_SHORT).show();
                playButton.setText("Resume");
                model.playing.setValue(false);
                releaseWakeLock();
                handler.removeCallbacks(runnable);
                break;
            case RadioService.PLAYING:
                playButton.setText("Pause");
                model.playing.setValue(true);
                handler.postDelayed(runnable, STEP_MILLIS);
                acquireWakeLock();
                break;
            default:
                playButton.setText("Play");
                model.playing.setValue(false);
                releaseWakeLock();
                handler.removeCallbacks(runnable);
                break;
        }
//
//        trigger.setImageResource(status.equals(PlaybackStatus.PLAYING)
//                ? R.drawable.ic_pause_black
//                : R.drawable.ic_play_arrow_black);

    }

    @Override
    public void onStart() {
        super.onStart();
        bind();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(runnable);
        releaseWakeLock();

        if (service.isPlaying()) {
            service.stop();
        }

        if (serviceBound) {
            getApplication().unbindService(serviceConnection);
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void bind() {
        Intent intent = new Intent(getApplicationContext(), RadioService.class);
        getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (service != null)
            EventBus.getDefault().post(service.getStatus());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
