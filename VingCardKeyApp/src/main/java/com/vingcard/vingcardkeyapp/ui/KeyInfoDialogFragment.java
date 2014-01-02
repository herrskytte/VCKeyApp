package com.vingcard.vingcardkeyapp.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.Hotel;
import com.vingcard.vingcardkeyapp.standard.MyLinearLayout;
import com.vingcard.vingcardkeyapp.standard.MyPagerAdapter;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelDB;
import com.vingcard.vingcardkeyapp.util.AppConstants;

public class KeyInfoDialogFragment extends DialogFragment{

    private TextView mHotelNameText;
	private TextView mRoomNumberText;
    private ImageView mLogoImageView;
    private String mAction;

    private static final float mScale = MyPagerAdapter.SMALL_SCALE;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = null;

        mAction = getArguments().getString(AppConstants.KeySync.DATA_ACTION);
        switch (mAction) {
            case AppConstants.KeySync.ACTION_NEW_KEY:
                view = inflater.inflate(R.layout.dialog_new_key, null);
                //If new key, show and start animation
                ImageView phoneImageView = (ImageView) view.findViewById(R.id.keydialog_phone);
                //load the phone movement animation
                AnimatorSet phoneSet = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.phone_anim);
                //set the view as target
                phoneSet.setTarget(phoneImageView);
                //start the animation
                phoneSet.start();

                ((TextView) view.findViewById(R.id.keydialog_header)).setText(R.string.key_notification_new);

                //builder.setTitle(R.string.key_notification_new);
                break;
            case AppConstants.KeySync.ACTION_UPDATED_KEY:
                view = inflater.inflate(R.layout.dialog_updated_key, null);
                ((TextView) view.findViewById(R.id.keydialog_header)).setText(R.string.key_notification_update);

                //builder.setTitle(R.string.key_notification_update);
                break;
            case AppConstants.KeySync.ACTION_REVOKED_KEY:
                view = inflater.inflate(R.layout.dialog_updated_key, null);
                ((TextView) view.findViewById(R.id.keydialog_header)).setText(R.string.key_notification_revoke);

                //builder.setTitle(R.string.key_notification_revoke);
                break;
        }

        MyLinearLayout mCardLayout = (MyLinearLayout) view.findViewById(R.id.keydialog_card);
		mCardLayout.setScaleBoth(mScale, false);
		
		mHotelNameText = (TextView) view.findViewById(R.id.card_hotel_name);
		mRoomNumberText = (TextView) view.findViewById(R.id.card_room);
        mLogoImageView = (ImageView) view.findViewById(R.id.card_hotel_logo);

		//Hide status views
		view.findViewById(R.id.card_status_text).setVisibility(View.GONE);
		view.findViewById(R.id.card_status_image).setVisibility(View.GONE);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
		builder.setView(view);

//		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int whichButton) {
//				dismiss();
//			}
//		});
		return builder.create();
	}

	@Override
	public void onResume() {
		super.onResume();

		Uri keyCardUri = getArguments().getParcelable("_uri");
		Cursor c = getActivity().getContentResolver().query(keyCardUri, KeyCardQuery.PROJECTION, null, null, null);
		if(c.moveToFirst()){
            mRoomNumberText.setText(getString(R.string.card_room, c.getString(KeyCardQuery.KEYCARD_LABEL)));

            Uri hotelUri = HotelDB.buildHotelUri(c.getString(KeyCardQuery.KEYCARD_HOTEL_ID));
            Cursor c2 = getActivity().getContentResolver().query(hotelUri, HotelQuery.PROJECTION, null, null, null);
            if(c2.moveToFirst()){
                mHotelNameText.setText(c2.getString(HotelQuery.HOTEL_NAME));
                String fullLogoUrl = Hotel.CreateFullLogoUrl(c2.getString(HotelQuery.HOTEL_LOGO_URL));
                if(fullLogoUrl != null){
                    Picasso picasso = Picasso.with(getActivity());
                    picasso.setDebugging(true);
                    picasso.load(fullLogoUrl)
                            .centerInside()
                            .fit()
                            .into(mLogoImageView);
                }
            }
            c2.close();
		}
        c.close();

        if(AppConstants.KeySync.ACTION_NEW_KEY.equals(mAction)){
            //Manually increase width of dialog in landscape beyond its default max width
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                int width = getResources().getDimensionPixelSize(R.dimen.newkeydialog_width_land);
                getDialog().getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        }
	}

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity().isFinishing()) {
            // Always cancel the request here, this is safe to call even if the image has been loaded.
            // This ensures that the anonymous callback we have does not prevent the activity from
            // being garbage collected. It also prevents our callback from getting invoked even after the
            // activity has finished.
            Picasso.with(getActivity()).cancelRequest(mLogoImageView);
        }
    }

    private interface KeyCardQuery {
		String[] PROJECTION = { KeyCardDB.KEYCARD_LABEL,
                                KeyCardDB.KEYCARD_HOTEL_ID};
		int KEYCARD_LABEL = 0;
        int KEYCARD_HOTEL_ID = 1;
	}

    private interface HotelQuery {
        String[] PROJECTION = { HotelDB.HOTEL_NAME,
                                      HotelDB.HOTEL_LOGO_URL};
        int HOTEL_NAME = 0;
        int HOTEL_LOGO_URL = 1;
    }
}
