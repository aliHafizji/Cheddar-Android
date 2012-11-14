package com.creativeperson.cheddar.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

public class CheddarContentProvider extends ContentProvider {

	private static final String DATABASE_NAME = "cheddardatabase.db";
	private static final int DATABASE_VERSION = 1;
	private static final String AUTHORITY = "com.creativeperson.cheddar.providers.todoContentProvider";
	private static final UriMatcher mUriMatcher;

	private static final int LISTS_DATA = 1;
	private static final int LIST_DATA = 2;
	private static final int TASKS_DATA = 3;
	private static final int TASK_DATA = 4;
	
	protected static final String LISTS_TABLE_NAME = "lists";
	protected static final String TASKS_TABLE_NAME = "tasks";
	
	public static final class Lists implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + LISTS_TABLE_NAME);

		public static final String CONTENT_TYPE_MUTIPLE_ROWS = "vnd.android.cursor.dir/vnd.com.creativeperson.cheddar.provider." + LISTS_TABLE_NAME;
		public static final String CONTENT_TYPE_SINGLE_ROW = "vnd.android.cursor.item/vnd.com.creativeperson.cheddar.provider." + LISTS_TABLE_NAME;

		public static final String LIST_ID = "_id";
		public static final String LIST_TITLE = "title";
		public static final String LIST_POSITION = "position";
		public static final String ARCHIVED_AT = "archived_at";
	}
	
	public static final class Tasks implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TASKS_TABLE_NAME);
		
		public static final String CONTENT_TYPE_MULTIPLE_ROWS = "vnd.android.cursor.dir/vnd.com.creativeperson.cheddar.provider." + TASKS_TABLE_NAME;
		public static final String CONTENT_TYPE_SINGLE_ROW = "vnd.android.cursor.item/vnd.com.creativeperson.cheddar.provider." + TASKS_TABLE_NAME;
		
		public static final String TASK_ID = "_id";
		public static final String LIST_ID = "list_id";
		public static final String TASK_POSITION = "position";
		public static final String ARCHIVED_AT = "archived_at";
		public static final String COMPLETED_AT = "completed_at";
		public static final String TASK_TEXT = "text";
		public static final String DISPLAY_TASK_TEXT = "display_text";
		public static final String DISPLAY_TASK_HTML = "display_html";
	}
	
	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, LISTS_TABLE_NAME, LISTS_DATA);
		mUriMatcher.addURI(AUTHORITY, LISTS_TABLE_NAME + "/#", LIST_DATA);
		mUriMatcher.addURI(AUTHORITY, TASKS_TABLE_NAME, TASKS_DATA);
		mUriMatcher.addURI(AUTHORITY, TASKS_TABLE_NAME + "/#", TASK_DATA);
	}

	private DatabaseHelper mDatabaseHelper;

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case LISTS_DATA:
			return Lists.CONTENT_TYPE_MUTIPLE_ROWS;
		case LIST_DATA:
			return Lists.CONTENT_TYPE_SINGLE_ROW;
		case TASKS_DATA:
			return Tasks.CONTENT_TYPE_MULTIPLE_ROWS;
		case TASK_DATA:
			return Tasks.CONTENT_TYPE_SINGLE_ROW;
		}
		return null;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if((mUriMatcher.match(uri) != LISTS_DATA ) && (mUriMatcher.match(uri) != TASKS_DATA)) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		ContentValues values;
		if(initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		long rowId = 0;
		switch (mUriMatcher.match(uri)) {
		case LISTS_DATA:
			rowId = db.insert(LISTS_TABLE_NAME, null, values);
			break;
		case TASKS_DATA:
			rowId = db.insert(TASKS_TABLE_NAME, null, values);
			break;
		}
		if(rowId > 0) {
			Uri listUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(listUri, null);
			return listUri;
		}
		return null;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch(mUriMatcher.match(uri)) {
		case LIST_DATA:
			selection = selection + "_id =" + uri.getLastPathSegment();
		case LISTS_DATA:
			qb.setTables(LISTS_TABLE_NAME);
			break;
		case TASK_DATA:
			selection = selection + "_id=" + uri.getLastPathSegment();
		case TASKS_DATA:
			qb.setTables(TASKS_TABLE_NAME);
			break;
		}
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count;
		switch(mUriMatcher.match(uri)) {
		case LISTS_DATA:
			count = db.update(LISTS_TABLE_NAME, values, selection, selectionArgs);
			break;
		case TASKS_DATA:
			count = db.update(TASKS_TABLE_NAME, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		String tableName;
		
		switch(mUriMatcher.match(uri)) {
		case LIST_DATA:
			selection = selection + "_id = " + uri.getLastPathSegment();
		case LISTS_DATA:
			tableName = LISTS_TABLE_NAME;
			break;
		case TASK_DATA:
			selection = selection + "_id =" + uri.getLastPathSegment();
		case TASKS_DATA:
			tableName = TASKS_TABLE_NAME;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		int count = db.delete(tableName, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	protected static final class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL("CREATE TABLE " + LISTS_TABLE_NAME + " (" + Lists.LIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
																	   + Lists.LIST_TITLE + " TEXT," 
																	   + Lists.LIST_POSITION + " INTEGER," 
																	   + Lists.ARCHIVED_AT +" TEXT );");
			
			database.execSQL("CREATE TABLE " + TASKS_TABLE_NAME + "(" + Tasks.TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
																	  + Tasks.LIST_ID + " INTEGER,"
																	  + Tasks.TASK_TEXT + " TEXT,"
																	  + Tasks.DISPLAY_TASK_TEXT + " TEXT,"
																	  + Tasks.DISPLAY_TASK_HTML + " TEXT,"
																	  + Tasks.ARCHIVED_AT + " TEXT,"
																	  + Tasks.COMPLETED_AT + " Text,"
																	  + Tasks.TASK_POSITION + " INTEGER);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			database.execSQL("DROP TABLE IF EXISTS " + LISTS_TABLE_NAME);
			database.execSQL("DROP TABLE IF EXISTS" + TASKS_TABLE_NAME);
			onCreate(database);
		}
	}
}
