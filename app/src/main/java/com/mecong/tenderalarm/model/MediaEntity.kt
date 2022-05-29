package com.mecong.tenderalarm.model

import android.database.Cursor

class MediaEntity(val id: Int, val header: String, val uri: String) {

  companion object {
    @JvmStatic
    fun fromCursor(cursor: Cursor): MediaEntity {
      return MediaEntity(
        cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
        cursor.getString(cursor.getColumnIndexOrThrow("title")),
        cursor.getString(cursor.getColumnIndexOrThrow("uri"))
      )
    }
  }

}