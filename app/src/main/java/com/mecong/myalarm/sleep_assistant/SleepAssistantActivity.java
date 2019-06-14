package com.mecong.myalarm.sleep_assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mecong.myalarm.R;
import com.mecong.myalarm.alarm.AlarmUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SleepAssistantActivity extends AppCompatActivity implements NoisesFragment.OnFragmentInteractionListener {

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

    int loadingCount = 0;
    private List<Noises> noises;
    private long timeMinutes;
    private long timeMs;
    private String stream;
    private float volume;
    private Handler handler;
    private Runnable runnable;
    private float volumeStep;
    private PowerManager.WakeLock wakeLock;
    private boolean serviceBound;
    private SoundListsPagerAdapter soundListsPagerAdapter;
    private ServiceConnection serviceConnection = new ServiceConnection() {
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_assistant);
        ButterKnife.bind(this);

        tabs.setupWithViewPager(viewPager);
        soundListsPagerAdapter = new SoundListsPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        viewPager.setAdapter(soundListsPagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                HyperLog.i(AlarmUtils.TAG, "page1");
            }

            @Override
            public void onPageSelected(int position) {
//                HyperLog.i(AlarmUtils.TAG, "page2");
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//                HyperLog.i(AlarmUtils.TAG, "page3");
            }
        });


        timeMinutes = 30;
        timeMs = TimeUnit.MINUTES.toMillis(timeMinutes);
        volume = 50;

        final Context context = getApplicationContext();

        final TextView textViewMinutes = findViewById(R.id.textViewMinutes);
        final SleepTimerView sliderSleepTime = findViewById(R.id.slliderSleepTime);
        sliderSleepTime.addListener(new SleepTimerView.SleepTimerViewValueListener() {
            @Override
            public void onValueChanged(long newValue) {
                textViewMinutes.setText(context.getString(R.string.sleep_minutes, newValue));
                timeMinutes = newValue;
                timeMs = TimeUnit.MINUTES.toMillis(timeMinutes);
                volumeStep = volume * STEP_MILLIS / timeMs;
            }
        });


        final TextView textViewVolumePercent = findViewById(R.id.textViewVolumePercent);
        final SleepTimerView sliderVolume = findViewById(R.id.sliderVolume);
        sliderVolume.addListener(new SleepTimerView.SleepTimerViewValueListener() {
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
        volumeStep = volume * STEP_MILLIS / TimeUnit.MINUTES.toMillis(timeMinutes);

        textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));
        textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

        noises = Noises.retrieveNoises(context);
        stream = noises.get(3).getUrl();


//        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume, 0);
//        origStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                timeMs -= STEP_MILLIS;
                if (timeMs <= 0) {
                    SleepAssistantActivity.this.finish();
                    System.exit(0);
                }

                sliderSleepTime.setCurrentValue(TimeUnit.MILLISECONDS.toMinutes(timeMs));

                volume -= volumeStep;

                Log.v(AlarmUtils.TAG, "timems=" + timeMs + " volume=" + volume);

                service.setAudioVolume(volume / 100f);
                sliderVolume.setCurrentValue((long) volume);

                textViewMinutes.setText(context.getString(R.string.sleep_minutes, TimeUnit.MILLISECONDS.toMinutes(timeMs)));
                textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

                handler.postDelayed(this, STEP_MILLIS);
            }
        };
    }

    @OnClick(R.id.play)
    public void onClicked() {
        handler.removeCallbacks(runnable);
        if (isPlaying()) {
            releaseWakeLock();
        } else {
            handler.postDelayed(runnable, STEP_MILLIS);
            acquireWakeLock();
        }

        service.playOrPause(stream);
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyApp::MyWakelockTag");
            wakeLock.acquire(timeMs + TimeUnit.MINUTES.toMillis(2));
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
            case PlaybackStatus.LOADING:
                // loading
                playButton.setText("Loading");
                loadingCount++;
                textViewTime.setText(String.valueOf(loadingCount));
                playButton.setEnabled(false);
                break;
            case PlaybackStatus.ERROR:
                Toast.makeText(this, "Can not stream", Toast.LENGTH_LONG).show();
                playButton.setText("Resume");
                break;
            case PlaybackStatus.PLAYING:
                playButton.setText("Pause");
                break;
            default:
                playButton.setText("Play");
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

        if (isPlaying()) {
            service.playOrPause(stream);
        }

        if (serviceBound) {
            unbind();
        }

        releaseWakeLock();
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

    void unbind() {
        getApplication().unbindService(serviceConnection);
    }

    boolean isPlaying() {
        return service.isPlaying();
    }


    @Override
    public void onFragmentInteraction(String uri) {
        stream = uri;
        onClicked();
    }
}
