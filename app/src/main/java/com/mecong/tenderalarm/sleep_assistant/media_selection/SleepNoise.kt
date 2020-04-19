package com.mecong.tenderalarm.sleep_assistant.media_selection

import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.Media

class SleepNoise {

    companion object {
        fun retrieveNoises(): List<Media> {
            return listOf(
                    Media(getNoiseUri(R.raw._sound_ocean), "Ocean noise"),
                    Media(getNoiseUri(R.raw.fire_noise), "Campfire Noise"),
                    Media(getNoiseUri(R.raw._sound_fireplace), "Fireplace"),
                    Media(getNoiseUri(R.raw._sound_song_birds), "Birds"),
                    Media(getNoiseUri(R.raw._sound_river_rapids), "River"),
                    Media(getNoiseUri(R.raw._sound_gentle_rain), "Rain"),
                    Media(getNoiseUri(R.raw.gardennoise), "Garden Noise"),
                    Media(getNoiseUri(R.raw.clock_noise), "Clock Noise"),
                    Media(getNoiseUri(R.raw._sound_thunder_heavy), "Thunder Noise 1"),
                    Media(getNoiseUri(R.raw._sound_thunderstorm), "Thunder Noise 2"),
                    Media(getNoiseUri(R.raw.thunder_noise), "Thunder Noise 3"),
                    Media(getNoiseUri(R.raw._sound_crickets), "Crickets"),
                    Media(getNoiseUri(R.raw._sound_tree_frogs), "Frogs"),
                    Media(getNoiseUri(R.raw.insect_noise), "Insects Noise"),
                    Media(getNoiseUri(R.raw._sound_outer_space), "Space"),
                    Media(getNoiseUri(R.raw.whitenoisegaussian), "White Noise"),
                    Media(getNoiseUri(R.raw.pinknoise), "Pink Noise"),
                    Media(getNoiseUri(R.raw._sound_brown_noise), "Brown Noise"),
                    Media(getNoiseUri(R.raw._sound_red_noise), "Red Noise")
            )
        }

        private fun getNoiseUri(id: Int) =
                RawResourceDataSource.buildRawResourceUri(id).toString()
    }
}
