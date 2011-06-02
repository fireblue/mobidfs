package com.ServerActivity;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper 
{
	static final String dbName = "mobiDB";
	//static final String tbName = "mobiRes";
	static final String colID = "ResourceID";
	static final String colFile = "FileName";
	static final String colPath = "FilePath";
	static final String colPublic = "Public";
	static final String colAccess = "AccessList";
	static final String colStatus = "Status"; // if deleted, marked as D. update resource will clean.
	static final String colModify = "LastModify";
	
	public DatabaseHandler(Context context)
	{
		super(context, dbName, null, 1);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String query = "CREATE TABLE "+dbName+" ("+colID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+colFile+" TEXT, "+colPath+" TEXT, "+colPublic+" INTEGER, "+colAccess+" TEXT, "+colStatus+" TEXT, "+colModify+" INTEGER)";
		db.execSQL(query);
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldV, int newV)
	{
		String query = "DROP TABLE IF EXISTS "+dbName;
		db.execSQL(query);
		onCreate(db);
	}
	
	// This function used to insert one item into the database. Database is store and handle by helper itself.
	public void insertRow(String [] row)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(colFile, row[0]);
		cv.put(colPath, row[1]);
		cv.put(colPublic, row[2]);
		cv.put(colAccess, row[3]);
		cv.put(colStatus, row[4]);
		cv.put(colModify, row[5]);
		
		db.insert(dbName, colFile, cv);
		db.close();
	}

	// update a row in the table. all information are stored as string in row[], will return the number of row that effect.
	public int updateRow(String [] row)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(colFile, row[1]);
		cv.put(colPath, row[2]);
		cv.put(colPublic, row[3]);
		cv.put(colAccess, row[4]);
		cv.put(colStatus, row[5]);
		cv.put(colModify, row[6]);
		
		return db.update(dbName, cv, colID+"=?", new String [] {row[0]});
	}
	
	// delete a row. Infomation provided in row[] could only have row[0] which is colID.
	public void deleteRow(String [] row)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(dbName, colID+"=?", new String [] {row[0]});
		db.close();
	}
	
	// execute an raw query, like select * from ..., used this to get the cursor and do other operation.
	public Cursor execQuery(String queryStr)
	{
		Log.d("Database", queryStr);
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery(queryStr, null);
		return c;
	}
	
	// This used to fetch one row in the cursor, to fetch all, need multiple call of this function.
	public String [] fetchOneRow(Cursor c)
	{
		String [] res = new String[7];
		
		if (c.isAfterLast()) return null;
		
		res[0] = c.getString(c.getColumnIndex(colID));
		res[1] = c.getString(c.getColumnIndex(colFile));
		res[2] = c.getString(c.getColumnIndex(colPath));
		res[3] = c.getString(c.getColumnIndex(colPublic));
		res[4] = c.getString(c.getColumnIndex(colAccess));
		res[5] = c.getString(c.getColumnIndex(colStatus));
		res[6] = c.getString(c.getColumnIndex(colModify));
		
		c.moveToNext();
		
		return res;
	}
	
	// return the number of rows for specific query executed.
	public int countRow(Cursor c)
	{
		return c.getCount();
	}
	
	
}
