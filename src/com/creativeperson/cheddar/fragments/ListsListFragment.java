package com.creativeperson.cheddar.fragments;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuItem;
import com.creativeperson.cheddar.R;
import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.services.CheddarListService;
import com.creativeperson.cheddar.utility.Constants;
import com.creativeperson.cheddar.views.PullToRefreshListView;
import com.creativeperson.cheddar.views.RefreshableListView;

public class ListsListFragment extends CheddarListFragment implements LoaderCallbacks<Cursor> {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";
	private PullToRefreshListView mPullToRefresh;
	
	private static String[] LIST_PROJECTION = new String[] {
		CheddarContentProvider.Lists.LIST_ID,
		CheddarContentProvider.Lists.LIST_TITLE
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
		return inflater.inflate(R.layout.fragment_pull_to_refresh_with_edit_text, null);
	}

	
	@Override
	public void onViewCreated(View view) {
		super.onViewCreated(view);
		mPullToRefresh = (PullToRefreshListView)getListView();
		mPullToRefresh.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		
		mPullToRefresh.setOnPullToRefresh(new RefreshableListView.OnPullToRefresh() {
			
			@Override
			public void onPullToRefresh() {
				forceListRefresh();
				mPullToRefresh.setEnabled(false);
			}
		});
		
		mPullToRefresh.setMultiChoiceModeListener(new ModeCallback());
        
        mEditText = (EditText)view.findViewById(R.id.edit_text);
        setEditorActionListener();
        
		if (getSavedInstanceState() != null && getSavedInstanceState().containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(getSavedInstanceState().getInt(STATE_ACTIVATED_POSITION));
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(Constants.LISTS_REFRESH_COMPLETE)) {
					mPullToRefresh.getListHeaderView().close(0);
					mPullToRefresh.setEnabled(true);
				} else if(intent.getAction().equals(Constants.CHEDDAR_PLUS_ACCOUNT_NEEDED)) {
					createCheddarPlusDialog();
				}
			}
		};
	}
	
	@Override
	public void onResume() {
		super.onResume();

		listRefresh();
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
		
		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.list_view_cell,
				null,
				new String[]{CheddarContentProvider.Lists.LIST_TITLE},
				new int[]{R.id.text1},
				0);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
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
		return new CursorLoader(getActivity(), 
				CheddarContentProvider.Lists.CONTENT_URI, 
				LIST_PROJECTION, 
				CheddarContentProvider.Lists.ARCHIVED_AT + " is NULL", 
				null, 
				CheddarContentProvider.Lists.LIST_POSITION);
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	@Override
	protected void archiveButtonPressed(ActionMode mode, MenuItem item) {
		Intent i = new Intent(getActivity(), CheddarListService.class);
    	i.putExtra(Constants.LISTS_ARCHIVE, getListView().getCheckedItemIds());
    	getSherlockActivity().startService(i);
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
		mPullToRefresh.setPullToRefreshEnabled(false);
	}

	@Override
	protected void contextualActionBarRemoved() {
		mPullToRefresh.setPullToRefreshEnabled(true);
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
	
	private void listRefresh() {
		Intent intent = new Intent(getActivity(), CheddarListService.class);
		getActivity().startService(intent);
	}
	
	private void createCheddarPlusDialog() {
		CheddarPlusDialogFragment cheddarPlusDialog = new CheddarPlusDialogFragment();
		cheddarPlusDialog.show(getActivity().getSupportFragmentManager(), "");
	}
}
