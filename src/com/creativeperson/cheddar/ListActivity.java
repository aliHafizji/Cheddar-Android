package com.creativeperson.cheddar;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.creativeperson.cheddar.fragments.CheddarPlusDialogFragment.CheddarPlusDialogListener;
import com.creativeperson.cheddar.fragments.ListsListFragment;
import com.creativeperson.cheddar.fragments.TasksListFragment;
import com.creativeperson.cheddar.utility.Constants;

public class ListActivity extends Activity implements ListsListFragment.Callbacks, CheddarPlusDialogListener {

	private boolean mTwoPane;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lists);

		if (findViewById(R.id.listitem_detail_container) != null) {
			mTwoPane = true;
		}
	}

	@Override
	public void upgradeToCheddarPlus() {
		Intent cheddarPlusIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CHEDDAR_PLUS_URL));
		startActivity(cheddarPlusIntent);
	}
	
	public void onItemSelected(long id) {
		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putLong(TasksListFragment.LIST_ID, id);
			TasksListFragment fragment = new TasksListFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
			.replace(R.id.listitem_detail_container, fragment)
			.commit();
		} else {
			Intent detailIntent = new Intent(this, TasksActivity.class);
			detailIntent.putExtra(TasksListFragment.LIST_ID, id);
			startActivity(detailIntent);
		}
	}
}
