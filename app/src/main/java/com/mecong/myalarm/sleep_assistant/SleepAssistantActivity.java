package com.mecong.myalarm.sleep_assistant;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mecong.myalarm.R;
import com.mecong.myalarm.alarm.AlarmUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SleepAssistantActivity extends AppCompatActivity {

    public static final long STEP_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private RadioManager radioManager;
    private List<Shoutcast> shoutcasts;
    private long timeMinutes;
    private long timeMs;
    private String stream;
    private float volume;
    private Handler handler;
    private Runnable runnable;
    private float volumeStep;
    private int origStreamVolume;
    private AudioManager audioManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_assistant);

        radioManager = RadioManager.with(this);
        ButterKnife.bind(this);

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
                radioManager.setVolume(volume / 100f);
                volumeStep = volume * STEP_MILLIS / timeMs;
            }
        });

        sliderSleepTime.setCurrentValue(timeMinutes);
        sliderVolume.setCurrentValue((long) volume);
        volumeStep = volume * STEP_MILLIS / TimeUnit.MINUTES.toMillis(timeMinutes);

        textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));
        textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

        shoutcasts = Shoutcast.retrieveShoutcasts(context);
        stream = shoutcasts.get(0).getUrl();


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

                radioManager.setVolume(volume / 100f);
                sliderVolume.setCurrentValue((long) volume);

                textViewMinutes.setText(context.getString(R.string.sleep_minutes, TimeUnit.MILLISECONDS.toMinutes(timeMs)));
                textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));


                handler.postDelayed(this, STEP_MILLIS);
            }
        };


    }

    @OnClick(R.id.play)
    public void onClicked() {
        if (radioManager.isPlaying()) {
            handler.removeCallbacks(runnable);
        } else {
            handler.post(runnable);
        }

        radioManager.playOrPause(stream);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(runnable);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, origStreamVolume, 0);
        radioManager.unbind();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        radioManager.bind();
    }

}
