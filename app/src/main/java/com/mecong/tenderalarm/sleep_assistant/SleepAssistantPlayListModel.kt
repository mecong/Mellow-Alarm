package com.mecong.tenderalarm.sleep_assistant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType

class SleepAssistantPlayListModel() : ViewModel() {
    var playlist = MutableLiveData<PlayList>()

    @JvmField
    var playing = MutableLiveData<Boolean>()

    constructor(playlist: MutableLiveData<PlayList>, playing: MutableLiveData<Boolean>) : this() {
        this.playlist = playlist
        this.playing = playing
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}]
    class PlayList {
        var index: Int
        var media: List<Media>
        var mediaType: SleepMediaType

        constructor(url: String?, name: String?, mediaType: SleepMediaType) {
            media = listOf(Media(url, name))
            this.mediaType = mediaType
            index = 0
        }

        constructor(index: Int, media: List<Media>, mediaType: SleepMediaType) {
            this.index = index
            this.media = media
            this.mediaType = mediaType
        }

    }


}

data class Media(val url: String?, val title: String?)