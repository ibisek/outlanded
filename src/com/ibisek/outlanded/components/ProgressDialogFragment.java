package com.ibisek.outlanded.components;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {

	private String message;

	/**
	 * @param message
	 * @return instance 
	 */
	public static ProgressDialogFragment newInstance(String message) {
		ProgressDialogFragment instance = new ProgressDialogFragment();
		instance.message = message;

		return instance;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(message);
		dialog.setIndeterminate(true);
		return dialog;
	}
}
