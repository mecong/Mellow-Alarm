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
                    SleepNoise("Brown Noise", getNoiseUri(R.raw._sound_brown_noise)),
                    SleepNoise("Red Noise", getNoiseUri(R.raw._sound_red_noise)),
                    SleepNoise("Insects Noise", getNoiseUri(R.raw.insect_noise)),
                    SleepNoise("Campfire Noise", getNoiseUri(R.raw.fire_noise)),
                    SleepNoise("Clock Noise", getNoiseUri(R.raw.clock_noise)),
                    SleepNoise("Thunder Noise", getNoiseUri(R.raw.thunder_noise)),
                    SleepNoise("Binaural beats", getNoiseUri(R.raw._sound_binaural_beats)),
                    SleepNoise("Crickets", getNoiseUri(R.raw._sound_crickets)),
                    SleepNoise("Fireplace", getNoiseUri(R.raw._sound_fireplace)),
                    SleepNoise("Rain", getNoiseUri(R.raw._sound_gentle_rain)),
                    SleepNoise("Space", getNoiseUri(R.raw._sound_outer_space)),
                    SleepNoise("River", getNoiseUri(R.raw._sound_river_rapids)),
                    SleepNoise("Birds", getNoiseUri(R.raw._sound_song_birds)),
                    SleepNoise("Thunder 1", getNoiseUri(R.raw._sound_thunder_heavy)),
                    SleepNoise("Thunder 2", getNoiseUri(R.raw._sound_thunderstorm)),
                    SleepNoise("Frogs", getNoiseUri(R.raw._sound_tree_frogs)),
                    SleepNoise("Ocean noise", getNoiseUri(R.raw._sound_ocean))
            )
        }

        private fun getNoiseUri(id: Int) =
                RawResourceDataSource.buildRawResourceUri(id).toString()
    }
}
