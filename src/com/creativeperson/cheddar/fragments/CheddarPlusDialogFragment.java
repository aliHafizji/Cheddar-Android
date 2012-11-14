package com.creativeperson.cheddar.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.creativeperson.cheddar.R;

public class CheddarPlusDialogFragment extends DialogFragment {

	public interface CheddarPlusDialogListener {
		public void upgradeToCheddarPlus();
	}
	
	private CheddarPlusDialogListener mListener;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setPositiveButton(getResources().getString(R.string.upgrade), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(mListener != null) {
					mListener.upgradeToCheddarPlus();
				}
			}
		});
		
		builder.setNegativeButton(getResources().getString(R.string.upgrade_later), null);
		
		LinearLayout parentLayout = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.custom_alert_dialog_content_view, null);
		
		TextView alertTitle = (TextView) parentLayout.findViewById(R.id.customAlertTitle);
		TextView message = (TextView) parentLayout.findViewById(R.id.message);
		ImageView icon = (ImageView) parentLayout.findViewById(R.id.icon);
		
		alertTitle.setText(getResources().getString(R.string.upgrade));
		message.setText(getResources().getString(R.string.upgrade_dialog_message));
		icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_dialog_alert_holo_light));
		
		builder.setView(parentLayout);
		return builder.create();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(!(activity instanceof CheddarPlusDialogListener)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}
		mListener = (CheddarPlusDialogListener)activity;
	}
}
