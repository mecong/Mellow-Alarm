package com.mecong.tenderalarm.sleep_assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.PropertyName;
import com.mecong.tenderalarm.model.SQLiteDBHelper;
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType;
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepNoise;
import com.mecong.tenderalarm.sleep_assistant.media_selection.SoundListsPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class SleepAssistantFragment extends Fragment {

    private static final long STEP_MILLIS = TimeUnit.SECONDS.toMillis(10);
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
    long timeMinutes;
    long timeMs;
    float volume;
    Handler handler;
    Runnable runnable;
    float volumeStep;
    boolean serviceBound;
    SleepAssistantPlayListModel playListModel;
    private RadioService service;
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

        ButterKnife.bind(this, rootView);

        HyperLog.i(TAG, "Sleep assistant fragment create view");

        if (savedInstanceState != null) {
            return rootView;
        }


        playListModel = (new ViewModelProvider(this)).get(SleepAssistantPlayListModel.class);

        if (playListModel.getPlaylist().getValue() == null) {
            SleepAssistantPlayListModel.PlayList defaultPlayList =
                    new SleepAssistantPlayListModel.PlayList(SleepNoise.Companion.retrieveNoises().get(0).getUrl(),
                            SleepNoise.Companion.retrieveNoises().get(0).getName(), SleepMediaType.NOISE);
            playListModel.setPlaylist(defaultPlayList);
        }

        playListModel.getPlaylist().observe(getViewLifecycleOwner(), new Observer<SleepAssistantPlayListModel.PlayList>() {
            @Override
            public void onChanged(SleepAssistantPlayListModel.PlayList playList) {
                if (service != null)
                    service.playMediaList(playList);
            }
        });


        final Context context = this.getContext();

        final SQLiteDBHelper instance = SQLiteDBHelper.getInstance(getContext());
        final Integer activeTab = instance.getPropertyInt(PropertyName.ACTIVE_TAB);
        initializeTabsAndMediaFragments(context, activeTab);

        timeMinutes = 39;

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

                    if (service.isPlaying()) {
                        service.stop();
                        timeMinutes = 30;
                        sliderSleepTime.setCurrentValue(timeMinutes);
                        textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));

                        volume = 30;
                        service.setAudioVolume(volume / 100f);
                        sliderVolume.setCurrentValue((long) volume);
                        textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));
                    }
                } else {
                    timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
                    sliderSleepTime.setCurrentValue(timeMinutes);
                    textViewMinutes.setText(context.getString(R.string.sleep_minutes, timeMinutes));

                    volume -= volumeStep;
                    service.setAudioVolume(volume / 100f);
                    sliderVolume.setCurrentValue((long) volume);
                    textViewVolumePercent.setText(context.getString(R.string.volume_percent, (int) volume));

                    handler.postDelayed(this, STEP_MILLIS);
                }
            }
        };

        return rootView;
    }

    private void initializeTabsAndMediaFragments(Context context, int activeTab) {
        SoundListsPagerAdapter soundListsPagerAdapter =
                new SoundListsPagerAdapter(this.getActivity().getSupportFragmentManager(), context, playListModel);
        viewPager.setAdapter(soundListsPagerAdapter);

        tabs.setupWithViewPager(viewPager);

        tabs.setSelectedTabIndicator(null);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            final SQLiteDBHelper instance = SQLiteDBHelper.getInstance(getContext());

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getCustomView().findViewById(R.id.view).setVisibility(View.INVISIBLE);
                tab.getCustomView().setBackground(getResources().getDrawable(R.drawable.tr2_background));

                instance.setPropertyString(PropertyName.ACTIVE_TAB, String.valueOf(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getCustomView().findViewById(R.id.view).setVisibility(View.VISIBLE);
                tab.getCustomView().setBackground(getResources().getDrawable(R.drawable.tr1_background));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
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
                service.playMediaList(playListModel.getPlaylist().getValue());
            }
        }
    }
//
//    private void acquireWakeLock() {
//        if (wakeLock == null) {
//            PowerManager powerManager = (PowerManager) this.getActivity().getSystemService(POWER_SERVICE);
//            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                    "TenderAlarm::SleepAssistantWakelockTag");
//        }
//
//        if (wakeLock != null && !wakeLock.isHeld()) {
//            wakeLock.acquire(timeMs);
//        }
//    }
//
//    private void releaseWakeLock() {
//        if (wakeLock != null && wakeLock.isHeld()) {
//            wakeLock.release();
//        }
//    }

    @Subscribe(sticky = true)
    public void onPlayFileChanged(SleepAssistantPlayListModel.Media media) {
        nowPlayingText.setText(media.getTitle());
    }


    @Subscribe(sticky = true)
    public void onEvent(String status) {

        ppButton.setEnabled(true);
        switch (status) {
            case RadioService.LOADING:
                ppButton.setImageResource(R.drawable.pause_btn);
                ppButton.setColorFilter(0);

                handler.removeCallbacks(runnable);
                break;
            case RadioService.ERROR:
                nowPlayingText.setText(getString(R.string.can_not_stream));
                Toast.makeText(this.getContext(), getString(R.string.can_not_stream), Toast.LENGTH_SHORT).show();
                ppButton.setImageResource(R.drawable.play_btn);

                playListModel.playing.setValue(false);
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


                playListModel.playing.setValue(true);

                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, STEP_MILLIS);
                break;
            default:
                ppButton.setImageResource(R.drawable.play_btn);

                playListModel.playing.setValue(false);
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
        HyperLog.i(TAG, "SA OnStart");
        bindRadioService();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        HyperLog.i(TAG, "SA onStop");

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        HyperLog.i(TAG, "SA onPause");

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        handler.removeCallbacks(runnable);

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
        HyperLog.i(TAG, "SA onResume");
    }

    private void bindRadioService() {
        Intent intent = new Intent(this.getContext().getApplicationContext(), RadioService.class);
        this.getActivity().getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (service != null)
            EventBus.getDefault().post(service.getStatus());
    }

}
