package com.creativeperson.cheddar.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.creativeperson.cheddar.LoginActivity;
import com.creativeperson.cheddar.R;
import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.services.CheddarListService;
import com.creativeperson.cheddar.utility.Constants;

public class ListsListFragment extends CheddarListFragment implements LoaderCallbacks<Cursor>{

	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	private SharedPreferences mSharedPreferences;
	
	private static String[] LIST_PROJECTION = new String[] {
		CheddarContentProvider.Lists.LIST_ID,
		CheddarContentProvider.Lists.LIST_TITLE,
        CheddarContentProvider.Lists.LIST_ACTIVE_UNCOMPLETED_TASK_COUNT
	};

	private Callbacks mCallbacks = sDummyCallbacks;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	public interface Callbacks {
		public void onItemSelected(long id);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		public void onItemSelected(long id) {

		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_pull_to_refresh_with_edit_text, container);

        mEditText = (EditText)view.findViewById(R.id.edit_text);
        setEditorActionListener();

        return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mSharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
		setHasOptionsMenu(true);

		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(Constants.LISTS_REFRESH_COMPLETE)) {
                    // TODO
					//mPullToRefreshListView.onRefreshComplete();
				} else if(intent.getAction().equals(Constants.CHEDDAR_PLUS_ACCOUNT_NEEDED)) {
					createCheddarPlusDialog();
				}
			}
		};
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.lists_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
            case R.id.logout:
                getActivity().finish();

                Editor editor = mSharedPreferences.edit();
                editor.putString(Constants.ACCESS_TOKEN, null);
                editor.commit();

                startActivity(new Intent(getActivity(), LoginActivity.class));
                return true;

            case R.id.refresh_lists_item:
                mRefreshMenuItem = item;
                updateLists();
                startRefreshAnimation();
                return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();

		updateLists();
		if(mReceiver != null) {
			IntentFilter intentFilter = new IntentFilter(Constants.LISTS_REFRESH_COMPLETE);
			intentFilter.addAction(Constants.CHEDDAR_PLUS_ACCOUNT_NEEDED);
			getActivity().registerReceiver(mReceiver, intentFilter);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if(mReceiver != null) {
			getActivity().unregisterReceiver(mReceiver);
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		forceListRefresh();
		mEditText.setHint(getResources().getString(R.string.list_hint_text));

        final String[] from = {CheddarContentProvider.Lists.LIST_TITLE, CheddarContentProvider.Lists.LIST_ACTIVE_UNCOMPLETED_TASK_COUNT};
        final int[] to = {R.id.lists_list_item_title, R.id.lists_list_item_title_uncompleted_task_count};
        mAdapter = new SimpleCursorAdapter(
                getActivity(),
				R.layout.lists_list_item,
				null,
                from,
                to,
				0);

		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		mCallbacks.onItemSelected(id);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(
                getActivity(),
				CheddarContentProvider.Lists.CONTENT_URI, 
				LIST_PROJECTION, 
				CheddarContentProvider.Lists.ARCHIVED_AT + " is NULL", 
				null, 
				CheddarContentProvider.Lists.LIST_POSITION);
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
        stopRefreshAnimation();
    }

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
        stopRefreshAnimation();
    }
	
	@Override
	protected void archiveButtonPressed(ActionMode mode, MenuItem item) {
		Intent i = new Intent(getActivity(), CheddarListService.class);
    	i.putExtra(Constants.LISTS_ARCHIVE, getListView().getCheckedItemIds());
    	getActivity().startService(i);
	}

	@Override
	protected void keyboardDoneButtonPressed() {
		if(!this.mEditText.getText().toString().trim().equals("")) {
			Intent i = new Intent(getActivity(), CheddarListService.class);
			i.putExtra(Constants.CREATE_NEW_LIST, mEditText.getText().toString());
			getActivity().startService(i);

			mEditText.setText("");
		}
	}
	
	@Override
	protected void contextualActionBarShow() {
		
	}

	@Override
	protected void contextualActionBarRemoved() {
		
	}
	
	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	private void forceListRefresh() {
		Intent intent = new Intent(getActivity(), CheddarListService.class);
		getActivity().startService(intent.putExtra(Constants.LISTS_FORCE_REFRESH, true));
	}
	
	private void updateLists() {
		Intent intent = new Intent(getActivity(), CheddarListService.class);
		getActivity().startService(intent);
	}
	
	private void createCheddarPlusDialog() {
		CheddarPlusDialogFragment cheddarPlusDialog = new CheddarPlusDialogFragment();
		cheddarPlusDialog.show(getActivity().getSupportFragmentManager(), "");
	}
}
