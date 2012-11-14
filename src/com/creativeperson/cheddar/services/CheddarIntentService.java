package com.creativeperson.cheddar.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;

import com.creativeperson.cheddar.utility.Constants;

public class CheddarIntentService extends IntentService {

	protected static String TAG = Constants.DEBUG_TAG;
	protected ConnectivityManager mConnectivityManger;
	protected SharedPreferences mSharedPrefs;
	protected Editor mPrefsEditor;
	
	public CheddarIntentService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mConnectivityManger = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mSharedPrefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
		mPrefsEditor = mSharedPrefs.edit();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
	}
}
