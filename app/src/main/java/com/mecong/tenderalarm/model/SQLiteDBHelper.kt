package com.mecong.tenderalarm.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.exoplayer2.database.DatabaseProvider
import java.util.*
import java.util.concurrent.TimeUnit

class SQLiteDBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), DatabaseProvider {
    private fun setDefaultOnlineMedia(database: SQLiteDatabase) {
        val values = ContentValues(2)

        values.put(TITLE, "Enigmatic radio")
        values.put(URI, "http://listen2.myradio24.com:9000/8226")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

        values.put(TITLE, "Milano Lounge")
        values.put(URI, "https://securestreams.autopo.st:1180/stream")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

        values.put(TITLE, "Zen noise")
        values.put(URI, "http://mynoise1.radioca.st/stream")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

        values.put(TITLE, "Space noise")
        values.put(URI, "http://mynoise5.radioca.st/stream")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

        values.put(TITLE, "Classical")
        values.put(URI, "http://mediaserv30.live-streams.nl:8088/live")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

        values.put(TITLE, "JR.FM Chill/Lounge Radio")
        values.put(URI, "http://149.56.157.81:5104/;stream/1")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
    }

    private fun initializeDefaultProperties(database: SQLiteDatabase) {
        setPropertyString(PropertyName.ACTIVE_TAB, "2", database)
        setPropertyString(PropertyName.TRACK_POSITION, "0", database)
        setPropertyString(PropertyName.AUTOSTART_TURNED_ON, "0", database)
    }

    val allAlarms: Cursor
        get() = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_ALARMS", null)

    private fun getAlarmEntity(sql: String): AlarmEntity? {
        val cursor = this.readableDatabase.rawQuery(sql, null)
        var entity: AlarmEntity? = null
        if (cursor != null && !cursor.isClosed) {
            if (cursor.moveToFirst()) {
                entity = AlarmEntity(cursor)
            }
            if (!cursor.isClosed) {
                cursor.close()
            }
        }
        return entity
    }

    val nextActiveAlarm: AlarmEntity?
        get() = getAlarmEntity(SELECT_NEXT_ALARM)

    fun getAlarmById(id: String?): AlarmEntity? {
        return getAlarmEntity("SELECT * FROM $TABLE_ALARMS WHERE _id=$id LIMIT 1")
    }

    val nextMorningAlarm: AlarmEntity?
        get() {
            val now = Calendar.getInstance()
            return getAlarmEntity(String.format(Locale.getDefault(),
                    "SELECT * FROM %s WHERE active=1 " +
                            " AND tts_notification=1" +
                            " AND hour>1 AND hour<10 " +
                            " AND (next_time-%d)>%d" +
                            " ORDER BY next_time LIMIT 1", TABLE_ALARMS, now.timeInMillis, TimeUnit.HOURS.toMillis(4)))
        }

    fun addOrUpdateAlarm(entity: AlarmEntity): Long {
        this.writableDatabase.use { database ->
            val values = ContentValues().apply {
                put("hour", entity.hour)
                put("minute", entity.minute)
                put("message", entity.message)
                put("days", entity.days)
                put("ticks_time", entity.ticksTime)
                put("ticks_type", entity.ticksType)
                put("canceled_next_alarms", entity.canceledNextAlarms)
                put("active", entity.isActive)
                put("exact_date", entity.exactDate)
                put("complexity", entity.complexity)
                put("snooze_max_times", entity.snoozeMaxTimes)
                put("melody_url", entity.melodyUrl)
                put("melody_name", entity.melodyName)
                put("next_time", entity.nextTime)
                put("next_not_canceled_time", entity.nextNotCanceledTime)
                put("next_request_code", entity.nextRequestCode)
                put("tts_notification", entity.isTimeToSleepNotification)
                put("heads_up", entity.isHeadsUp)
            }

            return if (entity.id == 0L) {
                //HyperLog.d(AlarmUtils.TAG, "Alarm added :: $entity")
                database.insert(TABLE_ALARMS, null, values)
            } else {
                database.update(TABLE_ALARMS, values, "_id=${entity.id}", null)
                //HyperLog.d(AlarmUtils.TAG, "Alarm updated :: $entity")
                entity.id
            }
        }
    }

