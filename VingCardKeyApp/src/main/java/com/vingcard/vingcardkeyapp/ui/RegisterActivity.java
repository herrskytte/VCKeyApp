package com.vingcard.vingcardkeyapp.ui;

import android.support.v4.app.Fragment;

import com.vingcard.vingcardkeyapp.standard.SimpleSinglePaneActivity;

public class RegisterActivity extends SimpleSinglePaneActivity {

	@Override
	protected Fragment onCreatePane() {
		return new RegisterFragment();
	}

}
