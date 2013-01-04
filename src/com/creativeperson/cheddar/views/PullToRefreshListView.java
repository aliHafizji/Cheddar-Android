package com.creativeperson.cheddar.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.creativeperson.cheddar.R;

public class PullToRefreshListView extends RefreshableListView {

	public PullToRefreshListView(final Context context, AttributeSet attrs) {
		super(context, attrs);

		setContentView(R.layout.pull_to_refresh);
		setOnHeaderViewChangedListener(new OnHeaderViewChangedListener() {

			@Override
			public void onViewChanged(View v, boolean canUpdate) {
				TextView tv = (TextView) v.findViewById(R.id.refresh_text);
				ImageView img = (ImageView) v.findViewById(R.id.refresh_icon);
				Animation anim;
				if (canUpdate) {
					anim = AnimationUtils.loadAnimation(context,
							R.anim.rotate_up);
					tv.setText(R.string.refresh_release);
				} else {
					tv.setText(R.string.refresh_pull_down);
					anim = AnimationUtils.loadAnimation(context,
							R.anim.rotate_down);
				}
				img.startAnimation(anim);
			}

			@Override
			public void onViewUpdating(View v) {
				TextView tv = (TextView) v.findViewById(R.id.refresh_text);
				ImageView img = (ImageView) v.findViewById(R.id.refresh_icon);
				ProgressBar pb = (ProgressBar) v
						.findViewById(R.id.refresh_loading);
				pb.setVisibility(View.VISIBLE);
				tv.setText(R.string.loading);
				img.clearAnimation();
				img.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onViewUpdateFinish(View v) {
				TextView tv = (TextView) v.findViewById(R.id.refresh_text);
				ImageView img = (ImageView) v.findViewById(R.id.refresh_icon);
				ProgressBar pb = (ProgressBar) v
						.findViewById(R.id.refresh_loading);

				tv.setText(R.string.refresh_pull_down);
				pb.setVisibility(View.INVISIBLE);
				tv.setVisibility(View.VISIBLE);
				img.setVisibility(View.VISIBLE);
			}
		});
	}

}
