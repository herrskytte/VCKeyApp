package com.vingcard.vingcardkeyapp.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;

import com.vingcard.vingcardkeyapp.standard.SimpleSinglePaneActivity;

public class DoorHandlerActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final DoorHandlerFragment doorDialog = (DoorHandlerFragment)Fragment.instantiate(this, 
				DoorHandlerFragment.class.getName(), SimpleSinglePaneActivity.intentToFragmentArguments(getIntent()));
		FragmentManager fm = getFragmentManager();
		doorDialog.show(fm, "fragment_handle_door");
		
		new Handler().postDelayed(new Runnable(){
	        public void run() {
	        	if(doorDialog.isVisible()){
	        		doorDialog.dismissAllowingStateLoss();	        		
	        	}
	      }}, 5000);
	}
}
