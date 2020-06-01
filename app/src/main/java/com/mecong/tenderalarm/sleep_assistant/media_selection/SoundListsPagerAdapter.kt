package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment

class SoundListsPagerAdapter(fm: FragmentManager?, context: Context, sleepAssistantFragment: SleepAssistantFragment) : FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val pageTitles = arrayOfNulls<String>(3)
    private val localFilesMediaFragment = LocalFilesMediaFragment(sleepAssistantFragment)
    private val onlineMediaFragment = OnlineMediaFragment()
    private val noisesFragment = NoisesFragment()


    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> localFilesMediaFragment
            1 -> onlineMediaFragment
            else -> noisesFragment
        }
    }

    override fun getCount(): Int {
        return pageTitles.size
    }

    init {
        pageTitles[0] = context.getString(R.string.offline)
        pageTitles[1] = context.getString(R.string.online)
        pageTitles[2] = context.getString(R.string.noises)
    }
}