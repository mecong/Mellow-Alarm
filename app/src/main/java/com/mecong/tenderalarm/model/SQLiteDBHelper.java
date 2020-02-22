package com.mecong.tenderalarm.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.exoplayer2.database.DatabaseProvider;
import com.hypertrack.hyperlog.HyperLog;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.mecong.tenderalarm.alarm.AlarmUtils.TAG;
import static java.lang.String.format;

public class SQLiteDBHelper extends SQLiteOpenHelper implements DatabaseProvider {
    private static final String TITLE = "title";
    private static final String URI = "uri";
    private static final int DATABASE_VERSION = 25;
    private static final String TABLE_ALARMS = "alarms";
    private static final String TABLE_ONLINE_MEDIA = "online_media";
    private static final String TABLE_OFFLINE_MEDIA = "offline_media";
    private static final String TABLE_PROPERTIES = "properties";
    private static final String SELECT_ALL_ALARMS = "SELECT * FROM " + TABLE_ALARMS;
    private static final String SELECT_NEXT_ALARM = format(
            "SELECT * FROM %s WHERE active=1 and canceled_next_alarms=0 ORDER BY next_not_canceled_time LIMIT 1", TABLE_ALARMS);
    private static final String DATABASE_NAME = "my_alarm_database";
    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static SQLiteDBHelper sInstance;

    private SQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SQLiteDBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SQLiteDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private void setDefaultOnlineMedia(SQLiteDatabase database) {
        ContentValues values = new ContentValues(2);

        values.put(TITLE, "Enigmatic radio");
        values.put(URI, "http://listen2.myradio24.com:9000/8226");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put(TITLE, "Zen noise");
        values.put(URI, "http://mynoise1.radioca.st/stream");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put(TITLE, "Space noise");
        values.put(URI, "http://mynoise5.radioca.st/stream");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put(TITLE, "1.FM - Afterbeat Electronica Radio");
        values.put(URI, "http://strm112.1.fm/electronica_mobile_mp3");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put(TITLE, "Graal Radio");
        values.put(URI, "http://graalradio.com:8123/future");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put(TITLE, "Sleepy");
        values.put(URI, "http://ample-10.radiojar.com/rrgq7vw4gxquv?rj-ttl=5&rj-token=AAABa1lok77_iuIXolNS9qjWnD31iEM1pwmTre7whSvHzvHEjWy5yg");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);


