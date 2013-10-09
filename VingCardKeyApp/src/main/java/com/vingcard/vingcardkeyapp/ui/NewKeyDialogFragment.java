package com.vingcard.vingcardkeyapp.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.standard.MyLinearLayout;
import com.vingcard.vingcardkeyapp.standard.MyPagerAdapter;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;

public class NewKeyDialogFragment extends DialogFragment{

	MyLinearLayout mCardLayout;
	RelativeLayout mCardContent;
	TextView mHotelNameText;
	ImageView mPhoneImageView;

	KeyCard mKeyCard;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_new_key, null);
		
		mCardLayout = (MyLinearLayout) view.findViewById(R.id.keydialog_card);
		mCardLayout.setScaleBoth(MyPagerAdapter.SMALL_SCALE, false);
		
		mCardContent = (RelativeLayout) view.findViewById(R.id.card_content);
		mHotelNameText = (TextView) view.findViewById(R.id.card_hotel_name);
		
		//Hide status views
		view.findViewById(R.id.card_status_text).setVisibility(View.GONE);
		view.findViewById(R.id.card_status_image).setVisibility(View.GONE);
		
		mPhoneImageView = (ImageView) view.findViewById(R.id.keydialog_phone);
		
		//load the phone movement animation
		AnimatorSet phoneSet = (AnimatorSet) 
				AnimatorInflater.loadAnimator(getActivity(), R.animator.phone_anim);
		//set the view as target
		phoneSet.setTarget(mPhoneImageView);
		//start the animation
		phoneSet.start();
		
		builder.setView(view);
		builder.setTitle(R.string.new_key_dialog_title);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dismiss();
			}
		});
		return builder.create();
	}

	@Override
	public void onResume() {
		super.onResume();

		boolean foundCardToShow = false;
		if(getArguments() != null){
			Uri keyCardUri = getArguments().getParcelable("_uri");
			if(keyCardUri != null){
				Cursor c = getActivity().getContentResolver().query(keyCardUri, KeyCardQuery.PROJECTION, null, null, null);
				if(c.moveToFirst()){
					mHotelNameText.setText(c.getString(KeyCardQuery.KEYCARD_LABEL));
					foundCardToShow = true;
				}
				c.close();
			}
		}

		if(!foundCardToShow){
			this.dismiss();
		} 

		//Manually increase width of dialog in landscape beyond its default max width
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			int width = getResources().getDimensionPixelSize(R.dimen.newkeydialog_width_land);
			getDialog().getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);			
		}
	}

	private interface KeyCardQuery {
		String[] PROJECTION = { KeyCardDB.KEYCARD_LABEL };

		int KEYCARD_LABEL = 0;
	}
}
