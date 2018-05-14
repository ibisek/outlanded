package com.ibisek.outlanded.components;

import android.view.View;

public abstract class LayoutableDialogFragmentController {

	protected LayoutableDialogFragment dialog;
	
	/**
	 * @param dialog
	 */
	public void init(LayoutableDialogFragment dialog) {
		this.dialog = dialog;
	}

	/**
	 * @param view
	 */
	public abstract void onCreateView(View view);

	
}
