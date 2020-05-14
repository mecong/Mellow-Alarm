package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.Media
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListActive
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListIdle
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepNoise.Companion.retrieveNoises
import kotlinx.android.synthetic.main.fragment_noises.*
import org.greenrobot.eventbus.EventBus

class NoisesFragment : Fragment(), NoisesItemClickListener {
    private var selectedPosition = 0
    private lateinit var adapter: NoisesItemViewAdapter
    private lateinit var noises: List<Media>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_noises, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        noisesList.layoutManager = LinearLayoutManager(view.context)

        val sqLiteDBHelper = SQLiteDBHelper.sqLiteDBHelper(this.context!!)!!
        noises = retrieveNoises(this.context!!)

        val savedActiveTab = sqLiteDBHelper.getPropertyInt(PropertyName.ACTIVE_TAB)

        selectedPosition = -1
        if (savedActiveTab == 2) {
            selectedPosition = sqLiteDBHelper.getPropertyInt(PropertyName.TRACK_NUMBER) ?: 0
            initPlaylist(selectedPosition, false)
        }

        adapter = NoisesItemViewAdapter(this.context, noises, selectedPosition)
        adapter.setClickListener(this)
        noisesList.adapter = adapter
        noisesList.scrollToPosition(selectedPosition)
    }

    override fun onItemClick(view: View?, position: Int) {
        initPlaylist(position, true)
    }

    private fun initPlaylist(position: Int, active: Boolean) {
        val newPlayList = if (active)
            SleepAssistantPlayListActive(position, noises, SleepMediaType.NOISE, -1)
        else
            SleepAssistantPlayListIdle(position, noises, SleepMediaType.NOISE, -1)

        EventBus.getDefault().postSticky(newPlayList)
    }

    companion object {
        private const val SELECTED_POSITION = "selectedPosition"
    }

}