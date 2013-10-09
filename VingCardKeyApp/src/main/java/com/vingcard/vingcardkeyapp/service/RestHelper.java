package com.vingcard.vingcardkeyapp.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.vingcard.vingcardkeyapp.model.Hotel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.ResultReceiver;
import android.util.Log;

import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.sms.SmsHelper;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;


public class RestHelper {
	private static final String TAG = "RestHelper";

	// Result-callbacks
	public static final int HTTP_OK = 200;
	public static final int HTTP_FAILED = -400;
	
	//Registration-callbacks
	public static final int SMS_REQUESTING = 300;
	public static final int SMS_RECEIVING = 302;
	public static final int REGISTERING = 304;

	// Url to AppHarbor demo server
	protected static final String BASE_URL = "http://vingcardportal.apphb.com/PhoneAppKeysService.svc/";

	protected RestTemplate restTemplate;
	protected HttpHeaders requestHeaders;

	protected ResultReceiver callback;
	protected int responseResult = HTTP_OK;

	protected Context context;

	public RestHelper(Context context) {

		this.context = context;

		// Create a new RestTemplate instance
		restTemplate = new RestTemplate();

		// Add the Gson message converter
		restTemplate.getMessageConverters().add(new VingCardGsonConverter());
		
		// Add a manual logger
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
		LoggerInterceptor loggerInterceptor = new LoggerInterceptor();
		interceptors.add(loggerInterceptor);
		restTemplate.setInterceptors(interceptors);

		// Set the Accept header
		requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(new MediaType(
				"application", "json"));
		requestHeaders.setAccept(Collections.singletonList(new MediaType(
				"application", "json")));
		requestHeaders.set("Connection", "Close");

	}

	protected HttpEntity<Object> getDefaultRequestEntity(){
		return new HttpEntity<Object>(requestHeaders);
	}

	public void getKeyCards(long userId, ResultReceiver callback) {
		this.callback = callback;
		new GetKeyCardsTask().execute(userId);
	}


    private class GetKeyCardsTask extends AsyncTask<Long, Void, KeyCard[]> {

		@Override
		protected KeyCard[] doInBackground(Long... params) {
			try {
				long userId = params[0];

				String url = BASE_URL + "user/" + userId + "/keys";
				
				ResponseEntity<KeyCard[]> responseEntity = restTemplate
						.exchange(new URI(url), HttpMethod.GET, getDefaultRequestEntity(), KeyCard[].class);


				return responseEntity.getBody();

			} catch (Exception e) {
				Log.e("RestHelper", "Exception: " + e.getMessage());
				responseResult = HTTP_FAILED;
			}

			return null;
		}

		@Override
		protected void onPostExecute(KeyCard[] keyCardList) {
			if(keyCardList != null){
				List<KeyCard> newCards = StorageHelper.storeKeyCards(context, keyCardList);
				if(newCards != null && !newCards.isEmpty()){
					CardNotificationHelper.notifyNewKey(context, newCards.get(0));
				}				
			}
		}
	}

	/**
	 * Method used to post door events to server and then delete them. Synchronous operation.
	 */
	public boolean sendDoorEvent(DoorEvent doorEvent) {

		String url = BASE_URL + "status";

		HttpEntity<DoorEvent> requestEntity = new HttpEntity<DoorEvent>(doorEvent, requestHeaders);
		
		try {
			ResponseEntity<Object> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
			Log.e("RestHelper", "SendEventStatus: " + responseEntity.getStatusCode());
		} catch (RestClientException e) {
			Log.e("RestHelper", "SendEventException: " + e.getMessage());
			return false;
		}
		
		return StorageHelper.deleteEvent(context, doorEvent);
	}

	/**
	 * Method used for registering user.
	 * Requests a code from the server via SMS and then registers with the received code
	 */
	public void registerUser(User user, ResultReceiver mReceiver) {
		this.callback = mReceiver;
		new RegisterUserTask().execute(user);
	}

	private class RegisterUserTask extends AsyncTask<User, Void, User> {

