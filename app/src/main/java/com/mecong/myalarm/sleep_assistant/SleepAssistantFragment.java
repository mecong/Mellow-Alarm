package com.mecong.myalarm.sleep_assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mecong.myalarm.R;
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

import static android.content.Context.POWER_SERVICE;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class SleepAssistantFragment extends Fragment {

    private static final long STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static RadioService service;

    @BindView(R.id.pp_button)
    ImageButton ppButton;
    @BindView(R.id.textViewTime)
    TextView textViewTime;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.textViewMinutes)
    TextView textViewMinutes;
    @BindView(R.id.nowPlayingText)
    TextView nowPlayingText;
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
    SleepAssistantViewModel model;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            service = ((RadioService.LocalBinder) binder).getService();
            service.setAudioVolume(volume / 100f);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.content_sleep_assistant, container, false);

        if (savedInstanceState != null)
            return rootView;


        model = ViewModelProviders.of(this).get(SleepAssistantViewModel.class);

        if (model.getPlaylist().getValue() == null) {
            SleepAssistantViewModel.PlayList defaultPlayList =
                    new SleepAssistantViewModel.PlayList(SleepNoise.Companion.retrieveNoises().get(0).getUrl(),
                            SleepNoise.Companion.retrieveNoises().get(0).getName(), SleepMediaType.NOISE);
            model.setPlaylist(defaultPlayList);
        }

        model.getPlaylist().observe(this, new Observer<SleepAssistantViewModel.PlayList>() {
            @Override
            public void onChanged(SleepAssistantViewModel.PlayList playList) {
                if (service != null)
                    service.playMediaList(playList);
            }
        });


        ButterKnife.bind(this, rootView);

        final Context context = this.getContext().getApplicationContext();

        initializeTabsAndMediaFragments(context, 1);

        timeMinutes = 42;

        AudioManager audioManager = (AudioManager) this.getActivity().getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        float volumeCoefficient = (float) systemVolume / streamMaxVolume;

        if (volumeCoefficient < 0.3f) {
            volumeCoefficient = 0.3f;
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC, (int) (streamMaxVolume * volumeCoefficient), 0);
            Toast.makeText(context, "System volume set to 30%", Toast.LENGTH_SHORT).show();
        }
        volume = 105 - 100 * volumeCoefficient;


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
                        System.exit(0);
                    }
                } else {
                    timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
                    sliderSleepTime.setCurrentValue(timeMinutes);

                    volume -= volumeStep;
//                    Log.v(AlarmUtils.TAG, "timems=" + timeMs + " volume=" + volume / 100f);
                    service.setAudioVolume(volume / 100f);
                    sliderVolume.setCurrentValue((long) volume);

                    textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));
                    textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

                    handler.postDelayed(this, STEP_MILLIS);
                }
            }
        };

        return rootView;
    }

    private void initializeTabsAndMediaFragments(Context context, int activeTab) {
        SoundListsPagerAdapter soundListsPagerAdapter =
                new SoundListsPagerAdapter(this.getActivity().getSupportFragmentManager(), context, model);
        viewPager.setAdapter(soundListsPagerAdapter);

        tabs.setupWithViewPager(viewPager);

        tabs.setSelectedTabIndicator(null);
        tabs.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getCustomView().findViewById(R.id.view).setVisibility(View.INVISIBLE);
                tab.getCustomView().setBackground(getResources().getDrawable(R.drawable.tr2_background));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getCustomView().findViewById(R.id.view).setVisibility(View.VISIBLE);
                tab.getCustomView().setBackground(getResources().getDrawable(R.drawable.tr1_background));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.getTabAt(i).setCustomView(R.layout.media_tab);
        }

        ((ImageView) tabs.getTabAt(0).getCustomView().findViewById(R.id.imageView)).setImageResource(R.drawable.local_media);
        ((ImageView) tabs.getTabAt(1).getCustomView().findViewById(R.id.imageView)).setImageResource(R.drawable.online_media);
        ((ImageView) tabs.getTabAt(2).getCustomView().findViewById(R.id.imageView)).setImageResource(R.drawable.noises);

        tabs.getTabAt(activeTab % tabs.getTabCount()).select();
    }


    @OnClick(R.id.pp_button)
    void onPlayPauseButtonsClicked() {
        if (service.isPlaying()) {
            service.pause();
        } else {
            if (service.hasPlayList()) {
                service.resume();
            } else {
                service.playMediaList(model.getPlaylist().getValue());
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) this.getActivity().getSystemService(POWER_SERVICE);
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
    public void onPlayFileChanged(SleepAssistantViewModel.Media media) {
        nowPlayingText.setText(media.getTitle());
    }

    @Subscribe
    public void onEvent(String status) {

        ppButton.setEnabled(true);
        switch (status) {
            case RadioService.LOADING:
//                playButton.setText("Loading");
                ppButton.setImageResource(R.drawable.pause_btn);
                loadingCount++;
                textViewTime.setText(String.valueOf(loadingCount));
//                ppButton.setEnabled(false);
                ppButton.setColorFilter(0);
                handler.removeCallbacks(runnable);
                break;
            case RadioService.ERROR:
                nowPlayingText.setText(getString(R.string.can_not_stream));
                Toast.makeText(this.getContext(), getString(R.string.can_not_stream), Toast.LENGTH_SHORT).show();
//                playButton.setText("Resume");
                ppButton.setImageResource(R.drawable.play_btn);

                model.playing.setValue(false);
                releaseWakeLock();
                handler.removeCallbacks(runnable);
                break;
            case RadioService.PLAYING:


                TransitionSet transition = new TransitionSet()
                        .addTransition(new ChangeBounds().setDuration(500))
                        .addTransition(new ChangeImageTransform().setDuration(500))
                        .addTransition(new Fade(Fade.OUT).setDuration(500));
//                TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.now_playing), transition);
                ppButton.setImageResource(R.drawable.pause_btn);
//                ppButton.setPaddingRelative(10,10,10,10);
                ppButton.setImageTintMode(PorterDuff.Mode.ADD);


                model.playing.setValue(true);

                handler.postDelayed(runnable, STEP_MILLIS);
                acquireWakeLock();
                break;
            default:
                ppButton.setImageResource(R.drawable.play_btn);

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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        releaseWakeLock();

        if (service.isPlaying()) {
            service.stop();
        }

        if (serviceBound) {
            this.getActivity().getApplication().unbindService(serviceConnection);
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void bind() {
        Intent intent = new Intent(this.getContext().getApplicationContext(), RadioService.class);
        this.getActivity().getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (service != null)
            EventBus.getDefault().post(service.getStatus());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
