package com.mecong.tenderalarm.sleep_assistant.media_selection;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel;

public class SoundListsPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = new String[3];
    private SleepAssistantPlayListModel model;

    public SoundListsPagerAdapter(FragmentManager fm, Context context, SleepAssistantPlayListModel model) {
        super(fm);
        this.model = model;
        pageTitles[0] = context.getString(R.string.offline).toUpperCase();
        pageTitles[1] = context.getString(R.string.online).toUpperCase();
        pageTitles[2] = context.getString(R.string.noises).toUpperCase();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new LocalFilesMediaFragment(model);
        } else if (position == 1) {
            return new OnlineMediaFragment(model);
        } else {
            return NoisesFragment.newInstance(0, model);
        }
    }

    @Override
    public int getCount() {
        return pageTitles.length;
    }
}
