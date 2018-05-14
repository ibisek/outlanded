package com.ibisek.outlanded.components;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * EditText with extended functionality.
 * 
 * @author ibisek
 * @version 2013-10-02
 */
public class EditText2 extends EditText {

	private String initialText;

	public EditText2(Context context) {
		super(context);
		init(null);
	}

	public EditText2(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public EditText2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	/**
	 * @param attrs
	 * @param attrName
	 * @return attribute value
	 */
	private String getAttributeValue(AttributeSet attrs, String attrName) {
		// (1) try to get it as resource:
		int resourceId = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", attrName, 0);
		if (resourceId != 0) {
			String attrValue = getResources().getString(resourceId);
			return attrValue;
		}

		// (2) or just return a 'raw' value:
		if (attrs != null) {
			for (int i = 0; i < attrs.getAttributeCount(); i++) {
				String name = attrs.getAttributeName(i);
				if (name!= null && name.toLowerCase().equals(attrName)) {
					String attrValue = attrs.getAttributeValue(i);
					return attrValue;
				}
			}
		}
		return null;
	}

	private void init(AttributeSet attrs) {
		initialText = getAttributeValue(attrs, "text");
		setSelectAllOnFocus(true);
		this.addTextChangedListener(new MyTextWatcher());
	}

	/**
	 * Sets the text if the text is not null. If so, the original value will
	 * remain.
	 * 
	 * @param text
	 */
	public void setText2(CharSequence text) {
		if (text != null)
			super.setText(text);
	}

	/**
	 * @return true if the text is equal to initial
	 */
	public boolean isEmpty() {
		return getText().toString().equals(initialText) || getText().toString().isEmpty();
	}

	private class MyTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() == 0) {
				setText(initialText);
				selectAll();
			}
			EditText2.this.setError(null);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// nix
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// nix
		}

	}
}
