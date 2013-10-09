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

public class SmsHelper {
	private static final String TAG = "SmsHelper";

	public static final String SERVICE_NUMBER = "+447860034911";
	public static final String SENDER_DEFAULT = "VingCard";
	public static final String SENDER_US = "+13025179780";

	/**
	 * Send event as SMS to SERVICE_NUMBER
	 * @param context
	 * @param event
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
		String message = event.eventType + " " + event.cardId + " " + event.deltaTime;
		sms.sendTextMessage(SERVICE_NUMBER, null, message, sentPI, null);        
	}

	public static String checkInboxForCode(Context context){
		String smsCode = null;
		final Uri smsUriInbox = Uri.parse("content://sms/inbox"); 
		final Uri smsUri = Uri.parse("content://sms");  
		Cursor cur = null;
		try {  
			//String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };  
			String[] projection = new String[] {"body", "thread_id"};  
			String where = "address=? OR address=?";
			String[] whereParams = new String[]{SENDER_DEFAULT, SENDER_US};
			cur = context.getContentResolver().query(smsUriInbox, projection, where, whereParams, null);
			if (cur.moveToFirst()) {   
				int indexBody = cur.getColumnIndex("body");  
				String body = cur.getString(indexBody);
				int indexThread = cur.getColumnIndex("thread_id");  
				long threadId = cur.getLong(indexThread);
				
				//Assume last word is the code
				if(body != null){
					String[] bodyWords = body.split(" ");
					smsCode = bodyWords[bodyWords.length - 1];
				}
				where = "thread_id=?";
				whereParams = new String[]{String.valueOf(threadId)};
				int delCount = context.getContentResolver().delete(smsUri, where, whereParams);
				Log.e(TAG, "Deleted: " + delCount);  
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
