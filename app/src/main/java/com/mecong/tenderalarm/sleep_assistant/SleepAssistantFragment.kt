package com.mecong.tenderalarm.sleep_assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.MainActivityMessages
import com.mecong.tenderalarm.databinding.ContentSleepAssistantBinding
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.RadioService.LocalBinder
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType
import com.mecong.tenderalarm.sleep_assistant.media_selection.SoundListsPagerAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class SleepAssistantFragment : Fragment() {
  var volume = 0f
  var volumeStep = 0f
  var timeMs: Long = 0
  var timeMinutes: Long = 0
  private val sleepTimeHandler: Handler = Handler()
  private lateinit var sleepTimeRunnable: Runnable
  private val progressHandler: Handler = Handler()
  private lateinit var progressRunnable: Runnable
  private var showRadioBuffer = false

  var serviceBound = false
  lateinit var radioService: RadioService

  private var _binding: ContentSleepAssistantBinding? = null

  // This property is only valid between onCreateView and onDestroyView.
  private val binding get() = _binding!!

  private var serviceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
      radioService = (binder as LocalBinder).service
      serviceBound = true
      radioService.audioVolume = volume / 100

      val dbHelper = sqLiteDBHelper(this@SleepAssistantFragment.requireContext())!!
      val savedShuffle = dbHelper.getPropertyString(PropertyName.SHUFFLE)?.toBoolean() ?: false
      radioService.shuffleModeEnabled = savedShuffle

      if (!EventBus.getDefault().isRegistered(this@SleepAssistantFragment)) {
        EventBus.getDefault().register(this@SleepAssistantFragment)
      }
    }

    override fun onServiceDisconnected(arg0: ComponentName) {
      serviceBound = false
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    playListModel = ViewModelProvider(this).get(SleepAssistantPlayListModel::class.java)

    val context = this.requireContext()
    val dbHelper = sqLiteDBHelper(context)!!

    showRadioBuffer = dbHelper.getPropertyString(PropertyName.SHOW_RADIO_BUFFER) == "1"

    binding.textViewMinutes.setOnLongClickListener {
      showRadioBuffer = !showRadioBuffer
      dbHelper.setPropertyString(PropertyName.SHOW_RADIO_BUFFER, if (showRadioBuffer) "1" else "0")
      true
    }

    val activeTab = dbHelper.getPropertyInt(PropertyName.ACTIVE_TAB) ?: 2
    initializeTabsAndMediaFragments(context, activeTab)

    timeMinutes = dbHelper.getPropertyLong(PropertyName.SLEEP_TIME) ?: 39
    binding.sliderSleepTime.setCurrentValue(timeMinutes)
    binding.textViewMinutes.text =
      context.resources.getQuantityString(R.plurals.n_minutes_plural, timeMinutes.toInt(), timeMinutes.toInt())

    binding.sliderSleepTime.addListener(object : SleepTimerViewValueListener {
      override fun onValueChanged(newValue: Long) {
        dbHelper.setPropertyString(PropertyName.SLEEP_TIME, newValue.toString())
        binding.textViewMinutes.text =
          context.resources.getQuantityString(R.plurals.n_minutes_plural, newValue.toInt(), newValue.toInt())
        timeMinutes = newValue
        timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)
        volumeStep = volume * STEP_MILLIS / timeMs
      }
    })

    binding.sliderVolume.addListener(object : SleepTimerViewValueListener {
      override fun onValueChanged(newValue: Long) {
        binding.textViewVolumePercent.text = context.getString(R.string.volume_percent, newValue)
        volume = newValue.toFloat()
        radioService.audioVolume = volume / 100f
        volumeStep = volume * STEP_MILLIS / timeMs
      }
    })

    sleepTimeRunnable = Runnable {
      timeMs -= STEP_MILLIS
      if (timeMs <= 0) {
        sleepTimeHandler.removeCallbacks(sleepTimeRunnable)

        if (radioService.isPlaying) {
          radioService.pause()

          timeMinutes = 30
          binding.sliderSleepTime.setCurrentValue(timeMinutes)
          timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)

          binding.textViewMinutes.text = context.resources.getQuantityString(
            R.plurals.n_minutes_plural,
            timeMinutes.toInt(),
            timeMinutes.toInt()
          )

          volume = 30f
          binding.sliderVolume.setCurrentValue(volume.toLong())
          binding.textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())
          radioService.audioVolume = volume / 100f

          volumeStep = volume * STEP_MILLIS / timeMs
        }
      } else {
        timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
        binding.sliderSleepTime.setCurrentValue(timeMinutes)
        binding.textViewMinutes.text = context.resources.getQuantityString(
          R.plurals.n_minutes_plural,
          timeMinutes.toInt(),
          timeMinutes.toInt()
        )

        volume -= volumeStep
        radioService.audioVolume = volume / 100f
        binding.sliderVolume.setCurrentValue(volume.toLong())

        binding.textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())

        sleepTimeHandler.postDelayed(sleepTimeRunnable, STEP_MILLIS)
      }
    }


    binding.nowPlayingText.setOnClickListener {
      val currentTab = dbHelper.getPropertyInt(PropertyName.ACTIVE_TAB) ?: 2
      binding.tabs.getTabAt(currentTab % binding.tabs.tabCount)!!.select()
    }
  }

  fun flipShuffleMode(): Boolean {
    radioService.shuffleModeEnabled = !radioService.shuffleModeEnabled
    return radioService.shuffleModeEnabled
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bindRadioService()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = ContentSleepAssistantBinding.inflate(inflater, container, false)
    return binding.root
  }

  private fun initializeTabsAndMediaFragments(context: Context?, activeTab: Int) {
    if (context == null) return

    val soundListsPagerAdapter = SoundListsPagerAdapter(this.activity?.supportFragmentManager, context)
    binding.viewPager.adapter = soundListsPagerAdapter
    binding.tabs.setupWithViewPager(binding.viewPager)
    binding.tabs.setSelectedTabIndicator(null)

    binding.tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {
        tab.customView?.findViewById<View>(R.id.blackLine)?.visibility = View.INVISIBLE
        tab.customView?.background = ResourcesCompat.getDrawable(resources, R.drawable.tr2_background, null)
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {
        tab.customView?.findViewById<View>(R.id.blackLine)?.visibility = View.VISIBLE
        tab.customView?.background = ResourcesCompat.getDrawable(resources, R.drawable.tr1_background, null)
      }

      override fun onTabReselected(tab: TabLayout.Tab) {
        onTabSelected(tab)
      }
    })

    for (i in 0 until binding.tabs.tabCount) {
      binding.tabs.getTabAt(i)?.setCustomView(R.layout.media_tab)
    }

    (binding.tabs.getTabAt(0)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
      .setImageResource(R.drawable.local_media)
    (binding.tabs.getTabAt(1)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
      .setImageResource(R.drawable.online_media)
    (binding.tabs.getTabAt(2)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
      .setImageResource(R.drawable.noises)

    binding.tabs.getTabAt(activeTab % binding.tabs.tabCount)!!.select()
  }

  @Subscribe
  fun onPlayButtonClicked(message: MainActivityMessages) {
    if (message == MainActivityMessages.SWITCH_PLAYBACK) {
      if (radioService.isPlaying) {
        radioService.pause()
      } else {
        if (radioService.hasPlayList()) {
          radioService.resume()
        } else {
          radioService.setMediaList(playListModel.playlist.value!!)
        }
      }
    }
  }


  @Subscribe
  fun onEvent(status: RadioServiceStatus) {
    when (status) {
      RadioServiceStatus.LOADING -> {
        sleepTimeHandler.removeCallbacks(sleepTimeRunnable)
      }
      RadioServiceStatus.ERROR -> {
        binding.nowPlayingText.text = getString(R.string.can_not_stream)
        Toast.makeText(this.context, getString(R.string.can_not_stream), Toast.LENGTH_SHORT).show()
        playListModel.playing.setValue(false)
      }
      RadioServiceStatus.PLAYING -> {

        startTimelineWatcher()
        playListModel.playing.value = true
        sleepTimeHandler.removeCallbacks(sleepTimeRunnable)
        sleepTimeHandler.postDelayed(sleepTimeRunnable, STEP_MILLIS)
      }
      else -> {
        playListModel.playing.value = false
        sleepTimeHandler.removeCallbacks(sleepTimeRunnable)
      }
    }
  }

  override fun onDestroy() {
    EventBus.getDefault().unregister(this)
    sleepTimeHandler.removeCallbacksAndMessages(null)
    progressHandler.removeCallbacksAndMessages(null)
    if (::radioService.isInitialized) {
      if (radioService.isPlaying) {
        radioService.stop()
      }
      if (serviceBound) {
        this.activity?.application?.unbindService(serviceConnection)
      }
    }
    super.onDestroy()
  }

  override fun onResume() {
    super.onResume()

    if (!::radioService.isInitialized || !radioService.isPlaying) {
      val audioManager = this.activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
      val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
      val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      var volumeCoefficient = systemVolume.toFloat() / streamMaxVolume
      if (volumeCoefficient >= 0.3f) {
        volumeCoefficient = 0.05f
        audioManager.setStreamVolume(
          AudioManager.STREAM_MUSIC, (streamMaxVolume * volumeCoefficient).roundToInt(), 0
        )
        Toast.makeText(
          this.requireActivity(),
          requireContext().getString(R.string.system_volume_toast),
          Toast.LENGTH_SHORT
        ).show()
      }

      volume = 105 - 100 * volumeCoefficient
      volume = min(volume, 100f)

      binding.sliderVolume.setCurrentValue(volume.toLong())
      timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)
      volumeStep = volume * STEP_MILLIS / timeMs
      binding.textViewVolumePercent.text = this.activity?.getString(R.string.volume_percent, volume.roundToInt())
    }
  }

  private fun startTimelineWatcher() {
    val localFileProgress = Runnable {
      Timber.v(
        "Local: Buffered position %d, content %d",
        radioService.getBufferedPos(),
        radioService.getContentPos()
      )
      binding.nowPlayingText.setPlayPosition(radioService.getContentPos() / radioService.getContentDuration().toFloat())

      binding.playerTime1.text = getElapsedTime()
      binding.playerTime2.text = getTimeLeft()

      if (radioService.isPlaying) {
        progressHandler.postDelayed(progressRunnable, 300)
      }
    }

    val onlineStreamProgress = Runnable {
      Timber.v(
        "Online: Buffered position %d, content %d",
        radioService.getBufferedPos(),
        radioService.getContentPos()
      )

      binding.playerTime1.text = getElapsedTime()
      binding.playerTime2.text = getBufferedSize()

      if (radioService.sleepAssistantPlayList?.mediaType == SleepMediaType.ONLINE) {
        progressHandler.postDelayed(progressRunnable, 800)
      }
    }

    progressHandler.removeCallbacksAndMessages(null)
    when (radioService.sleepAssistantPlayList?.mediaType) {
      SleepMediaType.LOCAL -> {
        progressRunnable = localFileProgress
        progressHandler.post(progressRunnable)
        binding.playerTime2.visibility = View.VISIBLE
        binding.playerTime1.visibility = View.VISIBLE
      }
      SleepMediaType.ONLINE -> {
        if (showRadioBuffer) {
          progressRunnable = onlineStreamProgress
          progressHandler.post(progressRunnable)
          binding.playerTime2.visibility = View.VISIBLE
          binding.playerTime1.visibility = View.VISIBLE
        } else {
          binding.playerTime2.visibility = View.GONE
          binding.playerTime1.visibility = View.GONE
        }
      }
      else -> {
        binding.playerTime2.visibility = View.GONE
        binding.playerTime1.visibility = View.GONE
      }
    }
  }

  private fun getStringForTime(timeMs: Long, negative: Boolean): String? {
    val totalSeconds = if (timeMs < 0) 0 else (timeMs + 500) / 1000

    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60

    val formattedTime = if (totalSeconds > 3600) {
      this.context?.getString(R.string.play_time_with_hour, totalSeconds / 3600, minutes, seconds)
    } else {
      this.context?.getString(R.string.play_time, minutes, seconds)
    }

    return if (negative) {
      "-$formattedTime"
    } else
      formattedTime
  }

  private fun getElapsedTime(percentPos: Float? = null): CharSequence? {
    return if (percentPos == null) {
      getStringForTime(radioService.getContentPos(), false)
    } else {
      getStringForTime((percentPos * radioService.getContentDuration()).roundToLong(), false)
    }
  }

  private fun getTimeLeft(percentPos: Float? = null): CharSequence? {
    return if (percentPos == null) {
      getStringForTime(radioService.getContentDuration() - radioService.getContentPos(), true)
    } else {
      getStringForTime(
        radioService.getContentDuration() - (percentPos * radioService.getContentDuration()).roundToLong(),
        true
      )
    }
  }

  private fun getBufferedSize(): CharSequence? {
    val buff = radioService.getBufferedPos() - radioService.getContentPos()
    return getStringForTime(buff, true)
  }

  @Subscribe(sticky = true)
  fun onPlayFileChanged(playList: SleepAssistantPlayListActive) {
    binding.nowPlayingText.interactiveMode = playList.mediaType == SleepMediaType.LOCAL
    radioService.setMediaList(playList)
    playListModel.playlist.value = playList

    radioService.play()
  }

  @Subscribe(sticky = true)
  fun onPlayFileChanged(playList: SleepAssistantPlayListIdle) {
    binding.nowPlayingText.interactiveMode = playList.mediaType == SleepMediaType.LOCAL
    radioService.setMediaList(playList)
    playListModel.playlist.value = playList
  }

  @Subscribe
  fun onPlayPosChanged(playPosition: PlayPosition) {
    if (playPosition.final) {
      radioService.seekToPercent(playPosition.playPositionPercent)
      progressHandler.post(progressRunnable)
    } else {
      progressHandler.removeCallbacks(progressRunnable)
    }

    binding.playerTime2.text = getTimeLeft(playPosition.playPositionPercent)
    binding.playerTime1.text = getElapsedTime(playPosition.playPositionPercent)
  }

  @Subscribe
  fun onPlayFileChanged(media: Media) {
    binding.nowPlayingText.text = media.title
  }

  @Subscribe
  fun persistMediaPosition(playList: SleepAssistantPlayList) {
    if (this.context == null) return

    val dbHelper = sqLiteDBHelper(this.requireContext())!!

    val activeTab = when (playList.mediaType) {
      SleepMediaType.LOCAL -> "0"
      SleepMediaType.ONLINE -> "1"
      SleepMediaType.NOISE -> "2"
    }

    dbHelper.setPropertyString(PropertyName.ACTIVE_TAB, activeTab)
    dbHelper.setPropertyString(PropertyName.TRACK_NUMBER, playList.index.toString())
    dbHelper.setPropertyString(PropertyName.PLAYLIST_ID, playList.playListId.toString())
    dbHelper.setPropertyString(PropertyName.POSITION_IN_TRACK, radioService.getContentPos().toString())
  }

  private fun bindRadioService() {
    val intent = Intent(this.context, RadioService::class.java)
    this.requireActivity().application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  companion object {
    private val STEP_MILLIS = TimeUnit.SECONDS.toMillis(10)
    lateinit var playListModel: SleepAssistantPlayListModel
  }
}