		private ProgressDialog progressDialog;
		private boolean canceled = false;
		
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(context);
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					canceled = true;
				}
			});
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.show();
			progressDialog.setMessage("Waiting for confirmation SMS...");
		}

		@Override
		protected User doInBackground(User... params) {
			try {
				User userObject = params[0];
				String url = BASE_URL + "user";

				//Requesting SMS from server
				responseResult = SMS_REQUESTING;
				String smsCode = getSmsCode();
				if(smsCode == null){
					User postObject = new User();
					postObject.phoneNumber = userObject.phoneNumber;
					
					HttpEntity<User> requestEntity = new HttpEntity<User>(postObject, requestHeaders);
					restTemplate.exchange(url, HttpMethod.POST, requestEntity, User.class);
				}
				
				//Listening for SMS and checking inbox
				responseResult = SMS_RECEIVING;
				int i = 0;
				while(smsCode == null){
					if(i++ > 100 || canceled){
						return null;
					}
					
					Thread.sleep(300);         
					smsCode = getSmsCode();
				}
				Log.e(TAG, "Got SMS in: " + (i*0.3) + " s");
				
				//Registering user on server
				responseResult = REGISTERING;
				userObject.registrationCode = smsCode;
				HttpEntity<User> requestEntity =  new HttpEntity<User>(userObject, requestHeaders);
				ResponseEntity<User> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, User.class);
				
				//User registered ok!
				responseResult = HTTP_OK;
				return responseEntity.getBody();

			} catch (Exception e) {
				Log.e(TAG, "Error registering: " + e.getMessage());
			}

			return null;
		}

		private String getSmsCode() {
			String smsCode;
			smsCode = PreferencesUtil.getSmsCode(context);
			if(smsCode == null){
				smsCode = SmsHelper.checkInboxForCode(context);
			}
			return smsCode;
		}

		@Override
		protected void onPostExecute(User user) {
			if (user != null) {
				PreferencesUtil.setUserData(context, user);
			}

			progressDialog.dismiss();
			callback.send(responseResult, null);
		}
	}
	
	public boolean updateUser(User user){
		String url = BASE_URL + "user/" + user.id;
		try{
			HttpEntity<User> requestEntity = new HttpEntity<User>(user, requestHeaders);
			restTemplate.exchange(url, HttpMethod.PUT, requestEntity, User.class);
			return true;
		} catch(Exception e){
			Log.e(TAG, "Error updating user: " + e.getMessage());
			return false;
		}
	}

    public Hotel getHotelData(String hotelId) {
        String url = BASE_URL + "hotel/" + hotelId;
//        try{
//            ResponseEntity<Hotel> responseEntity = restTemplate
//                    .exchange(new URI(url), HttpMethod.GET, getDefaultRequestEntity(), Hotel.class);
//            return responseEntity.getBody();
//        } catch(Exception e){
//            Log.e(TAG, "Error getting hotel: " + e.getMessage());
//            return null;
//        }
        return createDemoHotel();
    }

    private Hotel createDemoHotel(){
        if(new Random().nextDouble() > 0.5){
            Hotel h = new Hotel();
            h.name = "VingCard Demo Hotel";
            h.address = "Sophus Lies vei, 1523 Moss, Norge";
            h.phone = "+47 69 24 50 00";
            h.email = "info@vingcarddemohotel.com";
            h.website = "vingcardelsafe.com";
            h.logoUrl = "http://www.prlog.org/10390390-vingcard-elsafe.jpg";
            return h;
        }
        else {
            Hotel h = new Hotel();
            h.name = "Sheraton Park Hotel at the Anaheim Resort";
            h.address = "900 South Disneyland Drive Anaheim, California 92802";
            h.phone = "+1 (714) 778-1700";
            h.email = "sheratonpark@starwoorddemos.com";
            h.website = "www.starwoodhotels.com/sheraton";
            h.logoUrl = "http://upload.wikimedia.org/wikipedia/fr/b/bd/Sheraton.png";
            return h;

        }
    }
}
