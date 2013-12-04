package com.vingcard.vingcardkeyapp.storage;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.model.Hotel;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.DoorEventDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;

public class StorageHelper {	
	
	/**
	 * Store a keycard
	 * @param context App-context
	 * @param kc keyCard to store
	 */
	public static synchronized void storeKeyCard(Context context, KeyCard kc){
		final ContentResolver resolver = context.getContentResolver();
		
		Uri kcUri = KeyCardDB.buildKeyCardUri(kc.id);
		Cursor c = resolver.query(kcUri, new String[]{KeyCardDB.KEYCARD_ID}, null, null, null);
		//KeyCard exists. Update..
		if(c.moveToFirst()){
			resolver.update(kcUri, kc.getContentValuesForModel(), null, null);
		}
		//Insert new KeyCard.
		else{
			resolver.insert(KeyCardDB.CONTENT_URI, kc.getContentValuesForModel());
		}
		c.close();
	}

    public static synchronized void hideKeyCard(Context context, KeyCard kc){
        final ContentResolver resolver = context.getContentResolver();
        Uri kcUri = KeyCardDB.buildKeyCardUri(kc.id);
        ContentValues values = new ContentValues();
        values.put(KeyCardDB.KEYCARD_HIDDEN, true);
        resolver.update(kcUri, values, null, null);
    }

	public static synchronized List<KeyCard> storeKeyCards(Context context, KeyCard[] keyCards){
		final ContentResolver resolver = context.getContentResolver();

		List<KeyCard> newCards = new ArrayList<KeyCard>();
		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
		
		for(KeyCard kc : keyCards){
			
			Uri kcUri = KeyCardDB.buildKeyCardUri(kc.id);
			Cursor c = resolver.query(kcUri, new String[]{KeyCardDB.KEYCARD_ID}, null, null, null);
			//KeyCard exists. Update..
			if(c.moveToFirst()){
				ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(kcUri);
				builder.withValues(kc.getContentValuesForModel());
				batch.add(builder.build());	
				
				//DEBUG TO ALWAYS SHOW NOTIFICATION!
				//newCards.add(kc);
				
			}
			//Insert new KeyCard.
			else{
				ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(KeyCardDB.CONTENT_URI);
				builder.withValues(kc.getContentValuesForModel());
				batch.add(builder.build());
				if(kc.revoked == null || kc.revoked == false){
					newCards.add(kc);					
				}
			}
			c.close();
		}

		try {
			resolver.applyBatch(VingCardContract.CONTENT_AUTHORITY, batch);
		} catch (Exception e) {
			Log.e("StorageHelper", "Failed to store in database: " + e.getMessage());
			return null;
		}
		return newCards;
	}

	public static synchronized void storeEvent(Context context, DoorEvent event) {
		final ContentResolver resolver = context.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(DoorEventDB.EVENT_HOTEL_ID, event.hotelId);
		values.put(DoorEventDB.EVENT_ROOM_ID, event.roomId);
		values.put(DoorEventDB.EVENT_CARD_ID, event.cardId);
		values.put(DoorEventDB.EVENT_DATA, event.statusData);
		values.put(DoorEventDB.EVENT_TIMESTAMP, new DateTime().getMillis());
		values.put(DoorEventDB.EVENT_TYPE, DoorEvent.TYPE_LOCK);
		resolver.insert(DoorEventDB.CONTENT_URI, values);
	}

	public static synchronized boolean deleteEvent(Context context, DoorEvent doorEvent) {
		final ContentResolver resolver = context.getContentResolver();
		int deleted = resolver.delete(DoorEventDB.buildDoorEventUri(doorEvent.eventIndex), null, null);
		return deleted > 0;
	}

	public static synchronized void storeCheckInEvent(Context context, DoorEvent event) {
		final ContentResolver resolver = context.getContentResolver();

		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

		//Set card as checked in
		Uri kcUri = KeyCardDB.buildKeyCardUri(event.cardId);
		ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(kcUri);
		KeyCard kc = new KeyCard();
		kc.checkedIn = true;
		builder.withValues(kc.getContentValuesForModel());
		batch.add(builder.build());	
		
		//Create high priority event
		builder = ContentProviderOperation.newInsert(DoorEventDB.CONTENT_URI);
		ContentValues values = new ContentValues();
		values.put(DoorEventDB.EVENT_HOTEL_ID, event.hotelId);
		values.put(DoorEventDB.EVENT_ROOM_ID, event.roomId);
		values.put(DoorEventDB.EVENT_CARD_ID, event.cardId);
		values.put(DoorEventDB.EVENT_DATA, "CHECKIN");
		values.put(DoorEventDB.EVENT_TIMESTAMP, new DateTime().getMillis());
		values.put(DoorEventDB.EVENT_TYPE, DoorEvent.TYPE_CHECKIN);
		builder.withValues(values);
		batch.add(builder.build());	

		try {
			resolver.applyBatch(VingCardContract.CONTENT_AUTHORITY, batch);
		} catch (Exception e) {
			Log.e("StorageHelper", "Failed to store critical event in database: " + e.getMessage());
		}
	}

	public static synchronized void storeHotel(Context context, Hotel hotel) {
		final ContentResolver resolver = context.getContentResolver();
		resolver.insert(HotelDB.CONTENT_URI, hotel.getContentValuesForModel());
	}
}
