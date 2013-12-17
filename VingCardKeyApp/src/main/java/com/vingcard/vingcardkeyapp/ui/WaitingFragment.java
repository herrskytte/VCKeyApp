package com.vingcard.vingcardkeyapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.service.RestHelper;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class WaitingFragment extends Fragment {
    private static final String TAG = "WaitingFragment";

    private CallbackReceiver mReceiver;
    private TextView mNumberTextView;
    private View mWaitView;
    private TextView mErrorTextView;
    private Button mChangeNumberButton;
    private Button mRetryButton;

    private User mUserData;
    private String mUserCountryCode;

    private AsyncTask mRegistrationTask;

    private final static String STATUS = "status";
    private final static int STATUS_REGISTERING = 1;
    private final static int STATUS_SMS_ERROR = 2;
    private final static int STATUS_INTERNET_ERROR = 3;
    private final static int STATUS_SERVER_ERROR = 4;
    private int mCurrentStatus = 1;

    private final static String NUMBER = "number";
    private String mFormattedNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mReceiver = new CallbackReceiver(new Handler());
        mUserData = PreferencesUtil.getUserData(getActivity());
        mUserCountryCode = PreferencesUtil.getCountryCode(getActivity());

        if(savedInstanceState == null){
            new FormatNumberTask().execute();
            mRegistrationTask = new RestHelper(getActivity()).registerUser(mUserData, mReceiver);
        }
        else{
            mCurrentStatus = savedInstanceState.getInt(STATUS);
            mFormattedNumber = savedInstanceState.getString(NUMBER);
            if(mFormattedNumber == null){
                new FormatNumberTask().execute();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_waiting, null);
        mNumberTextView = (TextView) root.findViewById(R.id.waiting_number);
        mWaitView = root.findViewById(R.id.waiting_progress_layout);
        mErrorTextView = (TextView) root.findViewById(R.id.waiting_error);
        mChangeNumberButton = (Button) root.findViewById(R.id.waiting_button_change);
        mRetryButton = (Button) root.findViewById(R.id.waiting_button_retry);

        mChangeNumberButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        mRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRegistrationTask.getStatus() == AsyncTask.Status.FINISHED){
                    mCurrentStatus = STATUS_REGISTERING;
                    updateViewFromStatus();
                    mRegistrationTask = new RestHelper(getActivity()).registerUser(mUserData, mReceiver);
                }
            }
        });

        if(mFormattedNumber == null){
            mNumberTextView.setText(mUserData.phoneNumber);
        }else{
            mNumberTextView.setText(mFormattedNumber);
        }
        updateViewFromStatus();
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATUS, mCurrentStatus);
        outState.putString(NUMBER, mFormattedNumber);
    }

    private void updateViewFromStatus(){
        switch (mCurrentStatus){
            case STATUS_REGISTERING:
                showWaitingMessage();
                break;
            case STATUS_SMS_ERROR:
                mErrorTextView.setText(R.string.error_sms_receive);
                showErrorMessage();
                break;
            case STATUS_INTERNET_ERROR:
                mErrorTextView.setText(R.string.error_register);
                showErrorMessage();
                break;
            case STATUS_SERVER_ERROR:
                mErrorTextView.setText(R.string.error_server);
                showErrorMessage();
                break;
        }
    }

    private void showErrorMessage() {
        mWaitView.setVisibility(View.GONE);
        mRetryButton.setVisibility(View.VISIBLE);
        mErrorTextView.setVisibility(View.VISIBLE);
    }

    private void showWaitingMessage() {
        mWaitView.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.GONE);
        mErrorTextView.setVisibility(View.GONE);
    }

    private class CallbackReceiver extends ResultReceiver {

        public CallbackReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult (int resultCode, Bundle resultData) {
            if(getActivity() == null){
                return;
            }
            switch (resultCode) {
                case FormatNumberTask.NUMBER_FORMATTED:
                    mFormattedNumber = resultData.getString(FormatNumberTask.EXTRA_NUMBER);
                    mNumberTextView.setText(mFormattedNumber);
                    break;

                case RestHelper.HTTP_OK:
                    Log.e(TAG, "Register success");
                    Toast.makeText(getActivity(), R.string.reg_success, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().setResult(Activity.RESULT_OK, null);
                    getActivity().finish();
                    break;
                case  RestHelper.SERVER_FAILED:
                    Log.e(TAG, "Server error");
                    mCurrentStatus = STATUS_SERVER_ERROR;
                    updateViewFromStatus();
                    break;

                case RestHelper.SMS_RECEIVING:
                    Log.e(TAG, "Unable to get SMS");
                    mCurrentStatus = STATUS_SMS_ERROR;
                    updateViewFromStatus();
                    break;

                default:
                    Log.e(TAG, "Registration error");
                    mCurrentStatus = STATUS_INTERNET_ERROR;
                    updateViewFromStatus();
                    break;
            }
        }
    }

    private class FormatNumberTask extends AsyncTask<Void, Void, Void> {
        public static final int NUMBER_FORMATTED = 600;
        public static final String EXTRA_NUMBER = "com.vingcard.vingcardkeyapp.number";

        @Override
        protected Void doInBackground(Void... params) {
            String formattedNumber;
            try {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                PhoneNumber phoneNumber = phoneUtil.parse(mUserData.phoneNumber, mUserCountryCode);
                formattedNumber = phoneUtil.format(phoneNumber,PhoneNumberFormat.INTERNATIONAL);
            } catch (NumberParseException e) {
                formattedNumber = mUserData.phoneNumber;
            }
            Bundle b = new Bundle();
            b.putString(EXTRA_NUMBER, formattedNumber);
            mReceiver.send(NUMBER_FORMATTED, b);
            return null;
        }
    }
}
