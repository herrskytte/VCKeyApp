package com.vingcard.vingcardkeyapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.gcm.GcmHelper;
import com.vingcard.vingcardkeyapp.model.Country;
import com.vingcard.vingcardkeyapp.model.User;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int COUNTRY_PICKER_REQUEST = 1000;
    private static final int REGISTER_PHONE_REQUEST = 2000;

    private GcmHelper gcmHelper;

    private CallbackReceiver mReceiver;
    private Button mRegisterButton;
    private TextView mCountryTextView;
    private ImageView mCountryImageView;
    private EditText mPhoneEditText;
    private TextView mErrorTextView;

    private Country mSelectedCountry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcmHelper = new GcmHelper(getActivity());
            String gcmRegId = gcmHelper.getRegistrationId();

            if (gcmRegId == null) {
                gcmHelper.registerBackground();
            }

        } else {
            Log.e(TAG, "No valid Google Play Services APK found.");
        }

        mReceiver = new CallbackReceiver(new Handler());

        if(savedInstanceState != null){
            mSelectedCountry = savedInstanceState.getParcelable(CountryPickerFragment.EXTRA_COUNTRY);
        }else{
            new FindDeviceCountry().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_register, null);

        mCountryTextView = (TextView) root.findViewById(R.id.reg_country_selected);
        mCountryImageView = (ImageView) root.findViewById(R.id.reg_country_img);
        mPhoneEditText = (EditText) root.findViewById(R.id.reg_number_input);
        mErrorTextView = (TextView) root.findViewById(R.id.reg_error);
        mRegisterButton = (Button) root.findViewById(R.id.reg_button);

        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);

                //Validate input
                if(mSelectedCountry == null || TextUtils.isEmpty(mPhoneEditText.getText())){
                    mErrorTextView.setText(R.string.error_phone_input);
                    mErrorTextView.setVisibility(View.VISIBLE);
                    return;
                }
                //Validate registration-id received
                else if(gcmHelper.getRegistrationId() == null){
                    mErrorTextView.setText(R.string.error_register);
                    mErrorTextView.setVisibility(View.VISIBLE);
                    return;
                }
                else{
                    mErrorTextView.setVisibility(View.GONE);
                }

                //Store data in preferences
                final User u = new User();
                u.registrationId = gcmHelper.getRegistrationId();
                u.phoneNumber = "+" + mSelectedCountry.getPhoneCode() + mPhoneEditText.getText();
                PreferencesUtil.setUserData(getActivity(), u);
                PreferencesUtil.setCountryCode(getActivity(), mSelectedCountry.getCode());
                PreferencesUtil.setNumber(getActivity(), mPhoneEditText.getText().toString());

                startActivityForResult(new Intent(getActivity(), WaitingActivity.class), REGISTER_PHONE_REQUEST);
            }
        });

        mCountryTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorTextView.setVisibility(View.GONE);
                Intent pickerIntent = new Intent(getActivity(), CountryPickerActivity.class);
                startActivityForResult(pickerIntent, COUNTRY_PICKER_REQUEST);
            }
        });

        updateCountryView();
        mPhoneEditText.setText(PreferencesUtil.getNumber(getActivity()));

        showAppVersion(root.findViewById(R.id.reg_version));

        return root;
    }

    //Print version of app
    private void showAppVersion(View versionTextView) {
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) versionTextView).setText("Version: " + pInfo.versionName);
        } catch (Exception e) {Log.e(TAG,"No version found");}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CountryPickerFragment.EXTRA_COUNTRY, mSelectedCountry);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == COUNTRY_PICKER_REQUEST && resultCode == Activity.RESULT_OK){
            mSelectedCountry = data.getParcelableExtra(CountryPickerFragment.EXTRA_COUNTRY);
            updateCountryView();
        }
        else if(requestCode == REGISTER_PHONE_REQUEST && resultCode == Activity.RESULT_OK){
            getActivity().finish();
        }
    }

    private void updateCountryView() {
        if(mSelectedCountry != null){
            mCountryTextView.setText(mSelectedCountry.getName() + " (+" + mSelectedCountry.getPhoneCode() + ")");
            mCountryImageView.setImageResource(mSelectedCountry.getImageResId());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(TAG, "This device is not supported.");
                getActivity().finish();
            }
            return false;
        }
        return true;
    }

    private class FindDeviceCountry extends AsyncTask<Void, Void, Void> {
        public static final int COUNTRY_UPDATED = 600;
        public static final String EXTRA_COUNTRY = "com.vingcard.vingcardkeyapp.country";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String locale = PreferencesUtil.getCountryCode(getActivity());
                if(locale == null){
                    locale = getActivity().getResources().getConfiguration().locale.getCountry();
                }

                InputStream inputStream = getActivity().getResources().openRawResource(
                        R.raw.countries);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream));

                Gson gson = new Gson();
                Type listType = new TypeToken<List<Country>>(){}.getType();
                List<Country> countries = gson.fromJson(reader, listType);
                for(Country c : countries){
                    if(locale.equalsIgnoreCase(c.getCode())){
                        Bundle b = new Bundle();
                        b.putParcelable(EXTRA_COUNTRY, c);
                        mReceiver.send(COUNTRY_UPDATED, b);
                        return null;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }

            return null;
        }
    }

    private class CallbackReceiver extends ResultReceiver {

        public CallbackReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult (int resultCode, Bundle resultData) {
            if (resultCode == FindDeviceCountry.COUNTRY_UPDATED){
                mSelectedCountry = resultData.getParcelable(FindDeviceCountry.EXTRA_COUNTRY);
                updateCountryView();
            }
        }
    }
}
