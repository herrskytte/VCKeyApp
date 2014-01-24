package com.vingcard.vingcardkeyapp.sms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class SmsHelper {
	private static final String TAG = "SmsHelper";

//	public static final String SERVICE_NUMBER = "+47123456789";
	public static final String SERVICE_NUMBER = "+447860034911";
	public static final String SENDER_DEFAULT = "VingCard";
	public static final String SENDER_US = "+13025179780";

	/**
	 * Send event as SMS to SERVICE_NUMBER
	 */
	public static void sendEventSMS(Context context, final DoorEvent event)
	{        
		String SENT = "SMS_SENT";

		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
				new Intent(SENT), 0);

		//---when the SMS has been sent---
		context.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context c, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					StorageHelper.deleteEvent(c, event);
					Toast.makeText(c, "SMS sent", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(c, "Generic failure", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(c, "No service", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(c, "Null PDU", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(c, "Radio off", 
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		SmsManager sms = SmsManager.getDefault();
		String message = "TEST " + event.eventType + " " + event.cardId + " " + event.deltaTime;
		sms.sendTextMessage(SERVICE_NUMBER, null, message, sentPI, null);
        Log.e(TAG, "Sendt sms:\n" + message);
    }

	public static String checkInboxForCode(Context context){
		String smsCode = null;
        String wrongSmsCode = PreferencesUtil.getWrongSmsCode(context);
		final Uri smsUriInbox = Uri.parse("content://sms/inbox");
		Cursor cur = null;
		try {  
			//String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };  
			String[] projection = new String[] {"body"};
			String where = "address=? OR address=?";
            String orderBy = "date DESC";
			String[] whereParams = new String[]{SENDER_DEFAULT, SENDER_US};
			cur = context.getContentResolver().query(smsUriInbox, projection, where, whereParams, orderBy);
			while (cur.moveToNext() && smsCode == null) {
				String body = cur.getString(0);
				
				//Assume last word is the code
				if(body != null){
					String[] bodyWords = body.split(" ");
                    String lastWord = bodyWords[bodyWords.length - 1];
                    if(lastWord.length() == 32 &&
                       !lastWord.equals(wrongSmsCode)){
                        smsCode = lastWord;
                    }
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, "Exception " + ex.getMessage());  
		} finally{
			if (cur != null && !cur.isClosed()) {  
				cur.close();  
			} 
		}
		Log.e(TAG, "Read from inbox: " + smsCode);  
		return smsCode;
	}
}
