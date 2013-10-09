package com.vingcard.vingcardkeyapp.sms;

import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsBroadcastReceiver extends BroadcastReceiver{
	private static final String TAG = "SmsBroadcastReceiver";
	
    public void onReceive(Context context, Intent intent){
    	Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        SmsMessage message =SmsMessage.createFromPdu((byte[]) pdus[0]);    
    	if(message.getOriginatingAddress().equalsIgnoreCase(SmsHelper.SENDER_DEFAULT) ||
    			message.getOriginatingAddress().equalsIgnoreCase(SmsHelper.SENDER_US)	){
    		Log.e(TAG, "DoAbort - SMStext received: " + message.getMessageBody());
    		String[] bodyWords = message.getMessageBody().split(" ");
    		String smsCode = bodyWords[bodyWords.length - 1];
    		PreferencesUtil.setSmsCode(context, smsCode);
    		this.abortBroadcast();
    	}
    }
}
