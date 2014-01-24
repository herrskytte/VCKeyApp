package com.vingcard.vingcardkeyapp.ui;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.DoorEvent;
import com.vingcard.vingcardkeyapp.service.EventSyncService;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;


public class DoorHandlerFragment extends DialogFragment implements SensorEventListener {

	//Sensor variables
	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
    private static final float NOISE = (float) 3;

    private static final int READ_FAILED = 1;
    private static final int KEY_NOT_FOUND = 2;
    private static final int WRITE_FAILED = 3;
    private static final int SUCCESS = 4;

	private TextView mHelpTextView;
	private ImageView mStatusImageView;
	private ImageView mLockImageView;
	private ImageView mPhoneImageView;
	private int mCurrentStatus;
	private View mDoorAnimView;
	private String mCurrentHotelId;
	private String mCurrentRoomId;
	private String mCurrentEventData;
	private boolean mCardCheckedIn;
	private String mCurrentCardId;
	private String output;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		output = "";

		Tag tag = getArguments().getParcelable(NfcAdapter.EXTRA_TAG);
		MifareUltralight ultralight = null;
		if(tag != null){
			ultralight = MifareUltralight.get(tag);
		}
		if(ultralight == null){
			mCurrentStatus = READ_FAILED;
			return;
		}

		try {
			ultralight.connect();
			ultralight.setTimeout(2000);
			readData(ultralight);

			output += "Hotel: " + mCurrentHotelId + "\n";
			output += "Room: " + mCurrentRoomId + "\n";

			byte[] key = getKeyFromDB();
			if(key == null){
				mCurrentStatus = KEY_NOT_FOUND;
				return;
			}

			for(int i=0; i < key.length-1; i=i+4) {
				byte[] b = new byte[4];
				System.arraycopy(key, i, b, 0, 4);
				ultralight.writePage( ((i/4)+10), b);
			}
			ultralight.close();
			mCurrentStatus = SUCCESS;
		} catch (Exception e) {
			mCurrentStatus = WRITE_FAILED;
			output = "Failed: " + e.getMessage();
		}

		//Issue a vibrate to indicate that tag was scanned successfully
		Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		if(vibrator != null)
			vibrator.vibrate(150);
		Log.e("DoorHandler:", output);	
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_door_handler, null);

		mStatusImageView = (ImageView) view.findViewById(R.id.doordialog_status);
		mHelpTextView = (TextView) view.findViewById(R.id.doordialog_helptext);
		mLockImageView = (ImageView) view.findViewById(R.id.doordialog_lock);
		mPhoneImageView = (ImageView) view.findViewById(R.id.doordialog_phone);
		mDoorAnimView = view.findViewById(R.id.doordialog_anim);

		updateDialogFromStatus();

		getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
		getDialog().setCancelable(true);
		getDialog().setCanceledOnTouchOutside(true);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if(mCurrentStatus == SUCCESS && mCurrentEventData != null){
			DoorEvent event = new DoorEvent();
            event.hotelId = mCurrentHotelId;
            event.roomId = mCurrentRoomId;
            event.cardId = mCurrentCardId;
            event.statusData = mCurrentEventData;

            StorageHelper.storeEvent(getActivity(), event);
            if(!mCardCheckedIn){
                StorageHelper.storeCheckInEvent(getActivity(), event);
            }
			
			//Launch event-service
			Intent mEventSyncIntent = new Intent(getActivity(), EventSyncService.class);
			getActivity().startService(mEventSyncIntent);
		}
	}

	private void updateDialogFromStatus() {
		if(mCurrentStatus == SUCCESS){
			mStatusImageView.setImageResource(R.drawable.success);

			AnimatorSet phoneSet = (AnimatorSet) 
					AnimatorInflater.loadAnimator(getActivity(), R.animator.remove_phone_anim);
			phoneSet.setTarget(mPhoneImageView);
			phoneSet.start();

			startMotionSensor();
		}
		else if(mCurrentStatus == KEY_NOT_FOUND){
			mDoorAnimView.setVisibility(View.GONE);
			mStatusImageView.setImageResource(R.drawable.denied);
			mStatusImageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		}
		else{
			mStatusImageView.setImageResource(R.drawable.failed);
			mHelpTextView.setVisibility(View.VISIBLE);
			mHelpTextView.setText("Please remove phone and try again");
			AnimatorSet phoneSet = (AnimatorSet) 
					AnimatorInflater.loadAnimator(getActivity(), R.animator.retry_phone_anim);
			phoneSet.setTarget(mPhoneImageView);
			phoneSet.start();
		}
	}

	private void startMotionSensor() {
		mInitialized = false;
		mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void onPause() {
		super.onPause();
		if(mSensorManager != null)
			mSensorManager.unregisterListener(this);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if(getActivity() != null){
			getActivity().finish();			
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// ignored
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);

			mLastX = x;
			mLastY = y;
			mLastZ = z;

			if(deltaX + deltaY + deltaZ > NOISE){
				Log.e("Movement", "x: " + deltaX + " y: " + deltaY + " z: " + deltaZ + " tot: " + (deltaX+deltaY+deltaZ));		
				mDoorAnimView.setVisibility(View.GONE);
				mSensorManager.unregisterListener(this);
				mStatusImageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
			}
		}
	}

	private void readData(MifareUltralight ultralight) throws IOException {
		//Read hotel
		byte[] payload = ultralight.readPages(22);//10
		char[] chars = Hex.encodeHex(payload);
		mCurrentHotelId = new String(chars);

		//Read room
		payload = ultralight.readPages(26);//14
		chars = Hex.encodeHex(payload);
		mCurrentRoomId = new String(chars);

		//Read event
		payload = ultralight.readPages(30);//18
		chars = Hex.encodeHex(payload);
		mCurrentEventData = new String(chars);
	}

	private byte[] getKeyFromDB(){
		byte[] key = null;

		Cursor c = getActivity().getContentResolver().query(
				KeyCardDB.CONTENT_URI, 
				KeyCardQuery.PROJECTION, 
				KeyCardDB.WHERE_CORRECT_KEY, 
				new String[]{mCurrentHotelId.toUpperCase(Locale.getDefault()), 
						mCurrentRoomId.toUpperCase(Locale.getDefault()),
						"" + new DateTime().getMillis(),
						"" + new DateTime().getMillis()}, 
						null);
		if(c.moveToNext()){
			key = c.getBlob(KeyCardQuery.KEYCARD_KEY);	
			mCurrentCardId = c.getString(KeyCardQuery.KEYCARD_ID);
			mCardCheckedIn = c.getInt(KeyCardQuery.KEYCARD_CHECKED_IN)>0;
		}
		else{
			Log.e("KeyQueryFailed:", "Hot:" + mCurrentHotelId + " Room:" + mCurrentRoomId + " Ev:" + mCurrentEventData);	
		}
		c.close();
		return key;
	}

	private interface KeyCardQuery {
		String[] PROJECTION = { KeyCardDB.KEYCARD_KEY,
								KeyCardDB.KEYCARD_ID,
								KeyCardDB.KEYCARD_CHECKED_IN};

		int KEYCARD_KEY = 0;
		int KEYCARD_ID = 1;
		int KEYCARD_CHECKED_IN = 2;
	}
}
