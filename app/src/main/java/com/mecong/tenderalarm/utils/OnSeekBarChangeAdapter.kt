package com.mecong.tenderalarm.utils

import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

abstract class OnSeekBarChangeAdapter : OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}