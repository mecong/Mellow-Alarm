package com.mecong.tenderalarm.sleep_assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType;

import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
public class SleepAssistantPlayListModel extends ViewModel {
    MutableLiveData<PlayList> playlist = new MutableLiveData<>();
    MutableLiveData<Boolean> playing = new MutableLiveData<>();

    LiveData<PlayList> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(PlayList playlist) {
        this.playlist.setValue(playlist);
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}]

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PlayList {
        int index;
        List<Media> media;
        SleepMediaType mediaType;

        public PlayList(String url, String name, SleepMediaType mediaType) {
            this.media = Collections.singletonList(new Media(url, name));
            this.mediaType = mediaType;
            index = 0;
        }
    }


    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Media {
        String url;
        String title;
    }
}
