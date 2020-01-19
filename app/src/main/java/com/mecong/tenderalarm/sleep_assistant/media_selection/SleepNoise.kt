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
                    SleepNoise("Pink Noise", getNoiseUri(R.raw.pinknoise)),
                    SleepNoise("Garden Noise", getNoiseUri(R.raw.gardennoise)),
                    SleepNoise("Insects Noise", getNoiseUri(R.raw.insect_noise)),
                    SleepNoise("Fire Noise", getNoiseUri(R.raw.fire_noise)),
                    SleepNoise("Clock Noise", getNoiseUri(R.raw.clock_noise)),
                    SleepNoise("Rain Noise", getNoiseUri(R.raw.rain_noise)),
                    SleepNoise("Thunder Noise", getNoiseUri(R.raw.thunder_noise)),
                    SleepNoise("Tibet Noise 1", getNoiseUri(R.raw.tibet_noise1)),
                    SleepNoise("Tibet Noise 2", getNoiseUri(R.raw.tibet_noise2))
            )
        }

        private fun getNoiseUri(id: Int) =
                RawResourceDataSource.buildRawResourceUri(id).toString()
    }
}
