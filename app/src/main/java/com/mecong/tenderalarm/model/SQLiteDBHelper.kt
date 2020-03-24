package com.mecong.tenderalarm.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.exoplayer2.database.DatabaseProvider
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.alarm.AlarmUtils
import java.util.*
import java.util.concurrent.TimeUnit

class SQLiteDBHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), DatabaseProvider {
    private fun setDefaultOnlineMedia(database: SQLiteDatabase) {
        val values = ContentValues(2)
        values.put(TITLE, "Enigmatic radio")
        values.put(URI, "http://listen2.myradio24.com:9000/8226")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Zen noise")
        values.put(URI, "http://mynoise1.radioca.st/stream")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Space noise")
        values.put(URI, "http://mynoise5.radioca.st/stream")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "1.FM - Afterbeat Electronica Radio")
        values.put(URI, "http://strm112.1.fm/electronica_mobile_mp3")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Graal Radio")
        values.put(URI, "http://graalradio.com:8123/future")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Sleepy")
        values.put(URI, "http://ample-10.radiojar.com/rrgq7vw4gxquv?rj-ttl=5&rj-token=AAABa1lok77_iuIXolNS9qjWnD31iEM1pwmTre7whSvHzvHEjWy5yg")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Classical")
        values.put(URI, "http://mediaserv30.live-streams.nl:8088/live")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "MilanoLounge")
        values.put(URI, "http://antares.dribb.com:5080/autodj")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "Ibiza chill")
        values.put(URI, "http://edge3.peta.live365.net/b05055_128mp3?listenerId=1d6272312a62c2b159db7deeed43254b&aw_0_1st.playerid=esPlayer&aw_0_1st.skey=1567659697")
        database.insert(TABLE_ONLINE_MEDIA, null, values)
        values.put(TITLE, "JR.FM chill/Lounge Radio")
        values.put(URI, "http://149.56.157.81:5104/;stream/1")
        database.insert(TABLE_ONLINE_MEDIA, null, values)

    }

    private fun initializeDefaultProperties(database: SQLiteDatabase) {
        setPropertyString(PropertyName.ACTIVE_TAB, "2", database)
    }

    val allAlarms: Cursor
        get() = this.readableDatabase.rawQuery(SELECT_ALL_ALARMS, null)

    private fun getAlarmEntity(sql: String): AlarmEntity? {
        val cursor = this.readableDatabase.rawQuery(sql, null)
        var entity: AlarmEntity? = null
        if (cursor != null) {
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
                HyperLog.d(AlarmUtils.TAG, "Alarm added :: $entity")
                database.insert(TABLE_ALARMS, null, values)
            } else {
                database.update(TABLE_ALARMS, values, "_id=$entity.id", null)
                HyperLog.d(AlarmUtils.TAG, "Alarm updated :: $entity")
                entity.id
            }
        }
    }

    fun toggleAlarmActive(id: String, active: Boolean) {
        this.writableDatabase.use { writableDatabase ->
            val updateValues = ContentValues(1)
            updateValues.put("active", if (active) 1 else 0)
            if (!active) {
                updateValues.put("canceled_next_alarms", 0)
                updateValues.put("next_time", -1)
                updateValues.put("next_request_code", -1)
            }
            writableDatabase.update(TABLE_ALARMS, updateValues, "_id=?", arrayOf(id))
            HyperLog.v(AlarmUtils.TAG, "Alarm [id=$id] toggled to: $active")
        }
    }

    fun deleteAlarm(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_ALARMS, "_id=$id", null)
            HyperLog.v(AlarmUtils.TAG, "Alarm [id=$id] deleted")
        }
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(createAlarmTable())
        sqLiteDatabase.execSQL(createOnlineMediaResourcesTable())
        sqLiteDatabase.execSQL(createOfflineMediaResourcesTable())
        sqLiteDatabase.execSQL(createPropertiesTable())
        setDefaultOnlineMedia(sqLiteDatabase)
        initializeDefaultProperties(sqLiteDatabase)
        HyperLog.i(AlarmUtils.TAG, "Database created")
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldDbVersion: Int, newVersion: Int) {
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_ALARMS)
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_ONLINE_MEDIA)
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_OFFLINE_MEDIA)
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_PROPERTIES)
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
        return String.format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "title TEXT,"
                + "uri TEXT"
                + ")", TABLE_ONLINE_MEDIA)
    }

    private fun createOfflineMediaResourcesTable(): String {
        return String.format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "title TEXT,"
                + "uri TEXT"
                + ")", TABLE_OFFLINE_MEDIA)
    }

    private fun createPropertiesTable(): String {
        return String.format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "property_name TEXT,"
                + "property_value TEXT"
                + ")", TABLE_PROPERTIES)
    }

    fun getPropertyInt(propertyName: PropertyName): Int? {
        val propertyString = getPropertyString(propertyName) ?: return null
        return propertyString.toInt()
    }

    fun getPropertyString(propertyName: PropertyName): String? {
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
            HyperLog.v(AlarmUtils.TAG, "Property added -> $propertyName: $propertyValue")
            database.insert(TABLE_PROPERTIES, null, updateValues)
        } else {
            database.update(TABLE_PROPERTIES, updateValues, "_id=$id", null)
            HyperLog.v(AlarmUtils.TAG, "Property updated -> $propertyName: $propertyValue")
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
            HyperLog.v(AlarmUtils.TAG, "Add media URL :: $url")
            database.insert(TABLE_ONLINE_MEDIA, null, values)
        }
    }

    fun deleteOnlineMedia(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_ONLINE_MEDIA, "_id=?", arrayOf(id))
            HyperLog.v(AlarmUtils.TAG, "Online Media [id=$id] deleted")
        }
    }

    val allLocalMedia: Cursor
        get() = this.readableDatabase.rawQuery("SELECT * FROM $TABLE_OFFLINE_MEDIA",
                null)

    fun addLocalMediaUrl(url: String, title: String?) {
        this.writableDatabase.use { database ->
            val values = ContentValues()
            values.put(URI, url)
            values.put(TITLE, title)
            HyperLog.v(AlarmUtils.TAG, "Add media URL :: $url")
            database.insert(TABLE_OFFLINE_MEDIA, null, values)
        }
    }

    fun deleteLocalMedia(id: String) {
        this.writableDatabase.use { writableDatabase ->
            writableDatabase.delete(TABLE_OFFLINE_MEDIA, "_id=?", arrayOf(id))
            HyperLog.v(AlarmUtils.TAG, "Online Media [id=$id] deleted")
        }
    }

    companion object {
        private const val TITLE = "title"
        private const val URI = "uri"
        private const val DATABASE_VERSION = 26
        private const val TABLE_ALARMS = "alarms"
        private const val TABLE_ONLINE_MEDIA = "online_media"
        private const val TABLE_OFFLINE_MEDIA = "offline_media"
        private const val TABLE_PROPERTIES = "properties"
        private const val SELECT_ALL_ALARMS = "SELECT * FROM $TABLE_ALARMS"
        private const val SELECT_NEXT_ALARM = "SELECT * FROM $TABLE_ALARMS WHERE active=1 and canceled_next_alarms=0 ORDER BY next_not_canceled_time LIMIT 1"
        private const val DATABASE_NAME = "my_alarm_database"
        private const val DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS "
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