package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel
import com.mecong.tenderalarm.sleep_assistant.media_selection.NoisesFragment.Companion.newInstance

class SoundListsPagerAdapter(fm: FragmentManager?, context: Context, private val model: SleepAssistantPlayListModel) : FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val pageTitles = arrayOfNulls<String>(3)

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> LocalFilesMediaFragment(model)
            1 -> OnlineMediaFragment(model)
            else -> newInstance(0, model)
        }
    }

    override fun getCount(): Int {
        return pageTitles.size
    }

    init {
        pageTitles[0] = context.getString(R.string.offline).toUpperCase()
        pageTitles[1] = context.getString(R.string.online).toUpperCase()
        pageTitles[2] = context.getString(R.string.noises).toUpperCase()
    }
}