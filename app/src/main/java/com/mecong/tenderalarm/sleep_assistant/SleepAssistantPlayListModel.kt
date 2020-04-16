package com.mecong.tenderalarm.sleep_assistant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType

class SleepAssistantPlayListModel() : ViewModel() {
    var playlist = MutableLiveData<SleepAssistantPlayList>()

    var playing = MutableLiveData<Boolean>()

    constructor(playlist: MutableLiveData<SleepAssistantPlayList>, playing: MutableLiveData<Boolean>) : this() {
        this.playlist = playlist
        this.playing = playing
    }
}


data class Media(val url: String?, val title: String?)

class SleepAssistantPlayListActive(index: Int, media: List<Media>, mediaType: SleepMediaType, playListId: Long) :
        SleepAssistantPlayList(index, media, mediaType, playListId)

class SleepAssistantPlayListIdle(index: Int, media: List<Media>, mediaType: SleepMediaType, playListId: Long) :
        SleepAssistantPlayList(index, media, mediaType, playListId)

open class SleepAssistantPlayList(var index: Int, val media: List<Media>, val mediaType: SleepMediaType, val playListId: Long) {
    constructor(url: String?, name: String?, mediaType: SleepMediaType) : this(0, listOf(Media(url, name)), mediaType, -1)
}