        values.put(TITLE, "Classical");
        values.put(URI, "http://mediaserv30.live-streams.nl:8088/live");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);
    }

    private void initializeDefaultProperties(SQLiteDatabase database) {
        setPropertyString(PropertyName.ACTIVE_TAB, "2", database);
    }

    public Cursor getAllAlarms() {
        return this.getReadableDatabase().rawQuery(SELECT_ALL_ALARMS, null);
    }

    private AlarmEntity getAlarmEntity(String sql) {
        Cursor cursor = this.getReadableDatabase().rawQuery(sql, null);
        AlarmEntity entity = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                entity = new AlarmEntity(cursor);
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        return entity;
    }

    public AlarmEntity getNextActiveAlarm() {
        return getAlarmEntity(SELECT_NEXT_ALARM);
    }

    public AlarmEntity getAlarmById(String id) {
        return getAlarmEntity(format("SELECT * FROM %s WHERE _id=%s LIMIT 1", TABLE_ALARMS, id));
    }

    public AlarmEntity getNextMorningAlarm() {
        Calendar now = Calendar.getInstance();
        return getAlarmEntity(format(Locale.getDefault(),
                "SELECT * FROM %s WHERE active=1 " +
                        " AND tts_notification=1" +
                        " AND hour>1 AND hour<10 " +
                        " AND (next_time-%d)>%d" +
                        " ORDER BY next_time LIMIT 1", TABLE_ALARMS, now.getTimeInMillis(), TimeUnit.HOURS.toMillis(4)));
    }

    public long addOrUpdateAlarm(AlarmEntity entity) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("hour", entity.getHour());
            values.put("minute", entity.getMinute());
            values.put("message", entity.getMessage());
            values.put("days", entity.getDays());
            values.put("ticks_time", entity.getTicksTime());
            values.put("canceled_next_alarms", entity.getCanceledNextAlarms());
            values.put("active", entity.isActive());
            values.put("exact_date", entity.getExactDate());
            values.put("complexity", entity.getComplexity());
            values.put("snooze_max_times", entity.getSnoozeMaxTimes());
            values.put("melody_url", entity.getMelodyUrl());
            values.put("melody_name", entity.getMelodyName());
            values.put("next_time", entity.getNextTime());
            values.put("next_not_canceled_time", entity.getNextNotCanceledTime());
            values.put("next_request_code", entity.getNextRequestCode());
            values.put("tts_notification", entity.isTimeToSleepNotification());
            values.put("heads_up", entity.isHeadsUp());

            if (entity.getId() == 0) {
                HyperLog.d(TAG, "Alarm added :: " + entity);
                return database.insert(SQLiteDBHelper.TABLE_ALARMS, null, values);
            } else {
                database.update(TABLE_ALARMS, values, "_id=?",
                        new String[]{entity.getId() + ""});
                HyperLog.d(TAG, "Alarm updated :: " + entity);
                return entity.getId();
            }
        }
    }

    public void toggleAlarmActive(String id, boolean active) {
        try (SQLiteDatabase writableDatabase = this.getWritableDatabase()) {

            ContentValues updateValues = new ContentValues(1);
            updateValues.put("active", active ? 1 : 0);
            if (!active) {
                updateValues.put("canceled_next_alarms", 0);
                updateValues.put("next_time", -1);
                updateValues.put("next_request_code", -1);
            }
            writableDatabase.update(TABLE_ALARMS, updateValues, "_id=?", new String[]{id});
            HyperLog.v(TAG, "Alarm [id=" + id + "] toggled to: " + active);
        }
    }

    public void deleteAlarm(String id) {
        try (SQLiteDatabase writableDatabase = this.getWritableDatabase()) {
            writableDatabase.delete(TABLE_ALARMS, "_id=?", new String[]{id});
            HyperLog.v(TAG, "Alarm [id=" + id + "] deleted");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createAlarmTable());
        sqLiteDatabase.execSQL(createOnlineMediaResourcesTable());
        sqLiteDatabase.execSQL(createOfflineMediaResourcesTable());
        setDefaultOnlineMedia(sqLiteDatabase);
        sqLiteDatabase.execSQL(createPropertiesTable());
        initializeDefaultProperties(sqLiteDatabase);

        HyperLog.i(TAG, "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldDbVersion, int newVersion) {
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_ALARMS);
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_ONLINE_MEDIA);
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_OFFLINE_MEDIA);
        sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + TABLE_PROPERTIES);
        onCreate(sqLiteDatabase);
    }

    private String createAlarmTable() {

        return format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "hour TINYINT,"
                + "minute TINYINT,"
                + "complexity TINYINT,"
                + "days TINYINT,"
                + "exact_date LONG,"
                + "message TEXT,"
                + "active BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "heads_up BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "tts_notification BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "ticks_time TINYINT," // for how long before main alarm to play ticks (null - not play)
                + "melody_name TEXT,"
                + "melody_url TEXT,"
                + "vibration_type TEXT,"
                + "volume INTEGER,"
                + "canceled_next_alarms TINYINT DEFAULT 0,"
                + "snooze_max_times TINYINT,"
                + "next_time LONG,"
                + "next_not_canceled_time LONG,"
                + "next_request_code INTEGER"
                + ")", TABLE_ALARMS);
    }

    private String createOnlineMediaResourcesTable() {
        return format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "title TEXT,"
                + "uri TEXT"
                + ")", TABLE_ONLINE_MEDIA);
    }

    private String createOfflineMediaResourcesTable() {
        return format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "title TEXT,"
                + "uri TEXT"
                + ")", TABLE_OFFLINE_MEDIA);
    }

    private String createPropertiesTable() {
        return format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "property_name TEXT,"
                + "property_value TEXT"
                + ")", TABLE_PROPERTIES);
    }

    public Integer getPropertyInt(PropertyName propertyName) {
        final String propertyString = getPropertyString(propertyName);
        if (propertyString == null) return null;
        return Integer.parseInt(propertyString);
    }

    public String getPropertyString(PropertyName propertyName) {
        String sql = "SELECT property_value FROM  " + TABLE_PROPERTIES + " WHERE property_name=?";
        Cursor cursor = this.getReadableDatabase().rawQuery(sql, new String[]{propertyName.toString()});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            final String string = cursor.getString(0);
            cursor.close();
            return string;
        } else
            return null;
    }

    private void setPropertyString(PropertyName property, String propertyValue, SQLiteDatabase database) {
        String propertyName = property.toString();
        String sql = "SELECT _id, property_value FROM  " + TABLE_PROPERTIES + " WHERE property_name=?";
        Cursor cursor = database.rawQuery(sql, new String[]{propertyName});

        int id = -1;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            id = cursor.getInt(0);
        }
        cursor.close();


        ContentValues updateValues = new ContentValues(2);
        updateValues.put("property_name", propertyName);
        updateValues.put("property_value", propertyValue);


        if (id == -1) {
            HyperLog.v(TAG, "Property added -> " + propertyName + ": " + propertyValue);
            database.insert(SQLiteDBHelper.TABLE_PROPERTIES, null, updateValues);
        } else {
            database.update(TABLE_PROPERTIES, updateValues, "_id=" + id, null);
            HyperLog.v(TAG, "Property updated -> " + propertyName + ": " + propertyValue);
        }
    }

    public void setPropertyString(PropertyName property, String propertyValue) {
        try (SQLiteDatabase writableDatabase = this.getWritableDatabase()) {
            setPropertyString(property, propertyValue, writableDatabase);
        }
    }

    public Cursor getAllOnlineMedia() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_ONLINE_MEDIA,
                null);
    }

    public void addMediaUrl(String url) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(URI, url);
            HyperLog.v(TAG, "Add media URL :: " + url);
            database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);
        }
    }

    public void deleteOnlineMedia(String id) {
        try (SQLiteDatabase writableDatabase = this.getWritableDatabase()) {
            writableDatabase.delete(TABLE_ONLINE_MEDIA, "_id=?", new String[]{id});
            HyperLog.v(TAG, "Online Media [id=" + id + "] deleted");
        }
    }

    public Cursor getAllLocalMedia() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_OFFLINE_MEDIA,
                null);
    }

    public void addLocalMediaUrl(String url, String title) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(URI, url);
            values.put(TITLE, title);
            HyperLog.v(TAG, "Add media URL :: " + url);
            database.insert(SQLiteDBHelper.TABLE_OFFLINE_MEDIA, null, values);
        }
    }

    public void deleteLocalMedia(String id) {
        try (SQLiteDatabase writableDatabase = this.getWritableDatabase()) {
            writableDatabase.delete(TABLE_OFFLINE_MEDIA, "_id=?", new String[]{id});
            HyperLog.v(TAG, "Online Media [id=" + id + "] deleted");
        }
    }
}