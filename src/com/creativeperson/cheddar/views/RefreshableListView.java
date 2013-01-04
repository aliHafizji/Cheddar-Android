package com.creativeperson.cheddar.views;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

public class RefreshableListView extends ListView {

	private static final int STATE_NORMAL = 0;
	private static final int STATE_READY = 1;
	private static final int STATE_PULL = 2;
	private static final int STATE_UPDATING = 3;
	private static final int INVALID_POINTER_ID = -1;
	
	private int mActivePointerId;
	private int mState;
	private int mTouchSlop;
	private float mLastY;
	private boolean mEnablePullToRefresh;
	private OnPullToRefresh mOnPullToRefresh;
	
	protected ListHeaderView mListHeaderView;
	
	public RefreshableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public RefreshableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	/***
	 * Set the Header Content View.
	 * 
	 * @param id
	 *            The view resource.
	 */
	public void setContentView(int id) {
		final View view = LayoutInflater.from(getContext()).inflate(id,
				mListHeaderView, false);
		mListHeaderView.addView(view);
	}

	public void setContentView(View v) {
		mListHeaderView.addView(v);
	}

	public ListHeaderView getListHeaderView() {
		return mListHeaderView;
	}

	public boolean isPullToRefreshEnabled() {
		return mEnablePullToRefresh;
	}

	public void setPullToRefreshEnabled(boolean enablePullToRefresh) {
		this.mEnablePullToRefresh = enablePullToRefresh;
	}

	public void setOnPullToRefresh(OnPullToRefresh onPullToRefresh) {
		this.mOnPullToRefresh = onPullToRefresh;
	}

	/**
	 * Update immediately.
	 */
	public void startUpdateImmediate() {
		if (mState == STATE_UPDATING) {
			return;
		}
		setSelectionFromTop(0, 0);
		mListHeaderView.moveToUpdateHeight();
		update();
	}

	/**
	 * Set the Header View change listener.
	 * 
	 * @param listener
	 */
	public void setOnHeaderViewChangedListener(
			OnHeaderViewChangedListener listener) {
		mListHeaderView.mOnHeaderViewChangedListener = listener;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mState == STATE_UPDATING) {
			return super.dispatchTouchEvent(ev);
		}
		
		if(!mEnablePullToRefresh) {
			return super.dispatchTouchEvent(ev);
		}
		
		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			mLastY = ev.getY();
			isFirstViewTop();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mActivePointerId == INVALID_POINTER_ID) {
				break;
			}

			if (mState == STATE_NORMAL) {
				isFirstViewTop();
			}

			if (mState == STATE_READY) {
				final int activePointerId = mActivePointerId;
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(ev, activePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				final int deltaY = (int) (y - mLastY);
				mLastY = y;
				if (deltaY <= 0 || Math.abs(y) < mTouchSlop) {
					mState = STATE_NORMAL;
				} else {
					mState = STATE_PULL;
					ev.setAction(MotionEvent.ACTION_CANCEL);
					super.dispatchTouchEvent(ev);
				}
			}

			if (mState == STATE_PULL) {
				final int activePointerId = mActivePointerId;
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(ev, activePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				final int deltaY = (int) (y - mLastY);
				mLastY = y;

				final int headerHeight = mListHeaderView.getHeight();
				setHeaderHeight(headerHeight + deltaY * 5 / 9);
				return true;
			}

			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mActivePointerId = INVALID_POINTER_ID;
			if (mState == STATE_PULL) {
				update();
			}
			break;
		case MotionEventCompat.ACTION_POINTER_DOWN:
			final int index = MotionEventCompat.getActionIndex(ev);
			final float y = MotionEventCompat.getY(ev, index);
			mLastY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastY = MotionEventCompat.getY(ev, newPointerIndex);
			mActivePointerId = MotionEventCompat.getPointerId(ev,
					newPointerIndex);
		}
	}

	void setState(int state) {
		mState = state;
	}

	private void setHeaderHeight(int height) {
		mListHeaderView.setHeaderHeight(height);
	}

	private boolean isFirstViewTop() {
		final int count = getChildCount();
		if (count == 0) {
			return true;
		}
		final int firstVisiblePosition = this.getFirstVisiblePosition();
		final View firstChildView = getChildAt(0);
		boolean needs = firstChildView.getTop() == 0
				&& (firstVisiblePosition == 0);
		if (needs) {
			mState = STATE_READY;
		}

		return needs;
	}

	private void initialize() {
		final Context context = getContext();
		mListHeaderView = new ListHeaderView(context, this);
		addHeaderView(mListHeaderView, null, false);
		mState = STATE_NORMAL;
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
		mEnablePullToRefresh = true;
	}

	private void update() {
		if (mListHeaderView.isUpdateNeeded()) {
			mListHeaderView.startUpdate();
			mState = STATE_UPDATING;
			if(mOnPullToRefresh != null) {
				mOnPullToRefresh.onPullToRefresh();
			}
		} else {
			mListHeaderView.close(STATE_NORMAL);
		}
	}
	
	/** When use custom List header view */
	public static interface OnHeaderViewChangedListener {
		/**
		 * When user pull the list view, we can change the header status here.
		 * for example: the arrow rotate down or up.
		 * 
		 * @param v
		 *            : the list view header
		 * @param canUpdate
		 *            : if the list view can update.
		 */
		void onViewChanged(View v, boolean canUpdate);

		/**
		 * Change the header status when we really do the update task. for
		 * example: display the progressbar.
		 * 
		 * @param v
		 *            the list view header
		 */
		void onViewUpdating(View v);

		/**
		 * Will called when the update task finished. for example: hide the
		 * progressbar and show the arrow.
		 * 
		 * @param v
		 *            the list view header.
		 */
		void onViewUpdateFinish(View v);
	}

	public static interface OnPullToRefresh {
		
		/**
		 * Will get called when a pull is detected
		 */
		public void onPullToRefresh();
	}
}
