package com.example.skumaresan2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class PatientDownloadedDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DOWNLOADED_DATABASE_NAME = "SKumaresan2Downloaded.db";
    private String tableName=null;
    private SQLiteDatabase db;
    private static final String COLUMN_2 = "x";
    private static final String COLUMN_3 = "y";
    private static final String COLUMN_4 = "z";

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public PatientDownloadedDatabaseHelper(Context context) {
        super(context, DOWNLOADED_DATABASE_NAME, null, DATABASE_VERSION);
    }

    public PatientDownloadedDatabaseHelper(Context context, String tableName, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
        this.tableName = tableName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db=db;
        Log.d("Database","Downloaded database has been opened");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void closeDatabase() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


    public float[][] fetchLastTenRows() {
        float[][] result = new float[10][3];

        SQLiteDatabase db = this.getWritableDatabase();

        String query = "select * from "+tableName+ " order by timestamp DESC limit 10";
        Cursor cursor = db.rawQuery(query,null,null);

        int i=0;
        if(cursor.moveToFirst()){
            do {
                result[i][0] = cursor.getFloat(cursor.getColumnIndex(COLUMN_2));
                result[i][1] = cursor.getFloat(cursor.getColumnIndex(COLUMN_3));
                result[i][2] = cursor.getFloat(cursor.getColumnIndex(COLUMN_4));
                Log.d("Database Download", "x="+result[i][0] + " y="+result[i][1] + " z="+result[i][2]);
                i++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        Log.d("Database","Fetched last 10 rows ");

        return result;
    }

    public int getDatabaseVersion(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.getVersion();
    }


}
