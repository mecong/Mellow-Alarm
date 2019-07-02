package com.mecong.myalarm.model;

import android.database.Cursor;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaEntity {
    int id;
    String header;
    String uri;

    public static MediaEntity fromCursor(Cursor cursor) {
        return MediaEntity.builder()
                .id(cursor.getInt(cursor.getColumnIndex("_id")))
                .header(cursor.getString(cursor.getColumnIndex("title")))
                .uri(cursor.getString(cursor.getColumnIndex("uri")))
                .build();
    }
}
