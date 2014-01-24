package com.vingcard.vingcardkeyapp.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.service.FormatNumberService;
import com.vingcard.vingcardkeyapp.service.RestHelper;
import com.vingcard.vingcardkeyapp.util.AppConstants;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class WaitingFragment extends Fragment {
    private static final String TAG = "WaitingFragment";

    @InjectView(R.id.waiting_number) TextView mWaitingNumber;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.waiting_error) TextView mWaitingError;
    @InjectView(R.id.waiting_button_change) Button mWaitingButtonChange;
    @InjectView(R.id.waiting_button_retry) Button mWaitingButtonRetry;

    private CallbackReceiver mReceiver;
    private User mUserData;
    private AsyncTask mRegistrationTask;

    private final static String STATUS = "status";
    private final static int STATUS_REGISTERING = 1;
    private final static int STATUS_SMS_ERROR = 2;
    private final static int STATUS_INTERNET_ERROR = 3;
    private final static int STATUS_SERVER_ERROR = 4;
    private final static int STATUS_WRONG_CODE = 5;
    private int mCurrentStatus = 1;

    private final static String NUMBER = "number";
    private String mFormattedNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mReceiver = new CallbackReceiver(new Handler());
        mUserData = PreferencesUtil.getUserData(getActivity());

        if (savedInstanceState == null) {
            mRegistrationTask = new RestHelper(getActivity()).registerUser(mUserData, mReceiver);
        } else {
            mCurrentStatus = savedInstanceState.getInt(STATUS);
            mFormattedNumber = savedInstanceState.getString(NUMBER);
        }

        if (mFormattedNumber == null) {
            //Launch format number-service
            Intent fni = new Intent(getActivity(), FormatNumberService.class);
            getActivity().startService(fni);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_waiting, null);
        ButterKnife.inject(this, root);

        mWaitingButtonRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRegistrationTask.getStatus() == AsyncTask.Status.FINISHED) {
                    mCurrentStatus = STATUS_REGISTERING;
                    updateViewFromStatus();
                    mRegistrationTask = new RestHelper(getActivity()).registerUser(mUserData, mReceiver);
                }
            }
        });

        mWaitingButtonChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        mWaitingNumber.setText(mFormattedNumber == null ? mUserData.phoneNumber : mFormattedNumber);

        updateViewFromStatus();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATUS, mCurrentStatus);
        outState.putString(NUMBER, mFormattedNumber);
    }

    // Broadcast receiver for receiving updates from the IntentService
    private final BroadcastReceiver mNumberReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mFormattedNumber = intent.getStringExtra(AppConstants.Broadcasts.DATA_NUMBER);
            mWaitingNumber.setText(mFormattedNumber);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Listen for incoming broadcasts with formatted number
        IntentFilter intentFilter = new IntentFilter(AppConstants.Broadcasts.BROADCAST_NUMBER);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mNumberReceiver, intentFilter);
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mNumberReceiver);
        super.onDetach();
    }

    private void updateViewFromStatus() {
        switch (mCurrentStatus) {
            case STATUS_REGISTERING:
                showWaitingMessage();
                break;
            case STATUS_SMS_ERROR:
                mWaitingError.setText(R.string.error_sms_receive);
                showErrorMessage();
                break;
            case STATUS_INTERNET_ERROR:
                mWaitingError.setText(R.string.error_register);
                showErrorMessage();
                break;
            case STATUS_SERVER_ERROR:
                mWaitingError.setText(R.string.error_server);
                showErrorMessage();
                break;
            case STATUS_WRONG_CODE:
                mWaitingError.setText(R.string.error_code);
                showErrorMessage();
                break;
        }
    }

    private void showErrorMessage() {
        mProgressBar.setVisibility(View.GONE);
        mWaitingButtonRetry.setVisibility(View.VISIBLE);
        mWaitingError.setVisibility(View.VISIBLE);
    }

    private void showWaitingMessage() {
        mProgressBar.setVisibility(View.VISIBLE);
        mWaitingButtonRetry.setVisibility(View.GONE);
        mWaitingError.setVisibility(View.GONE);
    }

    private class CallbackReceiver extends ResultReceiver {

        public CallbackReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (getActivity() == null) {
                return;
            }
            switch (resultCode) {
                case RestHelper.HTTP_OK:
                    Log.e(TAG, "Register success");
                    Toast.makeText(getActivity(), R.string.reg_success, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().setResult(Activity.RESULT_OK, null);
                    getActivity().finish();
                    break;
                case RestHelper.SERVER_FAILED:
                    Log.e(TAG, "Server error");
                    mCurrentStatus = STATUS_SERVER_ERROR;
                    updateViewFromStatus();
                    break;

                case RestHelper.SMS_RECEIVING:
                    Log.e(TAG, "Unable to get SMS");
                    mCurrentStatus = STATUS_SMS_ERROR;
                    updateViewFromStatus();
                    break;

                case RestHelper.WRONG_CODE:
                    Log.e(TAG, "Wrong code");
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
}
