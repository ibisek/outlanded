package com.ibisek.outlanded.components;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Layout-wise configurable dialog fragment.
 * 
 * To display, call: LayoutableDialogFragment dialog =
 * LayoutableDialogFragment.create(title, layout, false, controller);
 * dialog.show(getSupportFragmentManager(), "myDialog");
 * 
 * @author ibisek
 * @version 2013-10-30
 */
public class LayoutableDialogFragment extends DialogFragment {

	private String title;
	private int layout;
	private boolean cancelable;
	private LayoutableDialogFragmentController controller;

	public LayoutableDialogFragment() {
		// nix
	}

	/**
	 * @param title
	 * @param layout
	 * @param cancelable
	 * @param controller
	 * @return initialised dialog
	 */
	public static LayoutableDialogFragment create(String title, int layout, boolean cancelable, LayoutableDialogFragmentController controller) {

		LayoutableDialogFragment dialog = new LayoutableDialogFragment();

		dialog.title = title;
		dialog.layout = layout;
		dialog.cancelable = cancelable;
		dialog.controller = controller;

		controller.init(dialog);

		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(layout, container);

		setCancelable(cancelable);
		getDialog().setTitle(title);

		controller.onCreateView(view);

		return view;
	}

}
