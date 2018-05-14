package com.ibisek.outlanded.components;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class AlertDialogFragment extends DialogFragment {

	private String title, message, positiveLabel, negativeLabel;
	private DialogInterface.OnClickListener onClickListener;

	/**
	 * @param title
	 *          - if null, no title will appear
	 * @param message
	 * @param positiveLabel
	 * @param negativeLabel
	 *          - if null, there will be just one DialogInterface.BUTTON_NEUTRAL
	 * @param onClickListener
	 * @return
	 */
	public static AlertDialogFragment newInstance(String title, String message, String positiveLabel, String negativeLabel,
			DialogInterface.OnClickListener onClickListener) {
		AlertDialogFragment instance = new AlertDialogFragment();
		instance.title = title;
		instance.message = message;
		instance.positiveLabel = positiveLabel;
		instance.negativeLabel = negativeLabel;
		instance.onClickListener = onClickListener;

		return instance;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (positiveLabel != null && negativeLabel != null) {

			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity()).setMessage(message).setCancelable(false)
					.setPositiveButton(positiveLabel, new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog, int id) {
							onClickListener.onClick(dialog, id);
						}
					}).setNegativeButton(negativeLabel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							onClickListener.onClick(dialog, id);
						}
					});

			if (title != null)
				alertBuilder.setTitle(title);

			return alertBuilder.create();

		} else { // there will be just one button

			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity()).setMessage(message).setCancelable(false)
					.setNeutralButton(positiveLabel, new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog, int id) {
							onClickListener.onClick(dialog, id);
						}
					});

			if (title != null)
				alertBuilder.setTitle(title);

			return alertBuilder.create();
		}
	}
}
