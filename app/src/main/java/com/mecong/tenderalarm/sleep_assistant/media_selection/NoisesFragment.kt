package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mecong.tenderalarm.databinding.FragmentNoisesBinding
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.Media
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListActive
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListIdle
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepNoise.Companion.retrieveNoises
import org.greenrobot.eventbus.EventBus

class NoisesFragment : Fragment(), NoisesItemClickListener {
  private var selectedPosition = 0
  private lateinit var adapter: NoisesItemViewAdapter
  private lateinit var noises: List<Media>

  private var _binding: FragmentNoisesBinding? = null

  // This property is only valid between onCreateView and onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentNoisesBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.noisesList.layoutManager = LinearLayoutManager(view.context)

    val sqLiteDBHelper = SQLiteDBHelper.sqLiteDBHelper(this.requireContext())!!
    noises = retrieveNoises(this.requireContext())

    val savedActiveTab = sqLiteDBHelper.getPropertyInt(PropertyName.ACTIVE_TAB)

    selectedPosition = -1
    if (savedActiveTab == 2) {
      selectedPosition = sqLiteDBHelper.getPropertyInt(PropertyName.TRACK_NUMBER) ?: 0
      initPlaylist(selectedPosition, false)
    }

    adapter = NoisesItemViewAdapter(this.context, noises, selectedPosition)
    adapter.setClickListener(this)
    binding.noisesList.adapter = adapter
    binding.noisesList.scrollToPosition(if (selectedPosition > -1) selectedPosition else 0)
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
}