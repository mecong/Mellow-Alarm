package com.mecong.myalarm.sleep_assistant;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.mecong.myalarm.R;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class Noises {

    private String name;
    @SerializedName("stream")
    private String url;

    static List<Noises> retrieveNoises(Context context) {
        Reader reader = new InputStreamReader(context.getResources().openRawResource(R.raw.noises));

        return (new Gson()).fromJson(reader, new TypeToken<List<Noises>>() {
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

}
