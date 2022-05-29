package com.mecong.tenderalarm.model

import android.database.Cursor

class PlaylistEntity(val id: Long, val title: String) {

  companion object {
    @JvmStatic
    fun fromCursor(cursor: Cursor): PlaylistEntity {
      return PlaylistEntity(
        cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
        cursor.getString(cursor.getColumnIndexOrThrow("title"))
      )
    }
  }

}