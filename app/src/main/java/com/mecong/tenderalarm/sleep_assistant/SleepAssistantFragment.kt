package com.mecong.tenderalarm.sleep_assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PorterDuff
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.HourglassComponent.SleepTimerViewValueListener
import com.mecong.tenderalarm.sleep_assistant.RadioService.LocalBinder
import com.mecong.tenderalarm.sleep_assistant.media_selection.SoundListsPagerAdapter
import kotlinx.android.synthetic.main.content_sleep_assistant.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SleepAssistantFragment : Fragment() {
    var timeMinutes: Long = 0
    var timeMs: Long = 0
    var volume = 0f
    var volumeStep = 0f
    private val handler: Handler = Handler()
    private lateinit var runnable: Runnable
    lateinit var playListModel: SleepAssistantPlayListModel
    var serviceBound = false
    lateinit var radioService: RadioService

    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            radioService = (binder as LocalBinder).service
            serviceBound = true
            radioService.audioVolume = volume / 100
            EventBus.getDefault().post(radioService.status)

            playListModel.playlist.observe(viewLifecycleOwner, Observer {
                radioService.playMediaList(it)
            })

//            if (playListModel.playlist.value == null) {
//                val defaultPlayList = PlayList(retrieveNoises()[0].url,
//                        retrieveNoises()[0].name, SleepMediaType.NOISE)
//                playListModel.playlist.value = defaultPlayList
//            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBound = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        HyperLog.i(AlarmUtils.TAG, "Sleep assistant fragment create view")

        playListModel = ViewModelProvider(this).get(SleepAssistantPlayListModel::class.java)

        val context = this.context!!
        val dbHelper = sqLiteDBHelper(context)

        val activeTab = dbHelper!!.getPropertyInt(PropertyName.ACTIVE_TAB)
        initializeTabsAndMediaFragments(context, activeTab!!)

        val audioManager = this.activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumeCoefficient = systemVolume.toFloat() / streamMaxVolume
        volume = 105 - 100 * volumeCoefficient
        timeMinutes = 39

        slliderSleepTime.addListener(object : SleepTimerViewValueListener {
            override fun onValueChanged(newValue: Long) {
                textViewMinutes.text = context.getString(R.string.sleep_minutes, newValue)
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

        slliderSleepTime.setCurrentValue(timeMinutes)
        sliderVolume.setCurrentValue(volume.toLong())
        timeMs = TimeUnit.MINUTES.toMillis(timeMinutes)
        volumeStep = volume * STEP_MILLIS / timeMs
        textViewMinutes.text = context.getString(R.string.sleep_minutes, timeMinutes)
        textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())


        runnable = Runnable {
            timeMs -= STEP_MILLIS
            if (timeMs <= 0) {
                handler.removeCallbacks(runnable)

                if (radioService.isPlaying) {
                    radioService.stop()
                    timeMinutes = 30
                    slliderSleepTime.setCurrentValue(timeMinutes)
                    textViewMinutes.text = context.getString(R.string.sleep_minutes, timeMinutes)

                    volume = 30f
                    radioService.audioVolume = volume / 100f
                    sliderVolume.setCurrentValue(volume.toLong())
                    textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())
                }
            } else {
                timeMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
                slliderSleepTime.setCurrentValue(timeMinutes)
                textViewMinutes.text = context.getString(R.string.sleep_minutes, timeMinutes)

                volume -= volumeStep
                radioService.audioVolume = volume / 100f
                sliderVolume.setCurrentValue(volume.toLong())

                textViewVolumePercent.text = context.getString(R.string.volume_percent, volume.roundToInt())

                handler.postDelayed(runnable, STEP_MILLIS)
            }
        }

        pp_button.setOnClickListener {
            if (radioService.isPlaying) {
                radioService.pause()
            } else {
                if (radioService.hasPlayList()) {
                    radioService.resume()
                } else {
                    radioService.playMediaList(playListModel.playlist.value!!)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        bindRadioService()

        return inflater.inflate(
                R.layout.content_sleep_assistant, container, false) as ViewGroup

    }

    private fun initializeTabsAndMediaFragments(context: Context?, activeTab: Int) {
        if (context == null) return

        val soundListsPagerAdapter = SoundListsPagerAdapter(this.activity!!.supportFragmentManager, context, playListModel)
        viewPager.adapter = soundListsPagerAdapter
        tabs.setupWithViewPager(viewPager)
        tabs.setSelectedTabIndicator(null)
        tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            val instance = sqLiteDBHelper(getContext()!!)
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView!!.findViewById<View>(R.id.view).visibility = View.INVISIBLE
                tab.customView!!.background = resources.getDrawable(R.drawable.tr2_background)
                instance!!.setPropertyString(PropertyName.ACTIVE_TAB, tab.position.toString())
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView!!.findViewById<View>(R.id.view).visibility = View.VISIBLE
                tab.customView!!.background = resources.getDrawable(R.drawable.tr1_background)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                onTabSelected(tab)
            }
        })
        for (i in 0 until tabs.tabCount) {
            tabs.getTabAt(i)!!.setCustomView(R.layout.media_tab)
        }
        (tabs.getTabAt(0)?.customView?.findViewById<View>(R.id.imageView) as ImageView).setImageResource(R.drawable.local_media)
        (tabs.getTabAt(1)?.customView?.findViewById<View>(R.id.imageView) as ImageView).setImageResource(R.drawable.online_media)
        (tabs.getTabAt(2)?.customView?.findViewById<View>(R.id.imageView) as ImageView).setImageResource(R.drawable.noises)
        tabs.getTabAt(activeTab % tabs.tabCount)!!.select()
    }


    @Subscribe(sticky = true)
    fun onPlayFileChanged(media: Media) {
        nowPlayingText.text = media.title
    }

    @Subscribe(sticky = true)
    fun onEvent(status: String?) {
        pp_button.isEnabled = true
        when (status) {
            RadioService.LOADING -> {
                pp_button.setImageResource(R.drawable.pause_btn)
                pp_button.setColorFilter(0)
                handler.removeCallbacks(runnable)
            }
            RadioService.ERROR -> {
                nowPlayingText.text = getString(R.string.can_not_stream)
                Toast.makeText(this.context, getString(R.string.can_not_stream), Toast.LENGTH_SHORT).show()
                pp_button.setImageResource(R.drawable.play_btn)
                playListModel.playing.setValue(false)
            }
            RadioService.PLAYING -> {
                pp_button.setImageResource(R.drawable.pause_btn)
                //                pp_button.setPaddingRelative(10,10,10,10);
                pp_button.imageTintMode = PorterDuff.Mode.ADD
                playListModel.playing.value = true
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, STEP_MILLIS)
            }
            else -> {
                pp_button.setImageResource(R.drawable.play_btn)
                playListModel.playing.value = false
                handler.removeCallbacks(runnable)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        HyperLog.i(AlarmUtils.TAG, "SA OnStart")
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        HyperLog.i(AlarmUtils.TAG, "SA onStop")
        EventBus.getDefault().unregister(this)
    }

    override fun onPause() {
        super.onPause()
        HyperLog.i(AlarmUtils.TAG, "SA onPause")
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        handler.removeCallbacks(runnable)
        if (radioService.isPlaying) {
            radioService.stop()
        }
        if (serviceBound) {
            this.activity?.application?.unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        HyperLog.i(AlarmUtils.TAG, "SA onResume")
        val audioManager = this.activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumeCoefficient = systemVolume.toFloat() / streamMaxVolume
        volume = 105 - 100 * volumeCoefficient
    }

    private fun bindRadioService() {
        val intent = Intent(this.context, RadioService::class.java)
        this.activity!!.application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    companion object {
        private val STEP_MILLIS = TimeUnit.SECONDS.toMillis(10)
    }
}