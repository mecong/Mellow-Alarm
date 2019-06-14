package com.mecong.myalarm.sleep_assistant;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.mecong.myalarm.R;

public class SoundListsPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = new String[3];

    SoundListsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        pageTitles[0] = context.getString(R.string.offline).toUpperCase();
        pageTitles[1] = context.getString(R.string.online).toUpperCase();
        pageTitles[2] = context.getString(R.string.noises).toUpperCase();
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new NoisesFragment();
        Bundle args = new Bundle();
        args.putInt("selectedPosition", 3);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles[position];
    }
}
