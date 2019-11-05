package com.mecong.tenderalarm.sleep_assistant.media_selection

import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.mecong.tenderalarm.R

class SleepNoise(
        val name: String,
        val url: String) {

    companion object {
        fun retrieveNoises(): List<SleepNoise> {
            return listOf(
                    SleepNoise("White Noise", getNoiseUri(R.raw.whitenoisegaussian)),
                    SleepNoise("Pink Noise", getNoiseUri(R.raw.pinknoise)))
        }

        private fun getNoiseUri(id: Int) =
                RawResourceDataSource.buildRawResourceUri(id).toString()
    }
}
