package com.creativeperson.cheddar.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.creativeperson.cheddar.R;

public abstract class CheddarListFragment extends ListFragment {

	protected EditText mEditText;
	protected CursorAdapter mAdapter;
	protected BroadcastReceiver mReceiver;
	
	protected abstract void archiveButtonPressed(ActionMode mode, MenuItem item);
	protected abstract void contextualActionBarShow();
	protected abstract void contextualActionBarRemoved();
	protected abstract void keyboardDoneButtonPressed();
	
	protected void setEditorActionListener() {
		if(mEditText != null) {
			mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if(actionId == EditorInfo.IME_ACTION_DONE) {
						InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
						keyboardDoneButtonPressed();
						return true;
					}
					return false;
				}
			});
		}
	}
	
	protected class ModeCallback implements ListView.MultiChoiceModeListener {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            mode.setTitle("Select Items");
            setSubtitle(mode);
            contextualActionBarShow();
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.archive:
            	archiveButtonPressed(mode, item);
            	mode.setTitle(null);
                break;
            }
            return true;
        } 

        public void onDestroyActionMode(ActionMode mode) {
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            setSubtitle(mode);
        }

        protected void setSubtitle(ActionMode mode) {
            final int checkedCount = getListView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setTitle(null);
                    contextualActionBarRemoved();
                    break;
                case 1:
                    mode.setTitle("One item selected");
                    break;
                default:
                    mode.setTitle("" + checkedCount + " items selected");
                    break;
            }
        }
    }
}
