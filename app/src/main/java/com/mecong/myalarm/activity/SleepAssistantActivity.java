package com.mecong.myalarm.activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.mecong.myalarm.R;

import java.io.IOException;

public class SleepAssistantActivity extends AppCompatActivity {


    //    private final static String stream = "https://www.ssaurel.com/tmp/mymusic.mp3";
//    private final static String stream = "http://radio.4duk.ru/4duk128.mp3";
    private final static String stream = "http://listen2.myradio24.com:9000/8226";
    static Button play;
    static MediaPlayer mediaPlayer;
    static boolean prepared = false;
    boolean started = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_assistant);

        play = findViewById(R.id.play);
        play.setEnabled(false);
        play.setText("Loading..");


        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume, 0);


        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (started) {
                    mediaPlayer.pause();
                    started = false;
                    play.setText("Play");
                } else {
                    mediaPlayer.start();
                    started = true;
                    play.setText("Pause");
                }

            }
        });

        new PlayTask(getApplicationContext()).execute(stream);
    }

    @Override
    protected void onPause() {
        super.onPause();
       /* if(started)
            mediaPlayer.pause();*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if(started)
            mediaPlayer.start();*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mediaPlayer.release();
    }

    private static class PlayTask extends AsyncTask<String, Void, Boolean> {
        Context context;

        public PlayTask(Context applicationContext) {
            this.context = applicationContext;
        }

        @Override
        protected Boolean doInBackground(String... strings) {

            try {

                mediaPlayer.stop();
                mediaPlayer.reset();
//                mediaPlayer.release();
                mediaPlayer.setDataSource(strings[0]);
//                mediaPlayer.setDataSource(context, Uri.parse(strings[0]));

//                mediaPlayer.setDataSource(context, Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.long_music));
                mediaPlayer.prepare();
                mediaPlayer.setVolume(1, 1);
                prepared = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            play.setEnabled(true);
            play.setText("Play");

        }
    }
}
