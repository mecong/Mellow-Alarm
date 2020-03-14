package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel.PlayList
import com.mecong.tenderalarm.sleep_assistant.media_selection.NoisesItemViewAdapter.NoisesItemClickListener
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepNoise.Companion.retrieveNoises

class NoisesFragment private constructor(private val model: SleepAssistantPlayListModel) : Fragment(), NoisesItemClickListener {
    private var selectedPosition = 0
    private var adapter: NoisesItemViewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            selectedPosition = arguments!!.getInt(SELECTED_POSITION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_noises, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView = view.findViewById(R.id.noisesList)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = NoisesItemViewAdapter(view.context,
                retrieveNoises(), selectedPosition)
        adapter!!.setClickListener(this)
        recyclerView.adapter = adapter
    }

    override fun onItemClick(view: View?, position: Int) {
        val newPlayList = PlayList(
                adapter!!.getItem(position).url, adapter!!.getItem(position).name, SleepMediaType.NOISE)
        model.playlist.value = newPlayList
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
        fun newInstance(selectedPosition: Int, model: SleepAssistantPlayListModel): NoisesFragment {
            val fragment = NoisesFragment(model)
            val args = Bundle()
            args.putInt(SELECTED_POSITION, selectedPosition)
            fragment.arguments = args
            return fragment
        }
    }

}