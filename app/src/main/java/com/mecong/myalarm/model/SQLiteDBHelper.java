package com.mecong.myalarm.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hypertrack.hyperlog.HyperLog;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.mecong.myalarm.AlarmUtils.TAG;
import static java.lang.String.format;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 5;
    private static final String TABLE_ALARMS = "alarms";

    private static final String SELECT_ALL_ALARMS = "SELECT * FROM " + TABLE_ALARMS;

    private static final String SELECT_NEXT_ALARM = format(
            "SELECT * FROM %s WHERE active=1 ORDER BY next_time LIMIT 1", TABLE_ALARMS);

    private static final String DATABASE_NAME = "my_alarm_database";

    public SQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Cursor getAllAlarms() {
        return this.getReadableDatabase().rawQuery(SELECT_ALL_ALARMS, null);
    }

    private AlarmEntity getAlarmEntity(String sql) {
        try (Cursor cursor = this.getReadableDatabase().rawQuery(sql, null)) {
            return cursor.moveToFirst() ? new AlarmEntity(cursor) : null;
        }
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
                HyperLog.i(TAG, "Alarm added :: " + values.toString());
                return database.insert(SQLiteDBHelper.TABLE_ALARMS, null, values);
            } else {
                database.update(TABLE_ALARMS, values, "_id=?",
                        new String[]{entity.getId() + ""});
                HyperLog.i(TAG, "Alarm updated :: " + values.toString());
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
        String createTableSQL = format("CREATE TABLE %s ("
                + "_id INTEGER PRIMARY KEY,"
                + "hour TINYINT,"
                + "minute TINYINT,"
                + "days TINYINT,"
                + "exact_date LONG,"
                + "message TEXT,"
                + "active BOOLEAN NOT NULL CHECK (active IN (0,1)) DEFAULT 1,"
                + "before_alarm_notification BOOLEAN NOT NULL CHECK (before_alarm_notification IN (0,1)) DEFAULT 1,"
                + "light_time TINYINT," // for how long before main alarm start light (null - not light)
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
        HyperLog.i(TAG, "Database created");
        sqLiteDatabase.execSQL(createTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
        onCreate(sqLiteDatabase);
    }
}