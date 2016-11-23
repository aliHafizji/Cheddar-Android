package com.creativeperson.cheddar.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.Time;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.utility.Constants;

public class CheddarListService extends CheddarIntentService {

	public CheddarListService() {
		super(TAG);
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
		NetworkInfo networkInfo = mConnectivityManger.getActiveNetworkInfo();
		boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();

		if(!isConnected) {
			/**
			 * TODO:
			 * This is the part where we handle offline support. 
			 * We register a listener, when internet connection is resumed we reconnect to all websockets
			 *
			 */
			Log.d(Constants.DEBUG_TAG, "This is called again and again");
			this.mToast.show();
		} else {
			long listIds[] = intent.getLongArrayExtra(Constants.LISTS_ARCHIVE);
			String listTitle = intent.getStringExtra(Constants.CREATE_NEW_LIST);
			
			if(listIds != null) {
				archiveLists(listIds);
			} else if(listTitle != null) {
				createNewList(listTitle);
			} else {
				boolean doUpdate = intent.getBooleanExtra(Constants.LISTS_FORCE_REFRESH, false);

				if(!doUpdate) {
					long lastTime = mSharedPrefs.getLong(Constants.LISTS_LAST_UPDATE_TIME, Long.MIN_VALUE);
					if(lastTime < System.currentTimeMillis() - Constants.LIST_MAX_TIME) {
						doUpdate = true;
					}
				}

				if(doUpdate) {
					fetchLists();
				} else {
					Log.d(Constants.DEBUG_TAG, "No need to update the lists");
				}
			}
		}
	}
	
	private void createNewList(String listTitle) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		String url = Constants.LISTS_URL + "?access_token=" + accessToken;
		try {
			URL createNewListUrl = new URL(url);
			URLConnection urlConnection = createNewListUrl.openConnection();
			HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
			
			httpsConnection.setDoOutput(true);
			httpsConnection.setRequestProperty("Content-Type","application/json");
			httpsConnection.setRequestMethod("POST");
			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("title", listTitle);
			
			OutputStream outputStream = httpsConnection.getOutputStream();
			outputStream.write(jsonObj.toString().getBytes());
			outputStream.close();
			
			int responseCode = httpsConnection.getResponseCode();
			if(responseCode == HttpStatus.SC_CREATED) {
				InputStream in = httpsConnection.getInputStream();
				JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
				
				try {
					parseListAndInsertInDataBase(jsonReader);
				} finally {
					jsonReader.close();
				}
			} else if(responseCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
				broadcastRequiresCheddarPlusAccount();
			} else {
				Log.d(Constants.DEBUG_TAG, httpsConnection.getResponseMessage());
				Toast.makeText(getApplicationContext(), httpsConnection.getResponseMessage(), Toast.LENGTH_SHORT).show();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void archiveLists(long listIds[]) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		for(long listId : listIds) {
			String archiveListUrl = Constants.LISTS_URL + "/" + listId + "?access_token=" + accessToken;
			try {
				URL url = new URL(archiveListUrl);
				URLConnection urlConnection = url.openConnection();
				HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
				
				httpsConnection.setDoOutput(true);
				httpsConnection.setRequestProperty("Content-Type", "application/json");
				httpsConnection.setRequestMethod("PUT");
				
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("archived_at", new Time().toString());
				
				OutputStream outputStream = httpsConnection.getOutputStream();
				outputStream.write(jsonObj.toString().getBytes());
				outputStream.close();
				
				if(httpsConnection.getResponseCode() == HttpStatus.SC_OK) {
					InputStream in = httpsConnection.getInputStream();
					JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
					
					try {
						parseListAndInsertInDataBase(jsonReader);
					} finally {
						jsonReader.close();
					}
				} else {
					Log.d(Constants.DEBUG_TAG, httpsConnection.getResponseMessage());
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void fetchLists() {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		String fetchTasksURL = Constants.LISTS_URL + "?access_token=" + accessToken;
		
		long currentTime = System.currentTimeMillis();
		
		try {
			URL url = new URL(fetchTasksURL);
			URLConnection urlConnection = url.openConnection();
			HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
			int responseCode = httpsConnection.getResponseCode();

			if(responseCode == HttpStatus.SC_OK) {
				InputStream in = httpsConnection.getInputStream();
				JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));

				try {
					parseListsArray(jsonReader);
				} finally {
					jsonReader.close();
					mPrefsEditor.putLong(Constants.LISTS_LAST_UPDATE_TIME, currentTime);
					mPrefsEditor.commit();
				}
			} else {
				Log.d(Constants.DEBUG_TAG, httpsConnection.getResponseMessage());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parseListsArray(JsonReader reader) throws IOException {
		reader.beginArray();
		while(reader.hasNext()) {
			parseListAndInsertInDataBase(reader);
		}
		reader.endArray();
		broadcastListRefreshComplete();
	}

	private void parseListAndInsertInDataBase(JsonReader reader) throws IOException {
		reader.beginObject();
		ContentValues contentValues = new ContentValues();

		long _id = 0;
		while(reader.hasNext()) {
			String name = reader.nextName();
			if(name.equals("id")) {
				_id = reader.nextLong();
				contentValues.put(CheddarContentProvider.Lists.LIST_ID, _id);
			} else if(name.equals("position")) {
				contentValues.put(CheddarContentProvider.Lists.LIST_POSITION, reader.nextLong());
			} else if(name.equals("title")) {
				contentValues.put(CheddarContentProvider.Lists.LIST_TITLE, reader.nextString());
			} else if(name.equals("archived_at") && reader.peek() != JsonToken.NULL) {
				contentValues.put(CheddarContentProvider.Lists.ARCHIVED_AT, reader.nextString());
            } else if(name.equals(CheddarContentProvider.Lists.LIST_ACTIVE_UNCOMPLETED_TASK_COUNT)) {
                contentValues.put(CheddarContentProvider.Lists.LIST_ACTIVE_UNCOMPLETED_TASK_COUNT, reader.nextLong());
			} else if(name.equals("error") && reader.nextString().equals("plus_required")) {
				//send out a broadcast saying that you need to buy stuff
				//redirect the user here
				Log.d(Constants.DEBUG_TAG, "You need to get cheddar plus");
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		if(contentValues.size() > 0) {
			if(getContentResolver().update(CheddarContentProvider.Lists.CONTENT_URI, contentValues, "_id=?", new String[]{String.valueOf(_id)}) == 0) {
				getContentResolver().insert(CheddarContentProvider.Lists.CONTENT_URI, contentValues);
			}
		}
	}
	
	private void broadcastRequiresCheddarPlusAccount() {
		Intent i = new Intent(Constants.CHEDDAR_PLUS_ACCOUNT_NEEDED);
		sendBroadcast(i);
	}
	
	private void broadcastListRefreshComplete() {
		Intent i = new Intent(Constants.LISTS_REFRESH_COMPLETE);
		sendBroadcast(i);
	}
}
