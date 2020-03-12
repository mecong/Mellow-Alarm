package com.mecong.tenderalarm.model;

import android.database.Cursor;

public class MediaEntity {
    private int id;
    private String header;
    private String uri;

    public MediaEntity(int id, String header, String uri) {
        this.id = id;
        this.header = header;
        this.uri = uri;
    }

    public static MediaEntity fromCursor(Cursor cursor) {
        return new MediaEntity(cursor.getInt(cursor.getColumnIndex("_id")),
                cursor.getString(cursor.getColumnIndex("title")),
                cursor.getString(cursor.getColumnIndex("uri")));
    }

    public int getId() {
        return id;
    }

    public String getHeader() {
        return header;
    }

    public String getUri() {
        return uri;
    }
}
