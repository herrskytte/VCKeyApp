package com.vingcard.vingcardkeyapp.ui;

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
import com.vingcard.vingcardkeyapp.util.AlertUtil;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class WaitingFragment extends Fragment {
    static final String TAG = "WaitingFragment";

    CallbackReceiver mReceiver;
    TextView mNumberTextView;
    Button mButton;

    User mUserData;
    String mUserCountryCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReceiver = new CallbackReceiver(new Handler());
        mUserData = PreferencesUtil.getUserData(getActivity());
        mUserCountryCode = PreferencesUtil.getCountryCode(getActivity());

        new RestHelper(getActivity()).registerUser(mUserData, mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_waiting, null);
        mNumberTextView = (TextView) root.findViewById(R.id.waiting_number);
        mButton = (Button) root.findViewById(R.id.waiting_button);

        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return root;
    }

    private class CallbackReceiver extends ResultReceiver {

        public CallbackReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult (int resultCode, Bundle resultData) {
            switch (resultCode) {
                case FormatNumber.NUMBER_FORMATTED:
                    mNumberTextView.setText(resultData.getString(FormatNumber.EXTRA_NUMBER));
                    break;

                case RestHelper.HTTP_OK:
                    Log.e(TAG, "Register success");
                    Toast.makeText(getActivity(), R.string.reg_success, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                    break;

                case RestHelper.SMS_RECEIVING:
                    Log.e(TAG, "Unable to get SMS");
                    AlertUtil.showErrorMessage(getActivity(), getString(R.string.error_title), getString(R.string.error_sms_receive));
                    break;

                default:
                    Log.e(TAG, "Registration error");
                    AlertUtil.showErrorMessage(getActivity(), getString(R.string.error_title), getString(R.string.error_register));
                    break;
            }
        }
    }

    private class FormatNumber extends AsyncTask<Void, Void, Void> {
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
