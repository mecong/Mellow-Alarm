package com.mecong.myalarm.sleep_assistant.media_selection;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.mecong.myalarm.R;

public class SoundListsPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = new String[3];

    public SoundListsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pageTitles[0] = context.getString(R.string.offline).toUpperCase();
        pageTitles[1] = context.getString(R.string.online).toUpperCase();
        pageTitles[2] = context.getString(R.string.noises).toUpperCase();
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0)
            return LocalFilesMediaFragment.newInstance();
        else if (position == 1)
            return OnlineMediaFragment.newInstance();
        else
            return NoisesFragment.newInstance(0);
    }

    @Override
    public int getCount() {
        return pageTitles.length;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles[position];
    }
}
