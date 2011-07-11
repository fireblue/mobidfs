package com.Uno.unoAndroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocalResourceDatabaseHelper extends SQLiteOpenHelper {

	static final String dbName = "RESOURCE_DB";
	static final String colID = "RESOURCE_ID";
	static final String colOwner = "RESOURCE_OWNER";
	static final String colDevice = "RESOURCE_DEVICE";
	static final String colResourceName = "RESOURCE_NAME";
	static final String colResourcePath = "RESOURCE_PATH";
	static final String colAccessList = "ACCESS_LIST";
	static final String colMetadata = "METADATA";
	
	public LocalResourceDatabaseHelper(Context context) {
		super(context, dbName, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String query = "CREATE TABLE "+dbName+" ("+colID+
						" INTEGER PRIMARY KEY AUTOINCREMENT, "+colOwner+
						" TEXT, "+colDevice+" TEXT,"+colResourceName+
						" TEXT, "+colResourcePath+" TEXT, "+colAccessList+
						" TEXT, "+colMetadata+" TEXT)";
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
		cv.put(colOwner, row[0]);
		cv.put(colDevice, row[1]);
		cv.put(colResourceName, row[2]);
		cv.put(colResourcePath, row[3]);
		cv.put(colAccessList, row[4]);
		cv.put(colMetadata, row[5]);
		db.insert(dbName, colResourceName, cv);
		db.close();
	}

	// update a row in the table. all information are stored as string in row[], 
	// will return the number of row that effect.
	public int updateRow(String [] row) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(colOwner, row[1]);
		cv.put(colDevice, row[2]);
		cv.put(colResourceName, row[3]);
		cv.put(colResourcePath, row[4]);
		cv.put(colAccessList, row[5]);
		cv.put(colMetadata, row[6]);
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
		String [] res = new String[7];
		if (c.isAfterLast()) return null;
		res[0] = c.getString(c.getColumnIndex(colID));
		res[1] = c.getString(c.getColumnIndex(colOwner));
		res[2] = c.getString(c.getColumnIndex(colDevice));
		res[3] = c.getString(c.getColumnIndex(colResourceName));
		res[4] = c.getString(c.getColumnIndex(colResourcePath));
		res[5] = c.getString(c.getColumnIndex(colAccessList));
		res[6] = c.getString(c.getColumnIndex(colMetadata));
		c.moveToNext();
		return res;
	}
	
	// return the number of rows for specific query executed.
	public int countRow(Cursor c) {
		return c.getCount();
	}

}
