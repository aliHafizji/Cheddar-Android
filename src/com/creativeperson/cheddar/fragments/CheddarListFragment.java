package com.creativeperson.cheddar.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.creativeperson.cheddar.R;

public abstract class CheddarListFragment extends ListFragment {

    protected MenuItem mRefreshMenuItem;

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

    protected void startRefreshAnimation() {
        if (getActivity() != null) {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

            Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh_anim);
            rotation.setRepeatCount(Animation.INFINITE);
            iv.startAnimation(rotation);

            mRefreshMenuItem.setActionView(iv);
        }
    }

    protected void stopRefreshAnimation() {
        if(mRefreshMenuItem != null && mRefreshMenuItem.getActionView() != null) {
            mRefreshMenuItem.getActionView().clearAnimation();
            mRefreshMenuItem.setActionView(null);
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
