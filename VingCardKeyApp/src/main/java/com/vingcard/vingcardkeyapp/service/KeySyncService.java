package com.vingcard.vingcardkeyapp.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;

import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class KeySyncService extends IntentService {
	
	public final class KeySyncConstants {

	    public static final String BROADCAST_ACTION =
	        "com.vingcard.vingcardkeyapp.BROADCAST";
	    
	    public static final String DATA_USER_ID =
		        "com.vingcard.vingcardkeyapp.DATA_USER_ID";
	    
	    public static final String DATA_NEW_KEY =
		        "com.vingcard.vingcardkeyapp.DATA_NEW_KEY";

	}
	
	private long mUserId;
	private RestHelper mRestHelper;
	
    public KeySyncService() {
		super("KeySyncService");
	}

	@Override
    protected void onHandleIntent(Intent workIntent) {
		mRestHelper = new RestHelper(this);
		mUserId = PreferencesUtil.getUserId(this);

		for(int i = 0; i < 10; i++){
			SystemClock.sleep(15000);
			
			//Do call to server and check for new keys.
			mRestHelper.getKeyCards(mUserId, null);

		}
    }
}
