package com.vingcard.vingcardkeyapp.gcm;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.service.RestHelper;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class GcmHelper {
	static final String TAG = "GcmHelper";
	
	private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    String SENDER_ID = "917619851459";

    GoogleCloudMessaging gcm;
    Context context;

    String regid;

	public GcmHelper(Context context) {
		super();
		this.context = context;
	}

	/**
	 * Gets the current registration id for application on GCM service.
	 * If result is empty, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not complete.
	 */
	public String getRegistrationId() {
	    String registrationId = PreferencesUtil.getGcmRegistrationId(context);
	    if (registrationId == null) {
	        Log.e(TAG, "Registration not found.");
	        return null;
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = PreferencesUtil.getStoredAppVersion(context);
	    int currentVersion = getAppVersion();
	    if (registeredVersion != currentVersion) {
	        Log.e(TAG, "App version changed.");
	        return null;
	    }
	    return registrationId;
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * Stores the registration id and app versionCode in the 
	 * application's shared preferences.
	 * Updates the user on VingCard-server if user is allready registered
	 */
	public void registerBackground() {
	    new AsyncTask<Void, Void, Void>() {
	        @Override
	        protected Void doInBackground(Void... params) {
	        	long backoff = BACKOFF_MILLI_SECONDS;
	            // Once GCM returns a registration id, we need to register it in the
	            // VingCard server. As the server might be down, we will retry it a couple
	            // times.
	            for (int i = 1; i <= MAX_ATTEMPTS; i++) {
	                try {
	                	if (gcm == null) {
		                    gcm = GoogleCloudMessaging.getInstance(context);
		                }
		                regid = gcm.register(SENDER_ID);
		                
		                User u = PreferencesUtil.getUserData(context);
		                //User not registered. Store GCM for pending registration
		                if(u.id == 0){
		                	PreferencesUtil.setRegistrationId(context, regid, getAppVersion());	 
		                	Log.e(TAG, "Device registered, registration id=" + regid);
		                	return null;
		                }
		                //User registered. Update registration ID on server
		                else{
		                	u.registrationId = regid;
		                	RestHelper restHelper = new RestHelper(context);
		                	if(restHelper.updateUser(u)){
		                		PreferencesUtil.setRegistrationId(context, regid, getAppVersion());
			                	Log.e(TAG, "User updated, registration id=" + regid);
		                		return null;
		                	}
		                }
	                } catch (IOException e) {
	                	Log.e(TAG, "Failed to register on attempt " + i + ":" + e.getMessage());
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
	                        return null;
	                    }
	                    // increase backoff exponentially
	                    backoff *= 2;
	                }
	            }
	            return null;
	        }
	    }.execute(null, null, null);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private int getAppVersion() {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
    
}
