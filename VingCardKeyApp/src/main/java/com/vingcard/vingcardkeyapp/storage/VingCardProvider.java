package com.vingcard.vingcardkeyapp.storage;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.DoorEventDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;
import com.vingcard.vingcardkeyapp.storage.VingCardDatabase.Tables;
import com.vingcard.vingcardkeyapp.util.SelectionBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class VingCardProvider extends ContentProvider {

    private VingCardDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int KEYCARDS = 100;
    private static final int KEYCARD_ID = 101;
    private static final int KEYCARDS_WITH_HOTEL = 102;
    private static final int DOOREVENTS = 200;
    private static final int DOOREVENT_ID = 201;
    private static final int HOTELS = 300;
    private static final int HOTEL_ID = 301;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = VingCardContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "keycard", KEYCARDS);
        matcher.addURI(authority, "keycard/with_hotel", KEYCARDS_WITH_HOTEL);
        matcher.addURI(authority, "keycard/*", KEYCARD_ID);
        matcher.addURI(authority, "doorevent", DOOREVENTS);
        matcher.addURI(authority, "doorevent/*", DOOREVENT_ID);
        matcher.addURI(authority, "hotel", HOTELS);
        matcher.addURI(authority, "hotel/*", HOTEL_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new VingCardDatabase(getContext());
        return true;
    }

    private void deleteDatabase() {
        mOpenHelper.close();
        Context context = getContext();
        VingCardDatabase.deleteDatabase(context);
        mOpenHelper = new VingCardDatabase(getContext());
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEYCARDS:
                return KeyCardDB.CONTENT_TYPE;
            case KEYCARD_ID:
                return KeyCardDB.CONTENT_ITEM_TYPE;
            case KEYCARDS_WITH_HOTEL:
                return KeyCardDB.CONTENT_TYPE;
            case DOOREVENTS:
                return DoorEventDB.CONTENT_TYPE;
            case DOOREVENT_ID:
                return DoorEventDB.CONTENT_ITEM_TYPE;
            case HOTELS:
                return HotelDB.CONTENT_TYPE;
            case HOTEL_ID:
                return HotelDB.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
			case KEYCARDS_WITH_HOTEL:{
				final SelectionBuilder builder = buildExpandedSelection(uri, match);
				return builder.where(selection, selectionArgs).query(db,
						projection, sortOrder);
			}
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildSimpleSelection(uri);
                return builder.where(selection, selectionArgs).query(db,
                        projection, sortOrder);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case KEYCARDS: {
                db.insertOrThrow(Tables.KEYCARD, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return KeyCardDB.buildKeyCardUri(values
                        .getAsString(VingCardContract.KeyCardDB.KEYCARD_ID));
            }
            case DOOREVENTS: {
                long index = db.insertOrThrow(Tables.DOOREVENT, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return DoorEventDB.buildDoorEventUri(Long.toString(index));
            }
            case HOTELS: {
                db.insertOrThrow(Tables.HOTEL, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return HotelDB.buildHotelUri(values
                        .getAsString(VingCardContract.HotelDB.HOTEL_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);

        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == VingCardContract.BASE_CONTENT_URI) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            getContext().getContentResolver().notifyChange(uri, null, false);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(
            ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEYCARDS: {
                return builder.table(Tables.KEYCARD);
            }
            case KEYCARD_ID: {
                final String keyCardId = KeyCardDB.getKeyCardId(uri);
                return builder.table(Tables.KEYCARD).where(
                        VingCardContract.KeyCardDB.KEYCARD_ID + "=?", keyCardId);
            }
            case DOOREVENTS: {
                return builder.table(Tables.DOOREVENT);
            }
            case DOOREVENT_ID: {
                final String eventIndex = DoorEventDB.getDoorEventIndex(uri);
                return builder.table(Tables.DOOREVENT).where(
                        VingCardContract.DoorEventDB._ID + "=?", eventIndex);
            }
            case HOTELS: {
                return builder.table(Tables.HOTEL);
            }
            case HOTEL_ID: {
                final String hotelId = HotelDB.getHotelId(uri);
                return builder.table(Tables.HOTEL).where(
                        VingCardContract.HotelDB.HOTEL_ID + "=?", hotelId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case KEYCARDS_WITH_HOTEL: {
                return builder.table(Tables.KEYCARDS_JOIN_HOTEL)
                        .mapToTable(KeyCardDB._ID, Tables.KEYCARD)
                        .mapToTable(KeyCardDB.KEYCARD_HOTEL_ID, Tables.KEYCARD);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

}
