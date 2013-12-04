package com.vingcard.vingcardkeyapp.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.vingcard.vingcardkeyapp.model.Hotel;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.service.CardNotificationHelper;
import com.vingcard.vingcardkeyapp.service.RestHelper;
import com.vingcard.vingcardkeyapp.service.VingCardGsonConverter;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.util.AppConstants;
import com.vingcard.vingcardkeyapp.util.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
	private static final String TAG = "GcmIntentService";

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	Log.e(TAG, "GCM MEssage received!");
    	
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        Gson gson = new VingCardGsonConverter().createGson();

        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
        	Log.e(TAG, "Send error");
        } 
        else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
        	Log.e(TAG, "Deleted messages on server");
        } 
        else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
        	String action = intent.getStringExtra("action");
        	String extraData = intent.getStringExtra("extraData");
        	String sentTime = intent.getStringExtra("sentTime");
        	if(sentTime != null){
        		DateTime sentDateTime = DateUtil.deserializeDateTime(sentTime);
        		int timePassed = (int)(DateTimeUtils.currentTimeMillis() - sentDateTime.getMillis()) / 1000;
        		Log.e(TAG, "Message received. Send time (s): " + timePassed);
        	}
        	
        	if (action == null) {
        		Log.e(TAG, "Message received without command action");
        	}
        	else{
        		Log.e(TAG, action + "- ExtraData:" + extraData);
        		KeyCard keyCard = gson.fromJson(extraData, KeyCard.class);
                if(keyCard.hotel != null){
                    StorageHelper.storeHotel(this, keyCard.hotel);
                }
                StorageHelper.storeKeyCard(this, keyCard);

                CardNotificationHelper.notifyKeyUpdate(this, keyCard, action);
        	}
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}