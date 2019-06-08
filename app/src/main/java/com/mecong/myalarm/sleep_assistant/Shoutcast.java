package com.mecong.myalarm.sleep_assistant;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.mecong.myalarm.R;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class Shoutcast {

    private String name;
    @SerializedName("stream")
    private String url;

    static List<Shoutcast> retrieveShoutcasts(Context context) {
        Reader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.shoutcasts));

        return (new Gson()).fromJson(reader, new TypeToken<List<Shoutcast>>() {
        }.getType());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
