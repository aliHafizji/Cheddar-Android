package com.creativeperson.cheddar;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.actionbarsherlock.view.MenuItem;
import com.creativeperson.cheddar.data.CheddarContentProvider;
import com.creativeperson.cheddar.fragments.TasksListFragment;

public class TasksActivity extends Activity implements LoaderCallbacks<Cursor> {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_taskslist);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putLong(TasksListFragment.LIST_ID, getIntent().getLongExtra(TasksListFragment.LIST_ID, -1));

			TasksListFragment fragment = new TasksListFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
			.add(R.id.listitem_detail_container, fragment)
			.commit();
			getSupportLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			NavUtils.navigateUpTo(this, new Intent(this, ListActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		long listId = getIntent().getLongExtra(TasksListFragment.LIST_ID, -1);
		if(listId != -1) {
			return new CursorLoader(this, 
					CheddarContentProvider.Lists.CONTENT_URI, 
					new String[]{CheddarContentProvider.Lists.LIST_TITLE}, 
					CheddarContentProvider.Lists.LIST_ID + "=?", 
					new String[]{String.valueOf(listId)}, 
					null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if(cursor != null) {
			cursor.moveToFirst();
			String title = cursor.getString(0);
			if(title != null)
				setTitle(title);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		setTitle("");
	}
}