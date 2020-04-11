package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel.SleepAssistantPlayList
import com.mecong.tenderalarm.sleep_assistant.media_selection.NoisesItemViewAdapter.NoisesItemClickListener
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepNoise.Companion.retrieveNoises
import kotlinx.android.synthetic.main.fragment_noises.*

class NoisesFragment private constructor() : Fragment(), NoisesItemClickListener {
    private var selectedPosition = 0
    private lateinit var adapter: NoisesItemViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedPosition = arguments?.getInt(SELECTED_POSITION) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_noises, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        noisesList.layoutManager = LinearLayoutManager(view.context)
        adapter = NoisesItemViewAdapter(view.context, retrieveNoises(), selectedPosition)
        adapter.setClickListener(this)
        noisesList.adapter = adapter
    }

    override fun onItemClick(view: View?, position: Int) {
        val newPlayList = SleepAssistantPlayList(
                adapter.getItem(position).url, adapter.getItem(position).name, SleepMediaType.NOISE)
        SleepAssistantFragment.playListModel.playlist.value = newPlayList
    }

    companion object {
        private const val SELECTED_POSITION = "selectedPosition"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param selectedPosition - selected item in the list
         * @return A new instance of fragment NoisesFragment.
         */
        @JvmStatic
        fun noisesFragmentNewInstance(selectedPosition: Int): NoisesFragment {
            val fragment = NoisesFragment()
            val args = Bundle()
            args.putInt(SELECTED_POSITION, selectedPosition)
            fragment.arguments = args
            return fragment
        }
    }

}