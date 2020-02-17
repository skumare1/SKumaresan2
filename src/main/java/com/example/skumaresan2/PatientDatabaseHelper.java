package com.example.skumaresan2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class PatientDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SKumaresan2.db";
    private static final int DATABASE_VERSION = 1;
    private static final String DOWNLOADED_DATABASE_NAME = "SKumaresan2Downloaded.db";
    private String tableName=null;
    private static final String COLUMN_1 = "timestamp";
    private static final String COLUMN_2 = "x";
    private static final String COLUMN_3 = "y";
    private static final String COLUMN_4 = "z";

    private String CREATE_TABLE ;

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public PatientDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public PatientDatabaseHelper(Context context, String tableName, int databaseVersion) {
        super(context, DATABASE_NAME, null, databaseVersion);
        //super(context, Environment.getExternalStorageDirectory().getPath()
//        super(context, File.separator + "sdcard"
//                + File.separator + "Android"+ File.separator + "data" + File.separator + "CSE535_ASSIGNMENT2"
//                + File.separator + DATABASE_NAME, null, DATABASE_VERSION);

        this.tableName = tableName;


        CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "
                + tableName + "(" + COLUMN_1 + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_2 + " REAL, "
                + COLUMN_3 + " REAL, "
                + COLUMN_4 + " REAL)";
        Log.d("Database","Constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        Log.d("Database","Table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }

    public void closeDatabase() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


    public long insertRow(float[] data) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_2, data[0]);
        values.put(COLUMN_3, data[1]);
        values.put(COLUMN_4, data[2]);

        long id = db.insert(tableName, null, values);
        Log.d("Database","Inserted row "+data[0]+", "+ data[1]+ ", "+ data[2]);
        return id;
    }

    public String getDatabaseName(){
        return DATABASE_NAME;
    }

    public String getDownloadedDatabaseName(){
        return DOWNLOADED_DATABASE_NAME;
    }

    public int getDatabaseVersion(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.getVersion();
    }

    public SQLiteDatabase getDatabase(){
        return this.getWritableDatabase();
    }
}
