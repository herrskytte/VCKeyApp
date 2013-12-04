package com.vingcard.vingcardkeyapp.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;

import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class KeySyncService extends IntentService {

    public KeySyncService() {
		super("KeySyncService");
	}

	@Override
    protected void onHandleIntent(Intent workIntent) {
        RestHelper mRestHelper = new RestHelper(this);
        long mUserId = PreferencesUtil.getUserId(this);

		for(int i = 0; i < 10; i++){
			SystemClock.sleep(15000);
			
			//Do call to server and check for new keys.
			mRestHelper.getKeyCards(mUserId);

		}
    }
}
