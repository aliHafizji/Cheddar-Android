package com.creativeperson.cheddar.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.widget.Toast;

import com.creativeperson.cheddar.R;
import com.creativeperson.cheddar.utility.Constants;

public class CheddarIntentService extends IntentService {

	protected static String TAG = Constants.DEBUG_TAG;
	protected ConnectivityManager mConnectivityManger;
	protected SharedPreferences mSharedPrefs;
	protected Editor mPrefsEditor;
	protected Toast mToast;
	
	public CheddarIntentService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.mToast = Toast.makeText(this, getResources().getString(R.string.no_offline_support), Toast.LENGTH_SHORT);
		mConnectivityManger = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mSharedPrefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
		mPrefsEditor = mSharedPrefs.edit();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
	}
}
