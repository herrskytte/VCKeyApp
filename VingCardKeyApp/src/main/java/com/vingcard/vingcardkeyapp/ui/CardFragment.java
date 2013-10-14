package com.vingcard.vingcardkeyapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import com.squareup.picasso.Picasso;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.standard.AnimationFactory;
import com.vingcard.vingcardkeyapp.standard.AnimationFactory.FlipDirection;
import com.vingcard.vingcardkeyapp.standard.MyLinearLayout;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.util.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.Days;

public class CardFragment extends Fragment {
	
	private static final String ARG_SCALE = "scale";
	private static final String ARG_CARD = "keycard";
	
	private float mScale;
	private KeyCard mKeyCard;
	
	private ViewFlipper mViewFlipper;
	
	//Front
	private MyLinearLayout mCardRoot;
	private Button mRemoveButton;
	private ImageView mHotelLogoImage;
	private TextView mHotelNameText;
	private TextView mRoomText;
	private ImageView mStatusImage;
	private TextView mStatusText;

	//Back
	private MyLinearLayout mCardBackRoot;
    private FragmentTabHost mTabHost;
	private TextView mCheckinText;
	private TextView mCheckoutText;
	
	
	public static Fragment newInstance(Context context, float scale, KeyCard keyCard)
	{
		Bundle b = new Bundle();
		b.putFloat(ARG_SCALE, scale);
		b.putParcelable(ARG_CARD, keyCard);

		return Fragment.instantiate(context, CardFragment.class.getName(), b);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putFloat(ARG_SCALE, mScale);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View root = inflater.inflate(R.layout.fragment_card_flipper, container, false);
		
		mViewFlipper = (ViewFlipper) root.findViewById(R.id.card_flipper);	
		mCardRoot = (MyLinearLayout) root.findViewById(R.id.card_root);
		mRemoveButton = (Button) root.findViewById(R.id.card_remove_button);
		mHotelLogoImage =  (ImageView) root.findViewById(R.id.card_hotel_logo);
		mHotelNameText =  (TextView) root.findViewById(R.id.card_hotel_name);
		mRoomText =  (TextView) root.findViewById(R.id.card_room);
		mStatusImage =  (ImageView) root.findViewById(R.id.card_status_image);
		mStatusText =  (TextView) root.findViewById(R.id.card_status_text);

		mCardBackRoot = (MyLinearLayout) root.findViewById(R.id.card_back_root);
        mTabHost = (FragmentTabHost) root.findViewById(android.R.id.tabhost);
		mCheckinText =  (TextView) root.findViewById(R.id.card_back_checkin_date);
		mCheckoutText =  (TextView) root.findViewById(R.id.card_back_checkout_date);

		
		if(getArguments() != null){
			if(savedInstanceState != null && savedInstanceState.getFloat(ARG_SCALE, 0) != 0){
				mScale = savedInstanceState.getFloat(ARG_SCALE);
			}else{
				mScale = getArguments().getFloat(ARG_SCALE);				
			}
			mCardRoot.setScaleBoth(mScale, true);		
			mCardBackRoot.setScaleBoth(mScale, true);
			
			mKeyCard = getArguments().getParcelable(ARG_CARD);		

            setupCardFront();

            setupCardBack();

            setupButtonListeners();
        }
		return root;
	}

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity().isFinishing()) {
            // Always cancel the request here, this is safe to call even if the image has been loaded.
            // This ensures that the anonymous callback we have does not prevent the activity from
            // being garbage collected. It also prevents our callback from getting invoked even after the
            // activity has finished.
            Picasso.with(getActivity()).cancelRequest(mHotelLogoImage);
        }
    }

    private void setupCardFront() {
        if(mKeyCard.hotel != null){
            mHotelNameText.setText(mKeyCard.hotel.name);
            mRoomText.setText("Room 203");
            Picasso picasso = Picasso.with(getActivity());
            picasso.setDebugging(true);
            picasso.load(mKeyCard.hotel.logoUrl).centerInside().resizeDimen(R.dimen.card_hotel_logo_width,R.dimen.card_hotel_logo_height).into(mHotelLogoImage);
        }

        if(mKeyCard.isActive()){
            mStatusImage.setImageResource(R.drawable.ic_nfc);
            mStatusText.setText(R.string.card_valid);
        }
        else{
            mStatusImage.setImageResource(R.drawable.ic_nfc_inactive);
            if(mKeyCard.isWaitingToBeActive()){
                Days d = Days.daysBetween(new DateTime(), mKeyCard.validFrom);
                int days = d.getDays();
                if(days == 1){
                    mStatusText.setText(R.string.card_valid_waiting_one);
                }else{
                    mStatusText.setText(getString(R.string.card_valid_waiting, days));
                }
            }
            else if(mKeyCard.isExpired()){
                mStatusText.setText(R.string.card_valid_expired);
                mRemoveButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupCardBack() {
        if(mKeyCard.validFrom != null && mKeyCard.validTo != null){
            mCheckinText.setText(DateUtil.getFormattedDate(mKeyCard.validFrom.toLocalDate()));
            mCheckoutText.setText(DateUtil.getFormattedDate(mKeyCard.validTo.toLocalDate()));
        }
        if(mKeyCard.hotel != null){
            mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.realtabcontent);

            ViewGroup.LayoutParams lp = mTabHost.getTabWidget().getLayoutParams();
            lp.height = (int) getResources().getDimension(R.dimen.tab_height);
            View v = mTabHost.getTabWidget();
            v.setBackgroundColor(Color.BLACK);

            Bundle bundleAdr = new Bundle();
            bundleAdr.putInt(CardTabFragment.TYPE, CardTabFragment.TYPE_ADDRESS);
            bundleAdr.putString(CardTabFragment.DATA, mKeyCard.hotel.address);
            mTabHost.addTab(mTabHost.newTabSpec("tabAdr").setIndicator(customTabView(R.drawable.ic_action_location_black)), CardTabFragment.class, bundleAdr);

            Bundle bundleWeb = new Bundle();
            bundleWeb.putInt(CardTabFragment.TYPE, CardTabFragment.TYPE_WEB);
            bundleWeb.putString(CardTabFragment.DATA, mKeyCard.hotel.website);
            mTabHost.addTab(mTabHost.newTabSpec("tabWeb").setIndicator(customTabView(R.drawable.ic_action_globe_black)), CardTabFragment.class, bundleWeb);

            Bundle bundleMail = new Bundle();
            bundleMail.putInt(CardTabFragment.TYPE, CardTabFragment.TYPE_MAIL);
            bundleMail.putString(CardTabFragment.DATA, mKeyCard.hotel.email);
            mTabHost.addTab(mTabHost.newTabSpec("tabMail").setIndicator(customTabView(R.drawable.ic_action_mail_black)), CardTabFragment.class, bundleMail);

            Bundle bundlePhone = new Bundle();
            bundlePhone.putInt(CardTabFragment.TYPE, CardTabFragment.TYPE_PHONE);
            bundlePhone.putString(CardTabFragment.DATA, mKeyCard.hotel.phone);
            mTabHost.addTab(mTabHost.newTabSpec("tabPhone").setIndicator(customTabView(R.drawable.ic_action_phone_start_black)), CardTabFragment.class, bundlePhone);
        }
        else{
            mTabHost.setVisibility(View.GONE);
        }
    }

    private View customTabView(int imageId) {
        ImageView imgTab = (ImageView) getActivity().getLayoutInflater().inflate(
                R.layout.card_tab_indicator, null);
        imgTab.setImageResource(imageId);
        return imgTab;
    }

    private void setupButtonListeners() {
		mViewFlipper.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if(!mViewFlipper.isFlipping()){
					AnimationFactory.flipTransition(mViewFlipper, FlipDirection.LEFT_RIGHT);					
				}
			}
		});

		mRemoveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Animate out the card then remove it from the list
				Animation animation = AnimationFactory.fadeOutAnimation(1000, 0);
				animation.setAnimationListener(new AnimationListener() { 
					@Override
					public void onAnimationEnd(Animation animation) {
						mViewFlipper.setVisibility(View.GONE);
						StorageHelper.hideKeyCard(getActivity(), mKeyCard);
					} 
					@Override
					public void onAnimationRepeat(Animation animation) {}  
					@Override
					public void onAnimationStart(Animation animation) {} 
			    });
			    
				mViewFlipper.startAnimation(animation); 
			}
		});
	}
	
	public void setScale(float mScale) {
		this.mScale = mScale;
		mCardRoot.setScaleBoth(mScale, true);		
		mCardBackRoot.setScaleBoth(mScale, true);
	}
	
	public void turnCardToFront(){
		if(mViewFlipper.getDisplayedChild() > 0){
			mViewFlipper.showPrevious();
		}
	}
}
