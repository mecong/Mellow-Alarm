package com.mecong.myalarm.sleep_assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mecong.myalarm.sleep_assistant.media_selection.SleepMediaType;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SleepAssistantViewModel extends ViewModel {
    MutableLiveData<PlayList> playlist = new MutableLiveData<>();
    MutableLiveData<Boolean> playing = new MutableLiveData<>();

    LiveData<PlayList> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(PlayList playlist) {
        this.playlist.setValue(playlist);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class PlayList {
        List<String> urls;
        int index;
        SleepMediaType mediaType;

        public PlayList(String url, SleepMediaType mediaType) {
            this.urls = Collections.singletonList(url);
            index = 0;
            this.mediaType = mediaType;
        }
    }
}
