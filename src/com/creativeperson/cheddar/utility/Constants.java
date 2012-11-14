package com.creativeperson.cheddar.utility;

import android.app.AlarmManager;

public class Constants {
	public static final String PREFS_NAME = "Cheddar prefs";
	public static final String DEBUG_TAG = "Cheddar";
	public static final String AUTHORIZATION_URL = "https://api.cheddarapp.com/oauth/authorize";
	public static final String ACCESS_TOKEN_URL = "https://api.cheddarapp.com/oauth/token";
	public static final String CLIENT_ID = "61fbad9e125aa79089ed29a1420a01db";
	public static final String CLIENT_SECRET = "be769bb594c5e29dd716280721675f7c";
	public static final String REDIRECT_URI = "cheddar://accessresponse";
	public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	
	public static final String CHEDDAR_PLUS_URL = "https://cheddarapp.com/account#plus";
	
	/**
	 * Intent details to fetch all the lists from the remote server.
	 */
	public static final String LISTS_FORCE_REFRESH = "force_refresh";
	public static final String LISTS_LAST_UPDATE_TIME = "lists_last_update_time";
	
	/**
	 * Intent details required to archive list items
	 */
	public static final String LISTS_ARCHIVE = "lists_archive";
	public static final String CREATE_NEW_LIST = "new_list";
	
	public static final long LIST_MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
	
	public static final String LISTS_URL = "https://api.cheddarapp.com/v1/lists";
	public static final String TASKS_URL = "https://api.cheddarapp.com/v1/tasks";
	
	public static final String TASKS_ARCHIVE = "tasks_archive";
	public static final String CREATE_NEW_TASK = "new_task";
	
	public static final String LISTS_REFRESH_COMPLETE = "com.creativeperson.cheddar.lists_refresh_complete";
	public static final String TASKS_REFRESH_COMPLETE = "com.creativeperson.cheddar.tasks_refresh_complete";
	
	public static final String TASKS_REORDER = "reorder_tasks";
	
	public static final String CHEDDAR_PLUS_ACCOUNT_NEEDED = "com.creativeperson.cheddar.cheddar_plus_account_needed";
	
	//pusher list event names
	public static final String LIST_CREATE = "list-create";
	public static final String LIST_UPDATE = "list-update";
	public static final String LIST_REORDER = "list-reorder";
}
