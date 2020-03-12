package com.mecong.tenderalarm.sleep_assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mecong.tenderalarm.sleep_assistant.media_selection.SleepMediaType;

import java.util.Collections;
import java.util.List;


public class SleepAssistantPlayListModel extends ViewModel {
    MutableLiveData<PlayList> playlist = new MutableLiveData<>();
    MutableLiveData<Boolean> playing = new MutableLiveData<>();

    LiveData<PlayList> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(PlayList playlist) {
        this.playlist.setValue(playlist);
    }

    public SleepAssistantPlayListModel(MutableLiveData<PlayList> playlist, MutableLiveData<Boolean> playing) {
        this.playlist = playlist;
        this.playing = playing;
    }

    public SleepAssistantPlayListModel() {
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}]


    public static class PlayList {
        int index;
        List<Media> media;
        SleepMediaType mediaType;

        public PlayList(String url, String name, SleepMediaType mediaType) {
            this.media = Collections.singletonList(new Media(url, name));
            this.mediaType = mediaType;
            index = 0;
        }

        public PlayList(int index, List<Media> media, SleepMediaType mediaType) {
            this.index = index;
            this.media = media;
            this.mediaType = mediaType;
        }

        public int getIndex() {
            return index;
        }

        public List<Media> getMedia() {
            return media;
        }

        public SleepMediaType getMediaType() {
            return mediaType;
        }
    }


    public static class Media {
        private String url;
        private String title;

        public Media(String url, String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }
    }
}
