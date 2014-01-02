package com.vingcard.vingcardkeyapp.storage;

import com.vingcard.vingcardkeyapp.storage.VingCardContract.DoorEventColumns;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelColumns;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardColumns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;



public class VingCardDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "vingcard.db";

	// NOTE: carefully update onUpgrade() when bumping database versions to make
	// sure user data is saved.
	private static final int VER_LAUNCH = 1;

	private static final int DATABASE_VERSION = VER_LAUNCH;

	interface Tables {
		String KEYCARD = "keycard";
		String DOOREVENT = "doorevent";
		String HOTEL = "hotel";

        String KEYCARDS_JOIN_HOTEL = "keycard "
                + "LEFT OUTER JOIN hotel ON keycard.hotel_id=hotel.hotelId";
	}

	public VingCardDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		db.execSQL("CREATE TABLE " + Tables.KEYCARD + " (" 
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ KeyCardColumns.KEYCARD_ID + " TEXT NOT NULL,"
				+ KeyCardColumns.KEYCARD_HOTEL_ID + " TEXT," 
				+ KeyCardColumns.KEYCARD_ROOM_ID + " TEXT," 
				+ KeyCardColumns.KEYCARD_KEY + " BLOB," 
				+ KeyCardColumns.KEYCARD_LABEL + " TEXT," 
				+ KeyCardColumns.KEYCARD_VALID_FROM + " NUMERIC," 
				+ KeyCardColumns.KEYCARD_VALID_TO + " NUMERIC," 
				+ KeyCardColumns.KEYCARD_REVOKED + " INTEGER DEFAULT 0," 
				+ KeyCardColumns.KEYCARD_PERSONAL_URL + " TEXT,"
				+ KeyCardColumns.KEYCARD_CHECKED_IN + " INTEGER DEFAULT 0,"
				+ KeyCardColumns.KEYCARD_HIDDEN + " INTEGER DEFAULT 0," 
				+ "UNIQUE (" + KeyCardColumns.KEYCARD_ID + "))");

		db.execSQL("CREATE INDEX keycard_id_idx ON " + Tables.KEYCARD + "("
				+ KeyCardColumns.KEYCARD_ID + ")");
		db.execSQL("CREATE INDEX keycard_hotel_idx ON " + Tables.KEYCARD + "("
				+ KeyCardColumns.KEYCARD_HOTEL_ID + ")");
		db.execSQL("CREATE INDEX keycard_room_idx ON " + Tables.KEYCARD + "("
				+ KeyCardColumns.KEYCARD_ROOM_ID + ")");
		
		db.execSQL("CREATE TABLE " + Tables.DOOREVENT + " (" 
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ DoorEventColumns.EVENT_HOTEL_ID + " TEXT,"
				+ DoorEventColumns.EVENT_ROOM_ID + " TEXT," 
				+ DoorEventColumns.EVENT_CARD_ID + " TEXT," 
				+ DoorEventColumns.EVENT_DATA + " TEXT," 
				+ DoorEventColumns.EVENT_TIMESTAMP + " NUMERIC," 
				+ DoorEventColumns.EVENT_TYPE + " TEXT NOT NULL)");
		
		db.execSQL("CREATE TABLE " + Tables.HOTEL + " (" 
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ HotelColumns.HOTEL_ID + " TEXT NOT NULL,"
				+ HotelColumns.HOTEL_NAME + " TEXT,"
				+ HotelColumns.HOTEL_ADDRESS + " TEXT,"
				+ HotelColumns.HOTEL_PHONE + " TEXT,"
				+ HotelColumns.HOTEL_EMAIL + " TEXT,"
				+ HotelColumns.HOTEL_WEBSITE + " TEXT,"
				+ HotelColumns.HOTEL_LOGO_URL + " TEXT," 
				+ "UNIQUE (" + HotelColumns.HOTEL_ID + ") ON CONFLICT REPLACE)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
		int version = oldVersion;

		switch (version) {

		}

		if (version != DATABASE_VERSION) {
			onCreate(db);
		}
	}

	public static void deleteDatabase(Context context) {
		context.deleteDatabase(DATABASE_NAME);
	}
}
