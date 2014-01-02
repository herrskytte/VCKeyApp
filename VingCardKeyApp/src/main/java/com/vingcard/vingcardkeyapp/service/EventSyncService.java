package com.vingcard.vingcardkeyapp.service;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.sms.SmsHelper;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.DoorEventDB;

public class EventSyncService extends IntentService {
	private static final String TAG = "EventSyncService";

    //7 attempts equals 4,2 minutes
	private static final int MAX_ATTEMPTS = 7;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
	
    public EventSyncService() {
		super("EventSyncService");
	}

	@Override
    protected void onHandleIntent(Intent workIntent) {
        RestHelper mRestHelper = new RestHelper(this);
		
		setNetworkMonitoring(false);
		
		List<DoorEvent> smsQueue = new ArrayList<>();
		
		long backoff = BACKOFF_MILLI_SECONDS;
		boolean success = true;
		for (int i = 1; i <= MAX_ATTEMPTS; i++) {
			Cursor c = getContentResolver().query(DoorEventDB.CONTENT_URI, DoorEventQuery.PROJECTION, null, null, DoorEventDB.DEFAULT_SORT);
			while(c.moveToNext()){
				DoorEvent event = createDoorEventFromCursor(c);
				boolean sent = mRestHelper.sendDoorEvent(event);
				if(!sent && i == MAX_ATTEMPTS && event.isHighPriority()){
					smsQueue.add(event);
				}
				success &= sent;
			}
			c.close();
			
			if(!success){
				Log.e(TAG, "Failed to register on attempt " + i);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                	Log.e(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                	Log.e(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return;
                }
                // increase backoff exponentially
                backoff *= 2;
			} 
		}
		
		if(!success){
			for(DoorEvent event : smsQueue){
				SmsHelper.sendEventSMS(this, event);				
			}
			
			//Wait for sms to be sent before checking that all events are handled
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					Cursor c = getContentResolver().query(DoorEventDB.CONTENT_URI, new String[]{DoorEventDB._ID}, null, null, null);
					if(c.getCount() > 0){
						//No network. Listen for connection to try later
						setNetworkMonitoring(true);
					}
					c.close();
				}
			}, 20000);
		}
    }
	
	private DoorEvent createDoorEventFromCursor(Cursor c) {
		DoorEvent event = new DoorEvent();
		event.eventIndex = c.getString(DoorEventQuery.EVENT_ID);
		event.hotelId = c.getString(DoorEventQuery.EVENT_HOTEL_ID);
		event.cardId = c.getString(DoorEventQuery.EVENT_CARD_ID);
		event.roomId = c.getString(DoorEventQuery.EVENT_ROOM_ID);
		event.statusData = c.getString(DoorEventQuery.EVENT_DATA);
		event.eventType = c.getString(DoorEventQuery.EVENT_TYPE);
		//Convert delta-time to seconds before sending
		event.deltaTime = (new DateTime().getMillis() - c.getLong(DoorEventQuery.EVENT_TIMESTAMP))/1000;
		return event;
	}
	
	private void setNetworkMonitoring(boolean enabled){
		ComponentName receiver = new ComponentName(getApplicationContext(), NetworkStateReceiver.class);
		PackageManager pm = getApplicationContext().getPackageManager();

		int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		pm.setComponentEnabledSetting(receiver,
									  state,
									  PackageManager.DONT_KILL_APP);
	}
	
	
	private interface DoorEventQuery {
		String[] PROJECTION = { DoorEventDB._ID,
								DoorEventDB.EVENT_HOTEL_ID,
								DoorEventDB.EVENT_ROOM_ID,
								DoorEventDB.EVENT_CARD_ID,
								DoorEventDB.EVENT_DATA,
								DoorEventDB.EVENT_TIMESTAMP,
								DoorEventDB.EVENT_TYPE};

		int EVENT_ID = 0;
	    int EVENT_HOTEL_ID = 1;
        int EVENT_ROOM_ID = 2;
        int EVENT_CARD_ID = 3;
        int EVENT_DATA = 4;
        int EVENT_TIMESTAMP = 5;
        int EVENT_TYPE = 6;
	}
}