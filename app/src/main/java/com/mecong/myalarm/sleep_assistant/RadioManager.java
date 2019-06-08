package com.mecong.myalarm.sleep_assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;

public class RadioManager {

    private static RadioManager instance = null;

    private static RadioService service;

    private Context context;

    private boolean serviceBound;

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

    private RadioManager(Context context) {
        this.context = context;
        serviceBound = false;
    }

    static RadioManager with(Context context) {

        if (instance == null)
            instance = new RadioManager(context);

        return instance;
    }

    public static RadioService getService() {
        return service;
    }

    void playOrPause(String streamUrl) {

        service.playOrPause(streamUrl);
    }

    public float getVolume() {
        return service.getAudioVolume();
    }

    void setVolume(float volume) {
        if (service != null)
            service.setAudioVolume(volume);
    }

    void bind() {

        Intent intent = new Intent(context, RadioService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (service != null)
            EventBus.getDefault().post(service.getStatus());
    }

    void unbind() {

        context.unbindService(serviceConnection);
    }

    boolean isPlaying() {
        return service.isPlaying();
    }

}
