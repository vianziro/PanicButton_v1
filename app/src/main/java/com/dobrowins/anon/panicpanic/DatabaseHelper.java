package com.dobrowins.anon.panicpanic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int SCHEMA_VER = 1;
    private static final String DATABASE_NAME = "userInputInfo.db";
    private static final String TABLE_NAME = "userInputTable";
    private static final String COL_0 = "_id"; // all the Primary Keys must have this as a name
    private static final String COL_1 = "phoneNumberColumn";

    private Cursor cursor;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COL_0 + " INTEGER PRIMARY KEY, " +
                COL_1 + " TEXT);"); //creating database
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // upgrading database, all the old data is removed; also version name is changed
        database.setVersion(newVersion);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME); //deleting database if exists
        onCreate(database); //recreating database
    }

    //  inserting number from EditText
    void SaveNumber (String phoneNumber) {
        try {
            // filling in contentvalues before insert
            ContentValues cv = new ContentValues();
            cv.put(COL_1, phoneNumber); //  inserting in cv

            // getting instance of db
            SQLiteDatabase database = this.getWritableDatabase();

            // inserting cv in db
            // checking if db is not empty; if that's so - upgrading it
            cursor = database.rawQuery("SELECT " + COL_1 + " FROM " + TABLE_NAME + ";", null);
            if (cursor != null) {
                onUpgrade(database, 0, 1);
            }
            // putting data in db
            database.insert(TABLE_NAME, null, cv); // telling where and what to insert
            database.close(); // finishing the job
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    boolean CursorIsNotNull () {
        SQLiteDatabase database = this.getWritableDatabase();

        try {
            cursor = database.rawQuery("SELECT " + COL_1 + " FROM " + TABLE_NAME + ";", null);
            if (cursor != null) {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }
} 
