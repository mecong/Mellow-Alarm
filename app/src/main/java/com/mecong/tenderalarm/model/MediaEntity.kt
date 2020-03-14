package com.mecong.tenderalarm.model

import android.database.Cursor

class MediaEntity(val id: Int, val header: String, val uri: String) {

    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor): MediaEntity {
            return MediaEntity(cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("uri")))
        }
    }

}