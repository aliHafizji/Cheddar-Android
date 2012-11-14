package com.creativeperson.cheddar.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

		if(isConnected) {
			PackageManager pm = context.getPackageManager();

			ComponentName connectivityReceiver = new ComponentName(context, ConnectivityManager.class);

			pm.setComponentEnabledSetting(connectivityReceiver, 
					PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 
					PackageManager.DONT_KILL_APP);
			
			//TODO:commit any offline tasks created
		}
	}
}
