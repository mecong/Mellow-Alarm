package com.mecong.myalarm.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hypertrack.hyperlog.HyperLog;

import static com.mecong.myalarm.AlarmUtils.TAG;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private static final String TABLE_ALARMS = "alarms";

    private static final String DATABASE_NAME = "my_alarm_database";
    private static final int DATABASE_VERSION = 1;

    public SQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Cursor getAllAlarms() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_ALARMS, null);
    }

    public AlarmEntity getNextActiveAlarm() {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT * FROM " + TABLE_ALARMS + " WHERE active=1 ORDER BY next_time", null);

        AlarmEntity alarmEntity = null;
        if (cursor.moveToFirst()) {
            alarmEntity = new AlarmEntity(cursor);
        }
        cursor.close();

        return alarmEntity;
    }

    public AlarmEntity getAlarmById(String id) {
        Cursor cursor = this.getReadableDatabase()
                .rawQuery("SELECT * FROM " + TABLE_ALARMS + " WHERE _id=?", new String[]{id});

        AlarmEntity alarmEntity = null;
        if (cursor.moveToFirst()) {
            alarmEntity = new AlarmEntity(cursor);
        }
        cursor.close();

        return alarmEntity;
    }

    public long addAlarm(AlarmEntity entity) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hour", entity.getHour());
        values.put("minute", entity.getMinute());
        values.put("message", entity.getMessage());
        values.put("days", entity.getDays());
        values.put("ticks_time", entity.getTicksTime());
        return database.insert(SQLiteDBHelper.TABLE_ALARMS, null, values);
    }

    public void toggleAlarmActive(String id, boolean active) {
        SQLiteDatabase writableDatabase = this.getWritableDatabase();
        String whereClause = "_id=" + id;

        ContentValues updateValues = new ContentValues(1);
        updateValues.put("active", active ? 1 : 0);
        if (!active) {
            updateValues.put("next_time", -1);
            updateValues.put("next_request_code", -1);
        }
        writableDatabase.update(TABLE_ALARMS, updateValues, whereClause, null);
        HyperLog.i(TAG, "Alarm [id=" + id + "] toggled to: " + active);
    }

    public void updateNextAlarmTimeAndCode(long id, long nextTime, int requestCode) {
        SQLiteDatabase writableDatabase = this.getWritableDatabase();
        String whereClause = "_id=" + id;

        ContentValues updateValues = new ContentValues(1);
        updateValues.put("next_time", nextTime);
        updateValues.put("next_request_code", requestCode);
        writableDatabase.update(TABLE_ALARMS, updateValues, whereClause, null);
        HyperLog.i(TAG, "Alarm [id=" + id + "] updated. next_time=" + nextTime
                + ", next_request_code=" + requestCode);
    }

    public void deleteAlarm(String id) {
        this.getWritableDatabase().delete(TABLE_ALARMS, "_id=?", new String[]{id});
        HyperLog.i(TAG, "Alarm [id=" + id + "] deleted");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTableSQL = String.format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "hour TINYINT,"
                + "minute TINYINT,"
                + "days TINYINT,"
                + "exact_date TEXT,"
                + "message TEXT,"
                + "active BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "light_time TINYINT," // for how long before main alarm start light (null - not light)
                + "ticks_time TINYINT," // for how long before main alarm to play ticks (null - not play)
                + "melody TEXT,"
                + "vibration_type TEXT,"
                + "volume INTEGER,"
                + "snooze_interval TINYINT DEFAULT 5,"
                + "snooze_times TINYINT DEFAULT 3,"
                + "next_time INTEGER,"
                + "next_request_code INTEGER"
                + ")", TABLE_ALARMS);
        HyperLog.i(TAG, "Database created");
        sqLiteDatabase.execSQL(createTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
        onCreate(sqLiteDatabase);
    }
}