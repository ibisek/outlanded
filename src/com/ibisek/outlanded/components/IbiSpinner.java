package com.ibisek.outlanded.components;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class IbiSpinner extends Spinner {

	final int SPINNER_LAYOUT_A = android.R.layout.simple_spinner_dropdown_item;
	final int SPINNER_LAYOUT_B = android.R.layout.simple_list_item_1;
	final int SPINNER_LAYOUT_C = android.R.layout.simple_spinner_item;

	private List<CharSequence> items = new ArrayList<CharSequence>();

	/*
	 * (Used by the layout inflater)
	 */
	public IbiSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_list_item_1, items);
		adapter.setDropDownViewResource(SPINNER_LAYOUT_A);
		setAdapter(adapter);
	}

	public void setItems(List<CharSequence> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyItemsChanged();
	}

	public void addItem(CharSequence item) {
		items.add(item);
		notifyItemsChanged();
	}

	public void removeItemAt(int index) {
		items.remove(index);
		notifyItemsChanged();
	}

	/**
	 * Selects given object (if present).
	 * 
	 * @param object
	 */
	public void setSelection(CharSequence object) {
		setSelection(items.indexOf(object));
	}

	public List<CharSequence> getItems() {
		return items;
	}

	@SuppressWarnings("unchecked")
	private void notifyItemsChanged() {
		((ArrayAdapter<CharSequence>) getAdapter()).notifyDataSetChanged();
	}

}
