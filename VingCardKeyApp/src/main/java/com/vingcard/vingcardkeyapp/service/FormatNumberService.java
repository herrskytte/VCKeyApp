package com.vingcard.vingcardkeyapp.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.vingcard.vingcardkeyapp.util.AppConstants;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class FormatNumberService extends IntentService{

    public FormatNumberService() {
        super("FormatNumberService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String userPhoneNumber = PreferencesUtil.getMsisdn(this);
        String userCountryCode = PreferencesUtil.getCountryCode(this);

        String formattedNumber;
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(userPhoneNumber, userCountryCode);
            formattedNumber = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } catch (NumberParseException e) {
            formattedNumber = userPhoneNumber;
        }

        // Broadcast-intent for formatted number.
        Intent localIntent = new Intent(AppConstants.Broadcasts.BROADCAST_NUMBER);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);
        localIntent.putExtra(AppConstants.Broadcasts.DATA_NUMBER, formattedNumber);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        Log.e("FormatNumberService", "Numberformat broadcast: " + formattedNumber);
    }
}
