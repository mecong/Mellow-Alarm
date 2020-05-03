package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.Media

class SleepNoise {

    companion object {
        fun retrieveNoises(context: Context): List<Media> {
            return listOf(
                    Media(getNoiseUri(R.raw._sound_ocean), context.getString(R.string.Ocean_noise)),
                    Media(getNoiseUri(R.raw.fire_noise), context.getString(R.string.Campfire_noise)),
                    Media(getNoiseUri(R.raw._sound_fireplace), context.getString(R.string.Fireplace_noise)),
                    Media(getNoiseUri(R.raw._sound_song_birds), context.getString(R.string.Birds_noise)),
                    Media(getNoiseUri(R.raw._sound_river_rapids), context.getString(R.string.River_noise)),
                    Media(getNoiseUri(R.raw._sound_gentle_rain), context.getString(R.string.Rain_noise)),
                    Media(getNoiseUri(R.raw.gardennoise), context.getString(R.string.Garden_noise)),
                    Media(getNoiseUri(R.raw.clock_noise), context.getString(R.string.Clock_noise)),
                    Media(getNoiseUri(R.raw._sound_thunder_heavy), context.getString(R.string.Thunder_noise_1)),
                    Media(getNoiseUri(R.raw._sound_thunderstorm), context.getString(R.string.Thunder_noise_2)),
                    Media(getNoiseUri(R.raw.thunder_noise), context.getString(R.string.Thunder_noise_3)),
                    Media(getNoiseUri(R.raw._sound_crickets), context.getString(R.string.Crickets_noise)),
                    Media(getNoiseUri(R.raw._sound_tree_frogs), context.getString(R.string.Frogs_noise)),
                    Media(getNoiseUri(R.raw.insect_noise), context.getString(R.string.Insects_noise)),
                    Media(getNoiseUri(R.raw._sound_outer_space), context.getString(R.string.Space_noise)),
                    Media(getNoiseUri(R.raw.whitenoisegaussian), context.getString(R.string.White_noise)),
                    Media(getNoiseUri(R.raw.pinknoise), context.getString(R.string.Pink_noise)),
                    Media(getNoiseUri(R.raw._sound_brown_noise), context.getString(R.string.Brown_noise)),
                    Media(getNoiseUri(R.raw._sound_red_noise), context.getString(R.string.Red_noise))
            )
        }

        private fun getNoiseUri(id: Int) =
                RawResourceDataSource.buildRawResourceUri(id).toString()
    }
}
