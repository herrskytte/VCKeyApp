package com.vingcard.vingcardkeyapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.vingcard.vingcardkeyapp.model.User;

public class PreferencesUtil {

	private static final String PREF_ID = "userid";
	private static final String PREF_MSISDN = "usermsisdn";
	private static final String PREF_COUNTRY_CODE = "usercountry";
	private static final String PREF_NUMBER = "usernumber";
	private static final String PREF_GCM_REG = "usergcmregid";
	private static final String PREF_SMS_CODE = "usersmscode";
	private static final String PREF_WRONG_SMS_CODE = "userwrongsmscode";
	private static final String PREF_APP_VERSION = "appversion";
	
	public static User getUserData(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		User userData = new User();
		userData.id = sp.getLong(PREF_ID, 0);
		userData.phoneNumber = sp.getString(PREF_MSISDN, null);
		userData.registrationCode = sp.getString(PREF_SMS_CODE, null);
		userData.registrationId = sp.getString(PREF_GCM_REG, null);
		return userData;
	}
	
	public static void setUserData(Context context, User userData){
		if(userData != null){
			final Editor editPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editPrefs.putLong(PREF_ID, userData.id);
			editPrefs.putString(PREF_MSISDN, userData.phoneNumber);
			editPrefs.putString(PREF_SMS_CODE, userData.registrationCode);
			editPrefs.putString(PREF_GCM_REG, userData.registrationId);
			editPrefs.commit();
		}		
	}
	
	public static long getUserId(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getLong(PREF_ID, 0);
	}
	
	public static String getMsisdn(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_MSISDN, null);
	}

	public static String getCountryCode(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_COUNTRY_CODE, null);
	}

	public static String getNumber(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_NUMBER, null);
	}
	
	public static String getSmsCode(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_SMS_CODE, null);
	}

	public static String getWrongSmsCode(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_WRONG_SMS_CODE, null);
	}
	
	public static String getGcmRegistrationId(Context context){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(PREF_GCM_REG, null);
	}
	
	public static int getStoredAppVersion(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getInt(PREF_APP_VERSION, Integer.MIN_VALUE);
	}
	
	public static void setUserId(Context context, long userId){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putLong(PREF_ID, userId).commit();
	}
	
	public static void setMsisdn(Context context, String msisdn){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putString(PREF_MSISDN, msisdn).commit();
	}

	public static void setCountryCode(Context context, String cc){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putString(PREF_COUNTRY_CODE, cc).commit();
	}

	public static void setNumber(Context context, String num){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putString(PREF_NUMBER, num).commit();
	}
	
	public static void setSmsCode(Context context, String smsCode){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putString(PREF_SMS_CODE, smsCode).commit();
	}

	public static void setWrongSmsCode(Context context, String smsCode){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putString(PREF_WRONG_SMS_CODE, smsCode).commit();
	}

	/**
	 * Stores the registration id and app versionCode in the
	 * application's {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	public static void setRegistrationId(Context context, String regId, int forAppVersion) {
	    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
	    sp.edit().putString(PREF_GCM_REG, regId).commit();
	    sp.edit().putInt(PREF_APP_VERSION, forAppVersion).commit();
	}
}
