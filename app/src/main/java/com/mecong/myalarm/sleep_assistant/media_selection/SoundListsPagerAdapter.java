package com.mecong.myalarm.sleep_assistant.media_selection;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.mecong.myalarm.R;
import com.mecong.myalarm.sleep_assistant.SleepAssistantViewModel;

public class SoundListsPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = new String[3];
    private SleepAssistantViewModel model;

    public SoundListsPagerAdapter(FragmentManager fm, Context context, SleepAssistantViewModel model) {
        super(fm);
        this.model = model;
        pageTitles[0] = context.getString(R.string.offline).toUpperCase();
        pageTitles[1] = context.getString(R.string.online).toUpperCase();
        pageTitles[2] = context.getString(R.string.noises).toUpperCase();
    }

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
