package com.vingcard.vingcardkeyapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.vingcard.vingcardkeyapp.standard.SimpleSinglePaneActivity;
import com.vingcard.vingcardkeyapp.util.PreferencesUtil;

public class MainActivity extends SimpleSinglePaneActivity {
	
	@Override
	protected Fragment onCreatePane() {
		//Check if already registered
		if(PreferencesUtil.getUserId(this) == 0){
			return new WelcomeFragment();			
		}
		
		//If registered, jump straight to main screen
		else{
			return new CardsOverviewFragment();
		}
	}
	
	@Override
    public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(getFragment() instanceof CardsOverviewFragment){
			((CardsOverviewFragment)getFragment()).handleNewIntent(intent);
		}
    }
}
