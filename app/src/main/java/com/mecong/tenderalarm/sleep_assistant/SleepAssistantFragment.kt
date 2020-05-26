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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.MainActivityMessages
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.RadioService.LocalBinder
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType
import com.mecong.tenderalarm.sleep_assistant.media_selection.SoundListsPagerAdapter
import kotlinx.android.synthetic.main.content_sleep_assistant.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class SleepAssistantFragment : Fragment() {
    var timeMinutes: Long = 0
    var timeMs: Long = 0
    var volume = 0f
    var volumeStep = 0f
    private val sleepTimeHandler: Handler = Handler()
    private lateinit var sleepTimeRunnable: Runnable
    private val progressHandler: Handler = Handler()
    private lateinit var progressRunnable: Runnable
    private var showRadioBuffer = false

    var serviceBound = false
    lateinit var radioService: RadioService

    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            radioService = (binder as LocalBinder).service
            serviceBound = true
            radioService.audioVolume = volume / 100

            val dbHelper = sqLiteDBHelper(this@SleepAssistantFragment.context!!)!!
            val savedShuffle = dbHelper.getPropertyString(PropertyName.SHUFFLE)?.toBoolean() ?: false
            radioService.shuffleModeEnabled = savedShuffle

            ibPlayOrder.setImageResource(
                    if (radioService.shuffleModeEnabled)
                        R.drawable.ic_baseline_shuffle_24
                    else
                        R.drawable.ic_baseline_trending_flat_24
            )


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

        val context = this.context!!
        val dbHelper = sqLiteDBHelper(context)!!

        showRadioBuffer = dbHelper.getPropertyString(PropertyName.SHOW_RADIO_BUFFER) == "1"

        textViewMinutes.setOnLongClickListener {
            showRadioBuffer = !showRadioBuffer
            dbHelper.setPropertyString(PropertyName.SHOW_RADIO_BUFFER, if (showRadioBuffer) "1" else "0")
            true
        }

        val activeTab = dbHelper.getPropertyInt(PropertyName.ACTIVE_TAB) ?: 2
        initializeTabsAndMediaFragments(context, activeTab)

        timeMinutes = dbHelper.getPropertyLong(PropertyName.SLEEP_TIME) ?: 39
        sliderSleepTime.setCurrentValue(timeMinutes)
        textViewMinutes.text = context.resources.getQuantityString(R.plurals.n_minutes_plural, timeMinutes.toInt(), timeMinutes.toInt())

        sliderSleepTime.addListener(object : SleepTimerViewValueListener {
            override fun onValueChanged(newValue: Long) {
                dbHelper.setPropertyString(PropertyName.SLEEP_TIME, newValue.toString())
                textViewMinutes.text = context.resources.getQuantityString(R.plurals.n_minutes_plural, newValue.toInt(), newValue.toInt())
                timeMinutes = newValue
                timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)
                volumeStep = volume * STEP_MILLIS / timeMs
            }
        })

        sliderVolume.addListener(object : SleepTimerViewValueListener {
            override fun onValueChanged(newValue: Long) {
                textViewVolumePercent!!.text = context.getString(R.string.volume_percent, newValue)
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
                    sliderSleepTime.setCurrentValue(timeMinutes)
                    timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)

                    textViewMinutes.text = context.resources.getQuantityString(R.plurals.n_minutes_plural, timeMinutes.toInt(), timeMinutes.toInt())

                    volume = 30f
                    sliderVolume.setCurrentValue(volume.toLong())
                    textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())
                    radioService.audioVolume = volume / 100f

                    volumeStep = volume * STEP_MILLIS / timeMs
                }
            } else {
                timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
                sliderSleepTime.setCurrentValue(timeMinutes)
                textViewMinutes.text = context.resources.getQuantityString(R.plurals.n_minutes_plural, timeMinutes.toInt(), timeMinutes.toInt())

                volume -= volumeStep
                radioService.audioVolume = volume / 100f
                sliderVolume.setCurrentValue(volume.toLong())

                textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())

                sleepTimeHandler.postDelayed(sleepTimeRunnable, STEP_MILLIS)
            }
        }


        nowPlayingText.setOnClickListener {
            val currentTab = dbHelper.getPropertyInt(PropertyName.ACTIVE_TAB) ?: 2
            tabs.getTabAt(currentTab % tabs.tabCount)!!.select()
        }

        ibPlayOrder.setOnClickListener {
            radioService.shuffleModeEnabled = !radioService.shuffleModeEnabled

            ibPlayOrder.setImageResource(
                    if (radioService.shuffleModeEnabled) {
                        dbHelper.setPropertyString(PropertyName.SHUFFLE, "true")
                        R.drawable.ic_baseline_shuffle_24
                    } else {
                        dbHelper.setPropertyString(PropertyName.SHUFFLE, "false")
                        R.drawable.ic_baseline_trending_flat_24
                    }
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindRadioService()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(
                R.layout.content_sleep_assistant, container, false) as ViewGroup
    }

    private fun initializeTabsAndMediaFragments(context: Context?, activeTab: Int) {
        if (context == null) return

        val soundListsPagerAdapter = SoundListsPagerAdapter(this.activity?.supportFragmentManager, context)
        viewPager.adapter = soundListsPagerAdapter
        tabs.setupWithViewPager(viewPager)
        tabs.setSelectedTabIndicator(null)

        tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.findViewById<View>(R.id.blackLine)?.visibility = View.INVISIBLE
                tab.customView?.background = resources.getDrawable(R.drawable.tr2_background, null)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.findViewById<View>(R.id.blackLine)?.visibility = View.VISIBLE
                tab.customView?.background = resources.getDrawable(R.drawable.tr1_background, null)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                onTabSelected(tab)
            }
        })

        for (i in 0 until tabs.tabCount) {
            tabs.getTabAt(i)?.setCustomView(R.layout.media_tab)
        }

        (tabs.getTabAt(0)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
                .setImageResource(R.drawable.local_media)
        (tabs.getTabAt(1)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
                .setImageResource(R.drawable.online_media)
        (tabs.getTabAt(2)?.customView?.findViewById<View>(R.id.imageView) as ImageView)
                .setImageResource(R.drawable.noises)

        tabs.getTabAt(activeTab % tabs.tabCount)!!.select()
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
                nowPlayingText.text = getString(R.string.can_not_stream)
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
            if (volumeCoefficient < 0.3f || volumeCoefficient > 0.7f) {
                volumeCoefficient = 0.32f
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, (streamMaxVolume * volumeCoefficient).roundToInt(), 0)
                Toast.makeText(this.activity!!, context!!.getString(R.string.system_volume_toast), Toast.LENGTH_SHORT).show()
            }

            volume = 105 - 100 * volumeCoefficient
            volume = min(volume, 100f)

            sliderVolume.setCurrentValue(volume.toLong())
            timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)
            volumeStep = volume * STEP_MILLIS / timeMs
            textViewVolumePercent.text = this.activity?.getString(R.string.volume_percent, volume.roundToInt())
        }
    }

    private fun startTimelineWatcher() {
        val localFileProgress = Runnable {
            Timber.v("Local: Buffered position %d, content %d", radioService.getBufferedPos(), radioService.getContentPos())
            nowPlayingText?.setPlayPosition(radioService.getContentPos() / radioService.getContentDuration().toFloat())

            playerTime1?.text = getElapsedTime()
            playerTime2?.text = getTimeLeft()

            if (radioService.isPlaying) {
                progressHandler.postDelayed(progressRunnable, 300)
            }
        }

        val onlineStreamProgress = Runnable {
            Timber.v("Online: Buffered position %d, content %d", radioService.getBufferedPos(), radioService.getContentPos())

            playerTime1?.text = getElapsedTime()
            playerTime2?.text = getBufferedSize()

            if (radioService.sleepAssistantPlayList?.mediaType == SleepMediaType.ONLINE) {
                progressHandler.postDelayed(progressRunnable, 800)
            }
        }

        progressHandler.removeCallbacksAndMessages(null)
        when (radioService.sleepAssistantPlayList?.mediaType) {
            SleepMediaType.LOCAL -> {
                progressRunnable = localFileProgress
                progressHandler.post(progressRunnable)
                playerTime2.visibility = View.VISIBLE
                playerTime1.visibility = View.VISIBLE
                ibPlayOrder.visibility = View.VISIBLE
            }
            SleepMediaType.ONLINE -> {
                if (showRadioBuffer) {
                    progressRunnable = onlineStreamProgress
                    progressHandler.post(progressRunnable)
                    playerTime2.visibility = View.VISIBLE
                    playerTime1.visibility = View.VISIBLE
                } else {
                    playerTime2.visibility = View.GONE
                    playerTime1.visibility = View.GONE
                }
                ibPlayOrder.visibility = View.INVISIBLE
            }
            else -> {
                playerTime2.visibility = View.GONE
                playerTime1.visibility = View.GONE
                ibPlayOrder.visibility = View.INVISIBLE

            }
        }
    }

    private fun getStringForTime(timeMs: Long, negative: Boolean): String? {
        val totalSeconds = if (timeMs < 0) {
            0
        } else {
            (timeMs + 500) / 1000
        }

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


    fun getElapsedTime(percentPos: Float? = null): CharSequence? {
        return if (percentPos == null) {
            getStringForTime(radioService.getContentPos(), false)
        } else {
            getStringForTime((percentPos * radioService.getContentDuration()).roundToLong(), false)
        }
    }

    fun getTimeLeft(percentPos: Float? = null): CharSequence? {
        return if (percentPos == null) {
            getStringForTime(radioService.getContentDuration() - radioService.getContentPos(), true)
        } else {
            getStringForTime(radioService.getContentDuration() - (percentPos * radioService.getContentDuration()).roundToLong(), true)
        }
    }

    fun getBufferedSize(): CharSequence? {
        val buff = radioService.getBufferedPos() - radioService.getContentPos()
        return getStringForTime(buff, true)
    }


    @Subscribe(sticky = true)
    fun onPlayFileChanged(playList: SleepAssistantPlayListActive) {
        nowPlayingText.interactiveMode = playList.mediaType == SleepMediaType.LOCAL
        radioService.setMediaList(playList)
        playListModel.playlist.value = playList
        radioService.play()
    }

    @Subscribe(sticky = true)
    fun onPlayFileChanged(playList: SleepAssistantPlayListIdle) {
        playListModel.playlist.value = playList
        nowPlayingText?.interactiveMode = playList.mediaType == SleepMediaType.LOCAL

        radioService.setMediaList(playList)
    }

    @Subscribe
    fun onPlayPosChanged(playPosition: PlayPosition) {
        if (playPosition.final) {
            radioService.seekToPercent(playPosition.playPositionPercent)
            progressHandler.post(progressRunnable)
        } else {
            progressHandler.removeCallbacks(progressRunnable)
        }

        playerTime2.text = getTimeLeft(playPosition.playPositionPercent)
        playerTime1.text = getElapsedTime(playPosition.playPositionPercent)
    }

    @Subscribe
    fun onPlayFileChanged(media: Media) {
        nowPlayingText?.text = media.title ?: ""
    }

    @Subscribe
    fun persistMediaPosition(playList: SleepAssistantPlayList) {
        if (this.context == null) return

        val dbHelper = sqLiteDBHelper(this.context!!)!!

        val activeTab = when (playList.mediaType) {
            SleepMediaType.LOCAL -> "0"
            SleepMediaType.ONLINE -> "1"
            SleepMediaType.NOISE -> "2"
        }

        dbHelper.setPropertyString(PropertyName.ACTIVE_TAB, activeTab)
        dbHelper.setPropertyString(PropertyName.TRACK_NUMBER, playList.index.toString())
        dbHelper.setPropertyString(PropertyName.PLAYLIST_ID, playList.playListId.toString())
    }

    private fun bindRadioService() {
        val intent = Intent(this.context, RadioService::class.java)
        this.activity!!.application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    companion object {
        private val STEP_MILLIS = TimeUnit.SECONDS.toMillis(10)
        lateinit var playListModel: SleepAssistantPlayListModel
    }
}