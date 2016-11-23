package com.creativeperson.cheddar.fragments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.XMLReader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.format.Time;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.creativeperson.cheddar.R;
import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.services.CheddarTasksService;
import com.creativeperson.cheddar.utility.Constants;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter.ViewBinder;
import com.mobeta.android.dslv.SimpleFloatViewManager;

public class TasksListFragment extends CheddarListFragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

	public static final String LIST_ID = "list_id";

	private DragSortListView mDragSortListView;

	private static int TASK_TEXT_COLUMN = 1;
	private static int TASK_COMPLETED_COLUMN = 2;
	
	private static String[] TASKS_PROJECTION = new String[] {
		CheddarContentProvider.Tasks.TASK_ID,
		CheddarContentProvider.Tasks.DISPLAY_TASK_HTML,
		CheddarContentProvider.Tasks.COMPLETED_AT
	};

	private void updateTasks(long listId) {
		Intent i = new Intent(getActivity(), CheddarTasksService.class);
		i.putExtra(LIST_ID, listId);
		getActivity().startService(i);
	}

	public class TaskTagHandler implements TagHandler {

		public void handleTag(boolean opening, String tag, Editable output,
	            XMLReader xmlReader) {
	        if(tag.equalsIgnoreCase("strike") || tag.equals("s") || tag.equals("del")) {
	            processStrike(opening, output);
	        }
	    }

	    private void processStrike(boolean opening, Editable output) {
	        int len = output.length();
	        if(opening) {
	            output.setSpan(new StrikethroughSpan(), len, len, Spannable.SPAN_MARK_MARK);
	        } else {
	            Object obj = getLast(output, StrikethroughSpan.class);
	            int where = output.getSpanStart(obj);
	            output.removeSpan(obj);
	            if (where != len) {
	                output.setSpan(new StrikethroughSpan(), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            }
	        }
	    }

	    private Object getLast(Editable text, Class<?> kind) {
	        Object[] objs = text.getSpans(0, text.length(), kind);
	        if (objs.length == 0) {
	            return null;
	        } else {
	            for(int i = objs.length;i>0;i--) {
	                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
	                    return objs[i-1];
	                }
	            }
	            return null;
	        }
	    }
	}
	
	private class TaskItemBinder implements ViewBinder {

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

			if(columnIndex == TASK_TEXT_COLUMN) {
				((TextView)view).setText(Html.fromHtml(cursor.getString(columnIndex), null, new TaskTagHandler()));
			}
			if(columnIndex == TASK_COMPLETED_COLUMN) {
				CheckBox checkbox = (CheckBox)view;
				checkbox.setTag(cursor.getLong(0));
				checkbox.setOnCheckedChangeListener(null);
				
				RelativeLayout relativeLayout = (RelativeLayout)checkbox.getParent();
				
				if(cursor.getString(columnIndex) != null) {
					checkbox.setChecked(true);
					relativeLayout.setAlpha(0.5f);
				} else {
					checkbox.setChecked(false);
					relativeLayout.setAlpha(1f);
				}
				
				checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						Log.d(Constants.DEBUG_TAG, "Task tag:" + buttonView.getTag() + " isChecked:" + isChecked);

						Intent i = new Intent(getActivity(), CheddarTasksService.class);
						i.putExtra(Constants.COMPLETE_TASK, (Long)buttonView.getTag());
						
						RelativeLayout parent = (RelativeLayout) buttonView.getParent();
						if(isChecked) {
							parent.setAlpha(0.5f);
							i.putExtra(Constants.COMPLETE_TASK_UPDATE_VALUE, new Time().toString());
						} else {
							parent.setAlpha(1f);
						}
						
						getActivity().startService(i);
					}
				});
			}
			return true;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_drag_and_drop_list_with_edit_text, null);

		mDragSortListView = (DragSortListView) view.findViewById(android.R.id.list);
		mDragSortListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mDragSortListView.setMultiChoiceModeListener(new ModeCallback());
		SimpleFloatViewManager simpleFloatViewManager = new SimpleFloatViewManager(mDragSortListView);
		simpleFloatViewManager.setBackgroundColor(getResources().getColor(R.color.cheddar_orange));
		mDragSortListView.setFloatViewManager(simpleFloatViewManager);

		mEditText = (EditText)view.findViewById(R.id.edit_text);
		setEditorActionListener();
		
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(LIST_ID)) {
			updateTasks(getArguments().getLong(LIST_ID));
		}

		setHasOptionsMenu(true);
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(Constants.TASKS_REFRESH_COMPLETE)) {
					stopRefreshAnimation();
				}
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		if(mReceiver != null) {
			IntentFilter intentFilter = new IntentFilter(Constants.TASKS_REFRESH_COMPLETE);
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
		mEditText.setHint(getResources().getString(R.string.task_hint_text));

		mAdapter = new SimpleDragSortCursorAdapter(getActivity(),
				R.layout.task_list_item,
				null,
				new String[]{CheddarContentProvider.Tasks.COMPLETED_AT, CheddarContentProvider.Tasks.DISPLAY_TASK_HTML},
				new int[]{R.id.task_checkbox, R.id.task_text},
				0)
		{
			@Override
			public void drop(int from, int to) {
				super.drop(from, to);
				Log.d(Constants.DEBUG_TAG, "List:" + getCursorPositions());
				new ReorderTasksLocally().execute();
			}
		}; 

		((SimpleDragSortCursorAdapter)mAdapter).setViewBinder(new TaskItemBinder());
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.tasks_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.refresh_tasks:
			mRefreshMenuItem = item;
			updateTasks(getArguments().getLong(LIST_ID));
			startRefreshAnimation();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), 
				CheddarContentProvider.Tasks.CONTENT_URI, 
				TASKS_PROJECTION, 
				CheddarContentProvider.Tasks.ARCHIVED_AT + " is NULL and " + CheddarContentProvider.Tasks.LIST_ID + "=?", 
				new String[]{String.valueOf(getArguments().getLong(LIST_ID))}, 
				CheddarContentProvider.Tasks.TASK_POSITION);
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
		Intent i = new Intent(getActivity(), CheddarTasksService.class);
		i.putExtra(Constants.TASKS_ARCHIVE, getListView().getCheckedItemIds());
		getActivity().startService(i);
	}

	@Override
	protected void keyboardDoneButtonPressed() {
		if(!this.mEditText.getText().toString().trim().equals("")) {
			Intent i = new Intent(getActivity(), CheddarTasksService.class);
			i.putExtra(LIST_ID, getArguments().getLong(LIST_ID));
			i.putExtra(Constants.CREATE_NEW_TASK, mEditText.getText().toString());
			getActivity().startService(i);

			mEditText.setText("");
		}
	}

	@Override
	protected void contextualActionBarShow() {
		mDragSortListView.setDragEnabled(false);
	}

	@Override
	protected void contextualActionBarRemoved() {
		mDragSortListView.setDragEnabled(true);
	}
	
	private class ReorderTasksLocally extends AsyncTask<Void, Void, Collection<Long>> {

		@Override
		protected Collection<Long> doInBackground(Void... params) {
			Cursor cursor = mAdapter.getCursor();
			Log.d(Constants.DEBUG_TAG,"Cursor count:" + cursor.getCount());

			cursor.moveToFirst();
			ArrayList<Long> taskIds = new ArrayList<Long>();

			while ( ! cursor.isAfterLast())  {
				taskIds.add(cursor.getLong(0));
				cursor.moveToNext();
			}

			ArrayList<Integer> currentPositions = ((SimpleDragSortCursorAdapter)mAdapter).getCursorPositions();

			if(currentPositions.size() != taskIds.size())  return null; //since the adapter lazy loads this happens somtimes
			SortedMap<Integer, Long> sortedTasks = new TreeMap<Integer, Long>();

			for(int index = 0; index < currentPositions.size(); index++) {
				sortedTasks.put(currentPositions.indexOf(index), taskIds.get(index));
			}

            return sortedTasks.values();
		}

		@Override
		protected void onPostExecute(Collection<Long> result) {
			super.onPostExecute(result);
			if(result != null) {
				Intent i = new Intent(getActivity(), CheddarTasksService.class);
				i.putExtra(Constants.TASKS_REORDER, new ArrayList<Long>(result));
				i.putExtra(LIST_ID, getArguments().getLong(LIST_ID));
				getActivity().startService(i);
			}
		}
	}
}
