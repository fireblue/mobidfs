package com.Uno.unoAndroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PinDatabaseHelper extends SQLiteOpenHelper {

	static final String dbName = "PIN_DB";
	static final String colID = "PIN_ID";
	static final String colResourceID = "PIN_RESOURCE_ID";  // This is a global ID stored in Governor's database.
	static final String colCachePath = "PIN_CACHE_PATH";
	static final String colCacheName = "PIN_CACHE_NAME";
	
	public PinDatabaseHelper(Context context) {
		super(context, dbName, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String query = "CREATE TABLE "+dbName+" ("+colID+ 
						" INTEGER PRIMARY KEY AUTOINCREMENT, "+colResourceID+
						" TEXT, "+colCachePath+" TEXT, "+colCacheName+" INTEGER)";
		db.execSQL(query);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
		String query = "DROP TABLE IF EXISTS "+dbName;
		db.execSQL(query);
		onCreate(db);
	}
	
	// This function used to insert one item into the database. 
	// Database is store and handle by helper itself.
	public void insertRow(String [] row) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(colResourceID, row[0]);
		cv.put(colCachePath, row[1]);
		cv.put(colCacheName, row[2]);
		db.insert(dbName, colResourceID, cv);
		db.close();
	}

	// update a row in the table. all information are stored as string in row[], 
	// will return the number of row that effect.
	public int updateRow(String [] row) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(colResourceID, row[1]);
		cv.put(colCachePath, row[2]);
		cv.put(colCacheName, row[3]);
		return db.update(dbName, cv, colID+"=?", new String [] {row[0]});
	}
	
	// delete a row. Information provided in row[] could only have row[0] which is colID.
	public void deleteRow(String [] row) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(dbName, colID+"=?", new String [] {row[0]});
		db.close();
	}
	
	// execute an raw query, like select * from ..., 
	// used this to get the cursor and do other operation.
	public Cursor execQuery(String queryStr) {
		Log.d("Database", queryStr);
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery(queryStr, null);
		return c;
	}
	
	// This used to fetch one row in the cursor, to fetch all, 
	// need multiple call of this function.
	public String [] fetchOneRow(Cursor c) {
		String [] res = new String[4];
		if (c.isAfterLast()) return null;
		res[0] = c.getString(c.getColumnIndex(colID));
		res[1] = c.getString(c.getColumnIndex(colResourceID));
		res[2] = c.getString(c.getColumnIndex(colCachePath));
		res[3] = c.getString(c.getColumnIndex(colCacheName));
		c.moveToNext();
		return res;
	}
	
	// return the number of rows for specific query executed.
	public int countRow(Cursor c) {
		return c.getCount();
	}
}