    fun toggleAlarmActive(id: String, active: Boolean) {
        this.writableDatabase.use { writableDatabase ->
            val updateValues = ContentValues(1)
            updateValues.put("active", if (active) 1 else 0)
            updateValues.put("canceled_next_alarms", 0)
            if (!active) {
                updateValues.put("next_time", -1)
            }
            writableDatabase.update(TABLE_ALARMS, updateValues, "_id=?", arrayOf(id))
            //HyperLog.v(AlarmUtils.TAG, "Alarm [id=$id] toggled to: $active")
        }
    }

    fun deleteAlarm(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_ALARMS, "_id=$id", null)
            //HyperLog.v(AlarmUtils.TAG, "Alarm [id=$id] deleted")
        }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(createAlarmTable())
        sqLiteDatabase.execSQL(createOnlineMediaResourcesTable())
        sqLiteDatabase.execSQL(createOfflineMediaResourcesTable())
        sqLiteDatabase.execSQL(createOfflineMediaPlaylistsTable())
        sqLiteDatabase.execSQL(createPropertiesTable())
        setDefaultOnlineMedia(sqLiteDatabase)
        initializeDefaultProperties(sqLiteDatabase)
        //HyperLog.i(AlarmUtils.TAG, "Database created")
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldDbVersion: Int, newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_ONLINE_MEDIA")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_OFFLINE_MEDIA")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_PROPERTIES")
        onCreate(sqLiteDatabase)
    }

    private fun createAlarmTable(): String {
        return "CREATE TABLE $TABLE_ALARMS (" +
                "_id INTEGER PRIMARY KEY," +
                "hour TINYINT," +
                "minute TINYINT," +
                "complexity TINYINT," +
                "days TINYINT," +
                "exact_date LONG," +
                "message TEXT," +
                "active BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1," +
                "heads_up BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1," +
                "tts_notification BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1," +
                "ticks_time TINYINT," +
                "ticks_type INT," +
                "melody_name TEXT," +
                "melody_url TEXT," +
                "vibration_type TEXT," +
                "volume INTEGER," +
                "canceled_next_alarms TINYINT DEFAULT 0," +
                "snooze_max_times TINYINT," +
                "next_time LONG," +
                "next_not_canceled_time LONG," +
                "next_request_code INTEGER" +
                ")"
    }

    private fun createOnlineMediaResourcesTable(): String {
        return "CREATE TABLE $TABLE_ONLINE_MEDIA (_id INTEGER PRIMARY KEY, title TEXT, uri TEXT)"
    }

    private fun createOfflineMediaResourcesTable(): String {
        return "CREATE TABLE $TABLE_OFFLINE_MEDIA (_id INTEGER PRIMARY KEY, playlist_id INTEGER, title TEXT, uri TEXT)"
    }

    private fun createOfflineMediaPlaylistsTable(): String {
        return "CREATE TABLE $TABLE_PLAYLISTS (_id INTEGER PRIMARY KEY, title TEXT)"
    }

    private fun createPropertiesTable(): String {
        return "CREATE TABLE $TABLE_PROPERTIES (_id INTEGER PRIMARY KEY, property_name TEXT, property_value TEXT)"
    }

    fun getPropertyInt(propertyName: PropertyName): Int? {
        return getPropertyString(propertyName)?.toInt()
    }

    fun getPropertyLong(propertyName: PropertyName): Long? {
        return getPropertyString(propertyName)?.toLong()
    }

    private fun getPropertyString(propertyName: PropertyName): String? {
        val sql = "SELECT property_value FROM  $TABLE_PROPERTIES WHERE property_name=?"
        val cursor = this.readableDatabase.rawQuery(sql, arrayOf(propertyName.toString()))
        return if (cursor.count > 0) {
            cursor.moveToFirst()
            val string = cursor.getString(0)
            cursor.close()
            string
        } else null
    }

    private fun setPropertyString(property: PropertyName, propertyValue: String, database: SQLiteDatabase) {
        val propertyName = property.toString()
        val sql = "SELECT _id, property_value FROM  $TABLE_PROPERTIES WHERE property_name=?"
        val cursor = database.rawQuery(sql, arrayOf(propertyName))
        var id = -1
        if (cursor.count > 0) {
            cursor.moveToFirst()
            id = cursor.getInt(0)
        }
        cursor.close()
        val updateValues = ContentValues(2)
        updateValues.put("property_name", propertyName)
        updateValues.put("property_value", propertyValue)
        if (id == -1) {
            //HyperLog.v(AlarmUtils.TAG, "Property added -> $propertyName: $propertyValue")
            database.insert(TABLE_PROPERTIES, null, updateValues)
        } else {
            database.update(TABLE_PROPERTIES, updateValues, "_id=$id", null)
            //HyperLog.v(AlarmUtils.TAG, "Property updated -> $propertyName: $propertyValue")
        }
    }

    fun setPropertyString(property: PropertyName, propertyValue: String) {
        this.writableDatabase.use { writableDatabase -> setPropertyString(property, propertyValue, writableDatabase) }
    }

    val allOnlineMedia: Cursor
        get() = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_ONLINE_MEDIA", null)

    fun addMediaUrl(url: String) {
        this.writableDatabase.use { database ->
            val values = ContentValues()
            values.put(URI, url)
            //HyperLog.v(AlarmUtils.TAG, "Add media URL :: $url")
            database.insert(TABLE_ONLINE_MEDIA, null, values)
        }
    }

    fun deleteOnlineMedia(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_ONLINE_MEDIA, "_id=?", arrayOf(id))
            //HyperLog.v(AlarmUtils.TAG, "Online Media [id=$id] deleted")
        }
    }

    //---------------------------------------
    fun getLocalMedia(playlistId: Long): Cursor {
        return this.readableDatabase.rawQuery("SELECT * FROM $TABLE_OFFLINE_MEDIA WHERE playlist_id=$playlistId", null)
    }


    fun addLocalMediaUrl(playlistId: Long, url: String, title: String?) {
        this.writableDatabase.use { database ->
            val values = ContentValues()
            values.put(URI, url)
            values.put(PLAYLIST_ID, playlistId)
            values.put(TITLE, title)
            //HyperLog.v(AlarmUtils.TAG, "Add media URL :: $url to playlist $playlistId")
            database.insert(TABLE_OFFLINE_MEDIA, null, values)
        }
    }


    fun deleteLocalMedia(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_OFFLINE_MEDIA, "_id=$id", null)
            //HyperLog.v(AlarmUtils.TAG, "Online Media [id=$id] deleted")
        }
    }
    //-----------------------------------------

    fun getAllPlaylists(): Cursor {
        return this.readableDatabase.rawQuery("SELECT * FROM $TABLE_PLAYLISTS", null)
    }

    fun addPlaylist(title: String): Long {
        this.writableDatabase.use { database ->
            val values = ContentValues()
            values.put(TITLE, title)
            //HyperLog.v(AlarmUtils.TAG, "Add playlist :: $title")
            return@addPlaylist database.insert(TABLE_PLAYLISTS, null, values)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        this.writableDatabase.use { writableDatabase ->
            val contentValues = ContentValues()
            contentValues.put("title", newName)
            writableDatabase.update(TABLE_PLAYLISTS, contentValues, "_id=$id", null)
        }
    }

    fun deletePlaylist(id: Long) {
        this.writableDatabase.use { writableDatabase ->
            val deleted = writableDatabase.delete(TABLE_OFFLINE_MEDIA, "playlist_id=$id", null)
            //HyperLog.v(AlarmUtils.TAG, "Online Media with [playlist_id=$id] deleted $deleted row")

            writableDatabase.delete(TABLE_PLAYLISTS, "_id=$id", null)
            //HyperLog.v(AlarmUtils.TAG, "Playlist [id=$id] deleted")
        }
    }


    companion object {
        private const val TITLE = "title"
        private const val URI = "uri"
        private const val PLAYLIST_ID = "playlist_id"
        private const val DATABASE_VERSION = 51
        private const val TABLE_ALARMS = "alarms"
        private const val TABLE_ONLINE_MEDIA = "online_media"
        private const val TABLE_OFFLINE_MEDIA = "offline_media"
        private const val TABLE_PLAYLISTS = "offline_media_playlists"
        private const val TABLE_PROPERTIES = "properties"
        private const val SELECT_NEXT_ALARM = "SELECT * FROM $TABLE_ALARMS WHERE active=1 ORDER BY next_not_canceled_time LIMIT 1"
        private const val DATABASE_NAME = "my_alarm_database"

        private var sInstance: SQLiteDBHelper? = null

        @JvmStatic
        @Synchronized
        fun sqLiteDBHelper(context: Context): SQLiteDBHelper? {
            if (sInstance == null) {
                sInstance = SQLiteDBHelper(context)
            }
            return sInstance
        }
    }
}