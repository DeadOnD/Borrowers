/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.thilo.android.borrowersapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.thilo.android.borrowersapp.LentObject;
import com.thilo.android.borrowersapp.ListLentObjects;
import com.thilo.android.borrowersapp.MainActivity;

import static com.thilo.android.borrowersapp.MainActivity.LOG_TAG;

public class OpenLendDbAdapter {

    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DATE = "date";
    public static final String KEY_MODIFICATION_DATE = "modification_date";
    public static final String KEY_PERSON = "person";
	public static final String KEY_PERSON_KEY = "person_key";
    public static final String KEY_BACK = "back";
    public static final String KEY_CALENDAR_ENTRY = "calendar_entry";
    public static final String KEY_ROWID = "_id";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static Map<Context, OpenLendDbAdapter> instances;

    /**
     * Database creation sql statement
     */
    private static final String LENTOBJECTS_DATABASE_CREATE =
        "create table lentobjects (" + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_DESCRIPTION + " text not null, " + KEY_TYPE + " integer, " + KEY_DATE + " date not null, "
        + KEY_MODIFICATION_DATE + " date not null, " + KEY_PERSON + " text not null, " + KEY_PERSON_KEY + " text, "
		+ KEY_BACK + " integer not null, " + KEY_CALENDAR_ENTRY + " text);";

    private static final String DATABASE_NAME = "data";
    private static final String LENTOBJECTS_DATABASE_TABLE = "lentobjects";
    static final int DATABASE_VERSION = 4;

    private static final String CREATE_CALENDAR_ENTRY_COLUMN =
            "ALTER TABLE " + LENTOBJECTS_DATABASE_TABLE + " ADD COLUMN " + KEY_CALENDAR_ENTRY + " text";
    
    private static final String CREATE_TYPE_COLUMN =
            "ALTER TABLE " + LENTOBJECTS_DATABASE_TABLE + " ADD COLUMN " + KEY_TYPE + " integer";

    private static final String CREATE_MODIFICATION_DATE_COLUMN =
            "ALTER TABLE " + LENTOBJECTS_DATABASE_TABLE + " ADD COLUMN " + KEY_MODIFICATION_DATE + " date";

    private static final String COPY_DATES =
            "UPDATE " + LENTOBJECTS_DATABASE_TABLE + " SET " + KEY_MODIFICATION_DATE + " = " + KEY_DATE;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        final Context context;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(LENTOBJECTS_DATABASE_CREATE);

            SharedPreferences preferences =
                    context.getSharedPreferences(ListLentObjects.class.getSimpleName(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(MainActivity.FIRST_START, true);
            editor.commit();
        }

        public void createWithoutExampleData(SQLiteDatabase db) {
            db.execSQL(LENTOBJECTS_DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            if (oldVersion < 2) {
                db.execSQL(CREATE_CALENDAR_ENTRY_COLUMN);
            }

            if (oldVersion < 3) {
                db.execSQL(CREATE_TYPE_COLUMN);
            }

            if (oldVersion < 4) {
                db.execSQL(CREATE_MODIFICATION_DATE_COLUMN);
                db.execSQL(COPY_DATES);
            }

        }
    }

    public static synchronized OpenLendDbAdapter getInstance(Context ctx) {
        if (instances == null) {
            instances = new HashMap<Context, OpenLendDbAdapter>();
        }

        OpenLendDbAdapter instance;

        if (!instances.containsKey(ctx)) {
            instance = new OpenLendDbAdapter(ctx);
            instances.put(ctx, instance);
        }
        else {
            instance = instances.get(ctx);
        }

        return instance;
    }

    private OpenLendDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public OpenLendDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public void clearDatabase() {
        mDb.execSQL("DROP TABLE IF EXISTS lentobjects");
        mDbHelper.createWithoutExampleData(mDb);
    }

    public long createLentObject(LentObject lentObject) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_DESCRIPTION, lentObject.description);
        initialValues.put(KEY_TYPE, lentObject.type);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        initialValues.put(KEY_DATE, dateFormat.format(lentObject.date));
        initialValues.put(KEY_MODIFICATION_DATE, dateFormat.format(lentObject.modificationDate));
        initialValues.put(KEY_PERSON, lentObject.personName);
		initialValues.put(KEY_PERSON_KEY, lentObject.personKey);
        initialValues.put(KEY_BACK, lentObject.returned);
        if (lentObject.calendarEventURI != null) {
            initialValues.put(KEY_CALENDAR_ENTRY, lentObject.calendarEventURI.toString());
        }

        return mDb.insert(LENTOBJECTS_DATABASE_TABLE, null, initialValues);
    }

    public boolean deleteLentObject(long rowId) {
        return mDb.delete(LENTOBJECTS_DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

	public Cursor fetchAllObjects() {
		return mDb.query(LENTOBJECTS_DATABASE_TABLE, new String[] {KEY_ROWID,
				KEY_DESCRIPTION, KEY_TYPE, KEY_DATE, KEY_MODIFICATION_DATE, KEY_PERSON, KEY_PERSON_KEY, KEY_BACK, KEY_CALENDAR_ENTRY}, null, null, null, null, KEY_DATE + " ASC");
	}

    public Cursor fetchLentObjects() {
        return mDb.query(LENTOBJECTS_DATABASE_TABLE, new String[] {KEY_ROWID,
                KEY_DESCRIPTION, KEY_TYPE, KEY_DATE, KEY_MODIFICATION_DATE, KEY_PERSON, KEY_PERSON_KEY, KEY_BACK, KEY_CALENDAR_ENTRY}, KEY_BACK + "=0", null, null, null, KEY_DATE + " ASC");
    }

	public Cursor fetchReturnedObjects() {
		return mDb.query(LENTOBJECTS_DATABASE_TABLE, new String[] {KEY_ROWID,
				KEY_DESCRIPTION, KEY_TYPE, KEY_DATE, KEY_MODIFICATION_DATE, KEY_PERSON, KEY_PERSON_KEY, KEY_BACK, KEY_CALENDAR_ENTRY}, KEY_BACK + "=1", null, null, null, KEY_DATE + " ASC");
	}

    public boolean updateLentObject(long rowId, LentObject lentObject) {
        ContentValues args = new ContentValues();
        args.put(KEY_DESCRIPTION, lentObject.description);
        args.put(KEY_TYPE, lentObject.type);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        args.put(KEY_DATE, dateFormat.format(lentObject.date));
        args.put(KEY_PERSON, lentObject.personName);
		args.put(KEY_PERSON_KEY, lentObject.personKey);

		return updateLentObject(rowId, args);
    }

	public boolean markLentObjectAsReturned(long rowId) {
		ContentValues values = new ContentValues();
		values.put(KEY_BACK, true);
		return updateLentObject(rowId, values);
	}

    public boolean markReturnedObjectAsLentAgain(long rowId) {
        ContentValues values = new ContentValues();
        values.put(KEY_BACK, false);
        return updateLentObject(rowId, values);
    }

	private boolean updateLentObject(long rowId, ContentValues values) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        values.put(KEY_MODIFICATION_DATE, dateFormat.format(new Date()));
		return mDb.update(LENTOBJECTS_DATABASE_TABLE, values, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
