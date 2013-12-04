package com.vingcard.vingcardkeyapp.ui;

import android.app.LoaderManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.gcm.GcmHelper;
import com.vingcard.vingcardkeyapp.model.Hotel;
import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.service.EventSyncService;
import com.vingcard.vingcardkeyapp.service.RestHelper;
import com.vingcard.vingcardkeyapp.standard.MyPagerAdapter;
import com.vingcard.vingcardkeyapp.standard.SimpleSinglePaneActivity;
import com.vingcard.vingcardkeyapp.storage.StorageHelper;
import com.vingcard.vingcardkeyapp.storage.VingCardContract;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelDB;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;
import com.vingcard.vingcardkeyapp.util.AppConstants;
import com.vingcard.vingcardkeyapp.util.TimeLogger;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class CardsOverviewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
	private static final String TAG = "CardsOverviewFragment";

    private ViewPager mCardsViewPager;
	private View mEmptyView;
    private DialogFragment mKeyDialog;

	private List<KeyCard> mCardsList = new ArrayList<KeyCard>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActivity().getActionBar().hide();

		if(savedInstanceState == null){
			//Check if GCM registration-id needs to be updated (app version changed)
			GcmHelper gcmHelper = new GcmHelper(getActivity());
        	String gcmRegId = gcmHelper.getRegistrationId();
        	if (gcmRegId == null) {
        		gcmHelper.registerBackground();
        	}
			
			
			//Launch sync-service
//			long userId = PreferencesUtil.getUserId(getActivity());
//			if(userId != -1){
//				Intent mServiceIntent = new Intent(getActivity(), KeySyncService.class);
//				mServiceIntent.putExtra(KeySync.DATA_USER_ID, userId);
//				getActivity().startService(mServiceIntent);
//			}		
			
			//Launch event-service
			Intent mEventSyncIntent = new Intent(getActivity(), EventSyncService.class);
			getActivity().startService(mEventSyncIntent);			
		}
        getActivity().getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_cards_overview, null);

		mCardsViewPager = (ViewPager) root.findViewById(R.id.cards_viewpager);
		mEmptyView = root.findViewById(R.id.cards_empty_view);

		// Set margin for pages as a negative number, so a part of next and 
		// previous pages will be showed 
		int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
		int bigPageWidthPx = (int) getResources().getDimension(R.dimen.card_width);
		int smallPageWidthPx = (int) (bigPageWidthPx * MyPagerAdapter.SMALL_SCALE);
		int bigPageMarginPx = (screenWidthPx - bigPageWidthPx) / 2; 
		int smallPageMarginPx = (screenWidthPx - smallPageWidthPx) / 2; 
		int marginPx = bigPageMarginPx + smallPageMarginPx;	
		int px = (int) (marginPx - getResources().getDimension(R.dimen.card_spacing));
		mCardsViewPager.setPageMargin(-px);

		showAppVersion(root.findViewById(R.id.cards_version));
		
		return root;
	}
	
	//Print version of app
	private void showAppVersion(View versionTextView) {
		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			((TextView) versionTextView).setText("Version: " + pInfo.versionName);
		} catch (Exception e) {Log.w(TAG,"Cant tell version");}
	}

	public void handleNewIntent(final Intent intent) {
		if(intent.getData() != null){
			showKeyDialog(intent);
		}

	}

	private void showKeyDialog(Intent keyIntent) {
        if(mKeyDialog != null){
            mKeyDialog.dismissAllowingStateLoss();
        }

        mKeyDialog = new KeyInfoDialogFragment();
        mKeyDialog.setArguments(SimpleSinglePaneActivity.intentToFragmentArguments(keyIntent));
        mKeyDialog.show(getFragmentManager(), "fragment_key_info");

//		FragmentManager fm = getFragmentManager();
//		FragmentTransaction transaction = fm.beginTransaction();
//		transaction.add(keyDialog, "fragment_new_key");
//		transaction.commitAllowingStateLoss(); 
	}
	
	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() == null) {
				return;
			}
			Loader<Cursor> loader = getActivity().getLoaderManager().getLoader(0);
			if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    // Broadcast receiver for receiving status updates from the IntentService
    private final BroadcastReceiver mKeyReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            Uri keyUri = KeyCardDB.buildKeyCardUri(intent.getStringExtra(AppConstants.KeySync.DATA_KEY));
            intent.setData(keyUri);
            showKeyDialog(intent);
        }
    };

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		getActivity().getContentResolver().registerContentObserver(
				VingCardContract.KeyCardDB.CONTENT_URI, true, mObserver);
		
		// Listen for incoming broadcasts with new/updated keys
		IntentFilter keyIntentFilter = new IntentFilter(AppConstants.KeySync.BROADCAST_ACTION);
		keyIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mKeyReceiver, keyIntentFilter);
    }

	@Override
	public void onDetach() {
		getActivity().getContentResolver().unregisterContentObserver(mObserver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mKeyReceiver);
		super.onDetach();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(),
                KeyCardDB.buildKeyCardsWithHotelUri(),
                KeyCardQuery.PROJECTION,
                null,
                null,
                KeyCardDB.DEFAULT_SORT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}
        TimeLogger timings = new TimeLogger(TAG, "Cards loaded");

		mCardsList.clear();
		while(cursor.moveToNext()){
            timings.addSplit("Keycard creation start");
            boolean hidden = cursor.getInt(KeyCardQuery.KEYCARD_HIDDEN)>0;
            if(!hidden){
                KeyCard card = new KeyCard();
                card.id = cursor.getString(KeyCardQuery.KEYCARD_ID);
                card.roomNumber = cursor.getString(KeyCardQuery.KEYCARD_LABEL);
                card.validFrom = new DateTime(cursor.getLong(KeyCardQuery.KEYCARD_VALID_FROM));
                card.validTo = new DateTime(cursor.getLong(KeyCardQuery.KEYCARD_VALID_TO));
                card.revoked = cursor.getInt(KeyCardQuery.KEYCARD_REVOKED) > 0;

                //Check if hotel-data exists, else download now. If fail do not show card.
                if(cursor.getString(KeyCardQuery.HOTEL_NAME) == null){
                    Hotel hotel = new RestHelper(getActivity()).getHotelData(
                            cursor.getString(KeyCardQuery.KEYCARD_HOTEL_ID));
                    if(hotel != null){
                        StorageHelper.storeHotel(getActivity(), hotel);
                        card.hotel = hotel;
                    }else{
                        continue;
                    }
                } else{
                    Hotel hotel = new Hotel();
                    hotel.name = cursor.getString(KeyCardQuery.HOTEL_NAME);
                    hotel.address = cursor.getString(KeyCardQuery.HOTEL_ADDRESS);
                    hotel.email = cursor.getString(KeyCardQuery.HOTEL_EMAIL);
                    hotel.website = cursor.getString(KeyCardQuery.HOTEL_WEBSITE);
                    hotel.phone = cursor.getString(KeyCardQuery.HOTEL_PHONE);
                    hotel.logoUrl = cursor.getString(KeyCardQuery.HOTEL_LOGO_URL);
                    card.hotel = hotel;
                }
                mCardsList.add(card);
            }
		}
        timings.addSplit("Cards created");
        MyPagerAdapter mPagerAdapter = new MyPagerAdapter(getActivity(), getFragmentManager(), mCardsList);
        timings.addSplit("new MyPagerAdapter");
        mCardsViewPager.setAdapter(mPagerAdapter);
        timings.addSplit("setAdapter");
        mCardsViewPager.setOnPageChangeListener(mPagerAdapter);
        timings.addSplit("setOnPageChangeListener");
        mCardsViewPager.setOffscreenPageLimit(mCardsList.size());
        timings.addSplit("setOffscreenPageLimit");
        mPagerAdapter.notifyDataSetChanged();
        timings.addSplit("notifyDataSetChanged");
        //mCardsViewPager.setCurrentItem(0);
        timings.addSplit("setCurrentItem");
        //mPagerAdapter.onPageSelected(0);
        mPagerAdapter.onPageSelected(mCardsViewPager.getCurrentItem());
        timings.addSplit("onPageSelected: " + mCardsViewPager.getCurrentItem());
        timings.dumpToLog();

		if(mCardsList.isEmpty()){
			mEmptyView.setVisibility(View.VISIBLE);
		} else{
			mEmptyView.setVisibility(View.GONE);			
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private interface KeyCardQuery {

		String[] PROJECTION = {
				BaseColumns._ID,
				KeyCardDB.KEYCARD_ID,
				KeyCardDB.KEYCARD_LABEL,
				KeyCardDB.KEYCARD_VALID_FROM,
				KeyCardDB.KEYCARD_VALID_TO,
				KeyCardDB.KEYCARD_REVOKED,
				KeyCardDB.KEYCARD_HIDDEN,
                KeyCardDB.KEYCARD_HOTEL_ID,
                HotelDB.HOTEL_NAME,
                HotelDB.HOTEL_ADDRESS,
                HotelDB.HOTEL_EMAIL,
                HotelDB.HOTEL_PHONE,
                HotelDB.HOTEL_WEBSITE,
                HotelDB.HOTEL_LOGO_URL
        };

		//int _ID = 0;
		int KEYCARD_ID = 1;
		int KEYCARD_LABEL = 2;
		int KEYCARD_VALID_FROM = 3;
		int KEYCARD_VALID_TO = 4;
		int KEYCARD_REVOKED = 5;
		int KEYCARD_HIDDEN = 6;
		int KEYCARD_HOTEL_ID = 7;
        int HOTEL_NAME = 8;
        int HOTEL_ADDRESS = 9;
        int HOTEL_EMAIL = 10;
        int HOTEL_PHONE = 11;
        int HOTEL_WEBSITE = 12;
        int HOTEL_LOGO_URL = 13;
    }

}