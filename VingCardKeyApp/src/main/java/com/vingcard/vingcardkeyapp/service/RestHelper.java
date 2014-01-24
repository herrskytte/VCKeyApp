package com.vingcard.vingcardkeyapp.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.ResultReceiver;
import android.util.Log;
import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.model.Hotel;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.sms.SmsHelper;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.util.AppConstants;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RestHelper {
	private static final String TAG = "RestHelper";

	// Result-callbacks
	public static final int HTTP_OK = 200;
	public static final int HTTP_FAILED = -400;
	public static final int SERVER_FAILED = -500;

	//Registration-callbacks
	public static final int SMS_REQUESTING = 300;
	public static final int SMS_RECEIVING = 302;
	public static final int REGISTERING = 304;
	public static final int WRONG_CODE = 306;

	private RestTemplate restTemplate;
	private HttpHeaders requestHeaders;

	private ResultReceiver callback;
	private int responseResult = HTTP_OK;

	private Context context;

	public RestHelper(Context context) {

		this.context = context;

		// Create a new RestTemplate instance
		restTemplate = new RestTemplate();

		// Add the Gson message converter
		restTemplate.getMessageConverters().add(new VingCardGsonConverter());
		
		// Add a manual logger
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
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

        addBasicAuthentication("VingCard", "V1ngCard!");
	}

    protected void addBasicAuthentication(String username, String password) {
        HttpAuthentication authHeader = new HttpBasicAuthentication(username, password);
        requestHeaders.setAuthorization(authHeader);
    }

	private HttpEntity<Object> getDefaultRequestEntity(){
		return new HttpEntity<>(requestHeaders);
	}

	public void getKeyCards(long userId) {
		new GetKeyCardsTask().execute(userId);
	}

    private class GetKeyCardsTask extends AsyncTask<Long, Void, KeyCard[]> {

		@Override
		protected KeyCard[] doInBackground(Long... params) {
			try {
				long userId = params[0];

				String url = AppConstants.Uris.BASE_URI_REST + "user/" + userId + "/keys";
				
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
					CardNotificationHelper.notifyKeyUpdate(context, newCards.get(0), AppConstants.Broadcasts.ACTION_NEW_KEY);
				}				
			}
		}
	}

	/**
	 * Method used to post door events to server and then delete them. Synchronous operation.
	 */
	public boolean sendDoorEvent(DoorEvent doorEvent) {

		String url = AppConstants.Uris.BASE_URI_REST + "status";

		HttpEntity<DoorEvent> requestEntity = new HttpEntity<>(doorEvent, requestHeaders);
		
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
	public AsyncTask registerUser(User user, ResultReceiver mReceiver) {
		this.callback = mReceiver;
		return new RegisterUserTask().execute(user);
	}

	private class RegisterUserTask extends AsyncTask<User, Void, User> {

		private boolean canceled = false;

		@Override
		protected User doInBackground(User... params) {
            final User userObject = params[0];
            String url = AppConstants.Uris.BASE_URI_REST + "user";

            try {

				//Requesting SMS from server
				responseResult = SMS_REQUESTING;
				String smsCode = getSmsCode();
				if(smsCode == null){
					HttpEntity<User> requestEntity = new HttpEntity<>(userObject, requestHeaders);
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
				HttpEntity<User> requestEntity =  new HttpEntity<>(userObject, requestHeaders);
				ResponseEntity<User> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, User.class);
				
				//User registered ok!
				responseResult = HTTP_OK;
				return responseEntity.getBody();

			}
            catch (HttpClientErrorException ce){
                if(ce.getStatusCode().equals(HttpStatus.NOT_ACCEPTABLE)){
                    PreferencesUtil.setWrongSmsCode(context, userObject.registrationCode);
                    PreferencesUtil.setSmsCode(context, null);
                    userObject.registrationCode = null;
                    responseResult = WRONG_CODE;
                }
                else {
                    responseResult = HTTP_FAILED;
                }
            }
            catch (HttpServerErrorException re){
                responseResult = SERVER_FAILED;
            }
            catch (Exception e) {
				Log.e(TAG, "Error registering: " + e.getMessage());
			}

			return null;
		}

		private String getSmsCode() {
			String smsCode = PreferencesUtil.getSmsCode(context);
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
			callback.send(responseResult, null);
		}
	}
	
	public boolean updateUser(User user){
		String url = AppConstants.Uris.BASE_URI_REST + "user/" + user.id;
		try{
			HttpEntity<User> requestEntity = new HttpEntity<>(user, requestHeaders);
			restTemplate.exchange(url, HttpMethod.PUT, requestEntity, User.class);
			return true;
		} catch(Exception e){
			Log.e(TAG, "Error updating user: " + e.getMessage());
			return false;
		}
	}

    public Hotel getHotelData(String hotelId) {
        String url = AppConstants.Uris.BASE_URI_REST + "hotel/" + hotelId;
        try{
            ResponseEntity<Hotel> responseEntity = restTemplate
                    .exchange(new URI(url), HttpMethod.GET, getDefaultRequestEntity(), Hotel.class);
            return responseEntity.getBody();
        } catch(Exception e){
            Log.e(TAG, "Error getting hotel: " + e.getMessage());
            return null;
        }
    }
}
