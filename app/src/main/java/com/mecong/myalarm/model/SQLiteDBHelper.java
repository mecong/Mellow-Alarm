package com.mecong.myalarm.model;

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

import static com.mecong.myalarm.alarm.AlarmUtils.TAG;
import static java.lang.String.format;

public class SQLiteDBHelper extends SQLiteOpenHelper implements DatabaseProvider {
    private static final int DATABASE_VERSION = 7;
    private static final String TABLE_ALARMS = "alarms";
    private static final String TABLE_ONLINE_MEDIA = "online_media";
    private static final String TABLE_OFFLINE_MEDIA = "offline_media";
    private static final String SELECT_ALL_ALARMS = "SELECT * FROM " + TABLE_ALARMS;
    private static final String SELECT_NEXT_ALARM = format(
            "SELECT * FROM %s WHERE active=1 ORDER BY next_time LIMIT 1", TABLE_ALARMS);
    private static final String DATABASE_NAME = "my_alarm_database";
    private static volatile SQLiteDBHelper sInstance;

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

        values.put("title", "Enigmatic radio");
        values.put("uri", "http://listen2.myradio24.com:9000/8226");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Zen noise");
        values.put("uri", "http://mynoise1.radioca.st/stream");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Space noise");
        values.put("uri", "http://mynoise5.radioca.st/stream");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "magic80");
        values.put("uri", "http://strm112.1.fm/magic80_mobile_mp3");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "1.FM - Afterbeat Electronica Radio");
        values.put("uri", "http://strm112.1.fm/electronica_mobile_mp3");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Graal Radio");
        values.put("uri", "http://graalradio.com:8123/future");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Sleepy");
        values.put("uri", "http://ample-10.radiojar.com/rrgq7vw4gxquv?rj-ttl=5&rj-token=AAABa1lok77_iuIXolNS9qjWnD31iEM1pwmTre7whSvHzvHEjWy5yg");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Classical 1");
        values.put("uri", "http://peridot.streamguys.com:7010/bblive-sgplayer-aac");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);

        values.put("title", "Classical 2");
        values.put("uri", "http://mediaserv30.live-streams.nl:8088/live");
        database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);


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
                        " AND hour>1 AND hour<11 " +
                        " AND (next_time-%d)>%d" +
                        " ORDER BY next_time LIMIT 1", TABLE_ALARMS, now.getTimeInMillis(), TimeUnit.HOURS.toMillis(4)));
    }

    public long addAOrUpdateAlarm(AlarmEntity entity) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("hour", entity.getHour());
            values.put("minute", entity.getMinute());
            values.put("message", entity.getMessage());
            values.put("days", entity.getDays());
            values.put("ticks_time", entity.getTicksTime());
            values.put("canceled_next_alarms", entity.getCanceledNextAlarms());
            values.put("active", entity.isActive() ? 1 : 0);
            values.put("exact_date", entity.getExactDate());
            values.put("next_time", entity.getNextTime());
            values.put("next_request_code", entity.getNextRequestCode());
            values.put("before_alarm_notification", entity.isBeforeAlarmNotification() ? 1 : 0);

            if (entity.getId() == 0) {
                HyperLog.i(TAG, "Alarm added :: " + entity);
                return database.insert(SQLiteDBHelper.TABLE_ALARMS, null, values);
            } else {
                database.update(TABLE_ALARMS, values, "_id=?",
                        new String[]{entity.getId() + ""});
                HyperLog.i(TAG, "Alarm updated :: " + entity);
                return entity.getId();
            }
        }
    }

    public void toggleAlarmActive(String id, boolean active) {
        SQLiteDatabase writableDatabase = this.getWritableDatabase();

        ContentValues updateValues = new ContentValues(1);
        updateValues.put("active", active ? 1 : 0);
        if (!active) {
            updateValues.put("next_time", -1);
            updateValues.put("next_request_code", -1);
        } else {
            updateValues.put("canceled_next_alarms", 0);
        }
        writableDatabase.update(TABLE_ALARMS, updateValues, "_id=?", new String[]{id});
        HyperLog.i(TAG, "Alarm [id=" + id + "] toggled to: " + active);
    }

    public void deleteAlarm(String id) {
        this.getWritableDatabase().delete(TABLE_ALARMS, "_id=?", new String[]{id});
        HyperLog.i(TAG, "Alarm [id=" + id + "] deleted");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(createAlarmTable());
        sqLiteDatabase.execSQL(createOnlineMediaResourcesTable());
        sqLiteDatabase.execSQL(createOfflineMediaResourcesTable());
        setDefaultOnlineMedia(sqLiteDatabase);
        HyperLog.i(TAG, "Database created");
    }

    private String createAlarmTable() {

        return format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "hour TINYINT,"
                + "minute TINYINT,"
                + "days TINYINT,"
                + "exact_date LONG,"
                + "message TEXT,"
                + "active BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "before_alarm_notification BOOLEAN NOT NULL CHECK (before_alarm_notification IN (0,1)) DEFAULT 1,"
                + "ticks_time TINYINT," // for how long before main alarm to play ticks (null - not play)
                + "melody TEXT,"
                + "vibration_type TEXT,"
                + "volume INTEGER,"
                + "canceled_next_alarms INTEGER DEFAULT 0,"
                + "snooze_interval TINYINT DEFAULT 5,"
                + "snooze_times TINYINT DEFAULT 3,"
                + "next_time INTEGER,"
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

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ONLINE_MEDIA);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE_MEDIA);
        onCreate(sqLiteDatabase);
    }

    public Cursor getAllOnlineMedia() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_ONLINE_MEDIA,
                null);
    }

    public void addMediaUrl(String url) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("uri", url);
            HyperLog.i(TAG, "Add media URL :: " + url);
            database.insert(SQLiteDBHelper.TABLE_ONLINE_MEDIA, null, values);
        }
    }

    public void deleteOnlineMedia(String id) {
        this.getWritableDatabase().delete(TABLE_ONLINE_MEDIA, "_id=?", new String[]{id});
        HyperLog.i(TAG, "Online Media [id=" + id + "] deleted");
    }

    public Cursor getAllLocalMedia() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_OFFLINE_MEDIA,
                null);
    }

    public void addLocalMediaUrl(String url, String title) {
        try (SQLiteDatabase database = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("uri", url);
            values.put("title", title);
            HyperLog.i(TAG, "Add media URL :: " + url);
            database.insert(SQLiteDBHelper.TABLE_OFFLINE_MEDIA, null, values);
        }
    }

    public void deleteLocalMedia(String id) {
        this.getWritableDatabase().delete(TABLE_OFFLINE_MEDIA, "_id=?", new String[]{id});
        HyperLog.i(TAG, "Online Media [id=" + id + "] deleted");
    }
}