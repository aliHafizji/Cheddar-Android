package com.creativeperson.cheddar.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.text.format.Time;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.Toast;

import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.fragments.TasksListFragment;
import com.creativeperson.cheddar.receivers.ConnectivityChangeReceiver;
import com.creativeperson.cheddar.utility.Constants;

public class CheddarTasksService extends CheddarIntentService {

	public CheddarTasksService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		NetworkInfo networkInfo = mConnectivityManger.getActiveNetworkInfo();
		boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();

		if(!isConnected) {
			PackageManager pm = getPackageManager();

			ComponentName connectivityReceiver = new ComponentName(this, ConnectivityChangeReceiver.class);

			pm.setComponentEnabledSetting(connectivityReceiver, 
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
					PackageManager.DONT_KILL_APP);
		} else {
			long listId = intent.getLongExtra(TasksListFragment.LIST_ID, -1);
			String task = intent.getStringExtra(Constants.CREATE_NEW_TASK);
			long archiveTaskIds[] = intent.getLongArrayExtra(Constants.TASKS_ARCHIVE);
			long completeTaskId = intent.getLongExtra(Constants.COMPLETE_TASK, -1);
			ArrayList<Long> reorderedTaskIDs = (ArrayList<Long>)intent.getSerializableExtra(Constants.TASKS_REORDER);
			
			if(archiveTaskIds != null) {
				updateTasks(archiveTaskIds, "archived_at", new Time().toString());
			} else if(completeTaskId != -1) {
				updateTasks(new long[] {completeTaskId}, "completed_at", intent.getStringExtra(Constants.COMPLETE_TASK_UPDATE_VALUE));
			} else if(listId != -1 && task != null) {
				createTask(listId, task);
			} else if(listId != -1 && reorderedTaskIDs != null) {
				reorderTasks(listId, reorderedTaskIDs);
			} else if (listId != -1) {
				fetchTasks(listId);
			}
		}
	}

	private void reorderTasks(long listId, ArrayList<Long> reorderedTaskIDs) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		String reorderUrl = Constants.LISTS_URL + "/" + listId + "/tasks/reorder?access_token=" + accessToken;
		try {
			URL url = new URL(reorderUrl);
			URLConnection urlConnection = url.openConnection();
			HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
			
			httpsConnection.setDoOutput(true);
			httpsConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpsConnection.setRequestMethod("POST");
			
			String tasks = new String();
			for(int i = 0; i < reorderedTaskIDs.size(); i++) {
				Long taskId = reorderedTaskIDs.get(i);
				tasks += "task[]=" + taskId;
				if(i < reorderedTaskIDs.size() - 1) {
					tasks += "&";
				}
			}
			
			OutputStream outputStream = httpsConnection.getOutputStream();
			outputStream.write(tasks.toString().getBytes());
			outputStream.close();
			
			if(httpsConnection.getResponseCode() != HttpStatus.SC_NO_CONTENT) {
				Log.d(Constants.DEBUG_TAG, httpsConnection.getResponseMessage());
				Toast.makeText(getApplicationContext(), httpsConnection.getResponseMessage(), Toast.LENGTH_SHORT).show();
			} else {
				fetchTasks(listId);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateTasks(long[] tasks, String key, Object value) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		for(long taskId : tasks) {
			String updateTasksURL = Constants.TASKS_URL + "/" + taskId + "?access_token=" + accessToken;
			
			try {
				URL url = new URL(updateTasksURL);
				URLConnection urlConnection = url.openConnection();
				HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
				
				httpsConnection.setDoOutput(true);
				httpsConnection.setRequestProperty("Content-Type", "application/json");
				httpsConnection.setRequestMethod("PUT");
				
				JSONObject jsonObj = new JSONObject();
				if(value == null) {
					jsonObj.put(key, "null");
				} else {
					jsonObj.put(key, value);
				}
				
				OutputStream outputStream = httpsConnection.getOutputStream();
				outputStream.write(jsonObj.toString().getBytes());
				outputStream.close();
				
				if(httpsConnection.getResponseCode() == HttpStatus.SC_OK) {
					InputStream in = httpsConnection.getInputStream();
					JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
					
					try {
						parseTaskAndInsertIntoDatabase(jsonReader);
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
	
	private void createTask(long listId, String task) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		String createTaskUrl = Constants.LISTS_URL + "/" + listId + "/tasks" + "?access_token=" + accessToken;
		
		try {
			URL url = new URL(createTaskUrl);
			URLConnection urlConnection = url.openConnection();
			HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
			
			httpsConnection.setDoOutput(true);
			httpsConnection.setRequestProperty("Content-Type", "application/json");
			httpsConnection.setRequestMethod("POST");
			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("text", task);
			
			OutputStream outputStream = httpsConnection.getOutputStream();
			outputStream.write(jsonObj.toString().getBytes());
			outputStream.close();
			
			if(httpsConnection.getResponseCode() == HttpStatus.SC_CREATED) {
				InputStream in = httpsConnection.getInputStream();
				JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
				
				try {
					parseTaskAndInsertIntoDatabase(jsonReader);
				} finally {
					jsonReader.close();
				}
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
	
	private void fetchTasks(long listId) {
		String accessToken = mSharedPrefs.getString(Constants.ACCESS_TOKEN, null);
		String fetchTasksURL = Constants.LISTS_URL + "/" + listId + "/tasks" + "?access_token=" + accessToken;

		try {
			URL url = new URL(fetchTasksURL);
			URLConnection urlConnection = url.openConnection();
			HttpsURLConnection httpsConnection = (HttpsURLConnection)urlConnection;
			int responseCode = httpsConnection.getResponseCode();

			if(responseCode == HttpStatus.SC_OK) {
				InputStream in = httpsConnection.getInputStream();
				JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));

				try {
					parseTasksArray(jsonReader);
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
		}
	}

	private void parseTasksArray(JsonReader jsonReader) throws IOException {
		jsonReader.beginArray();
		while(jsonReader.hasNext()) {
			parseTaskAndInsertIntoDatabase(jsonReader);
		}
		jsonReader.endArray();
		broadcastTasksRefreshComplete();
	}

	private void parseTaskAndInsertIntoDatabase(JsonReader jsonReader) throws IOException {
		jsonReader.beginObject();
		
		ContentValues contentValues = new ContentValues();
		long _id = 0;
		
		while(jsonReader.hasNext()) {
			String name = jsonReader.nextName();
			if(name.equals("id")) {
				_id = jsonReader.nextLong();
				contentValues.put(CheddarContentProvider.Tasks.TASK_ID, _id);
			} else if(name.equals("list_id")) {
				contentValues.put(CheddarContentProvider.Tasks.LIST_ID, jsonReader.nextLong());
			} else if(name.equals("position")) {
				contentValues.put(CheddarContentProvider.Tasks.TASK_POSITION, jsonReader.nextLong());
			} else if(name.equals("archived_at") && jsonReader.peek() != JsonToken.NULL) {
				contentValues.put(CheddarContentProvider.Tasks.ARCHIVED_AT, jsonReader.nextString());
			} else if(name.equals("completed_at")) {
				if(jsonReader.peek() != JsonToken.NULL) {
					contentValues.put(CheddarContentProvider.Tasks.COMPLETED_AT, jsonReader.nextString());
				} else {
					contentValues.putNull(CheddarContentProvider.Tasks.COMPLETED_AT);
					jsonReader.skipValue();
				}
			} else if(name.equals("text")) {
				contentValues.put(CheddarContentProvider.Tasks.TASK_TEXT, jsonReader.nextString());
			} else if(name.equals("display_text")) {
				contentValues.put(CheddarContentProvider.Tasks.DISPLAY_TASK_TEXT, jsonReader.nextString());
			} else if(name.equals("display_html")) {
				contentValues.put(CheddarContentProvider.Tasks.DISPLAY_TASK_HTML, jsonReader.nextString());
			} else {
				jsonReader.skipValue();
			}
		}
		jsonReader.endObject();
		
		if(contentValues.size() > 0) {
			if(getContentResolver().update(CheddarContentProvider.Tasks.CONTENT_URI, contentValues, "_id=?", new String[]{String.valueOf(_id)}) == 0) {
				getContentResolver().insert(CheddarContentProvider.Tasks.CONTENT_URI, contentValues);
			}
		}
	}
	
	private void broadcastTasksRefreshComplete() {
		Intent i = new Intent(Constants.TASKS_REFRESH_COMPLETE);
		sendBroadcast(i);
	}
}
