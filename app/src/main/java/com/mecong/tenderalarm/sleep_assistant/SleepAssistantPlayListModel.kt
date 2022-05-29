package com.mecong.tenderalarm.sleep_assistant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType

class SleepAssistantPlayListModel : ViewModel() {
  var playlist = MutableLiveData<SleepAssistantPlayList>()

  var playing = MutableLiveData<Boolean>()
}


data class Media(val url: String, val title: String)

class SleepAssistantPlayListActive(index: Int, media: List<Media>, mediaType: SleepMediaType, playListId: Long) :
  SleepAssistantPlayList(index, media, mediaType, playListId)

class SleepAssistantPlayListIdle(index: Int, media: List<Media>, mediaType: SleepMediaType, playListId: Long) :
  SleepAssistantPlayList(index, media, mediaType, playListId)

open class SleepAssistantPlayList(
  var index: Int,
  val media: List<Media>,
  val mediaType: SleepMediaType,
  val playListId: Long
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as SleepAssistantPlayList
    if (index != other.index) return false
    if (mediaType != other.mediaType) return false
    if (playListId != other.playListId) return false
    return true
  }

  override fun hashCode(): Int {
    var result = index
    result = 31 * result + media.hashCode()
    result = 31 * result + mediaType.hashCode()
    result = 31 * result + playListId.hashCode()
    return result
  }
}
