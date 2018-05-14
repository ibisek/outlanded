package com.ibisek.outlanded;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ibisek.outlanded.storage.Templates;
import com.ibisek.outlanded.storage.TemplatesDao;

public class TemplateConfigurationActivity extends Activity {

	final int SPINNER_LAYOUT_A = android.R.layout.simple_spinner_dropdown_item;
	final int SPINNER_LAYOUT_B = android.R.layout.simple_list_item_1;
	final int SPINNER_LAYOUT_C = android.R.layout.simple_spinner_item;

	private Spinner spinner;
	private EditText templateEdit;
	private Button addBtn;
	private Button removeBtn;

	private TemplatesDao templatesDao;
	private Templates templates;
	private List<CharSequence> spinnerItems = new ArrayList<CharSequence>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_template_configuration);

		spinner = (Spinner) findViewById(R.id.spinner);
		templateEdit = (EditText) findViewById(R.id.templateEdit);
		templateEdit.setOnTouchListener(new TemplateEditOnTouchListener());

		addBtn = (Button) findViewById(R.id.addBtn);
		removeBtn = (Button) findViewById(R.id.removeBtn);

		templatesDao = new TemplatesDao(this);
		templates = templatesDao.loadTemplates();

		// spinner setup:
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, updateSpinnerItems());
		adapter.setDropDownViewResource(SPINNER_LAYOUT_A);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerItemSelectedListener());

		addBtn.setOnClickListener(new AddButtonListener());
		removeBtn.setOnClickListener(new RemoveBtnListener());
	}

	@Override
	protected void onResume() {
		super.onResume();

		// update selection:
		spinner.setSelection(templates.getSelectedIndex());
		templateEdit.setText(templates.getSelectedTemplate());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.configuration, menu);
		return true;
	}

	private List<CharSequence> updateSpinnerItems() {
		spinnerItems.clear();

		for (String template : templates.getTemplates()) {
			spinnerItems.add(template.substring(0, template.length() > 16 ? 16 : template.length()) + "..");
		}

		return spinnerItems;
	}

	/**
	 * Checks, whether currently listed template has been modified. If so, updates
	 * it in the templates list.
	 */
	private void checkCurrentTemplateDirty() {
		int selectedTemplateIndex = templates.getSelectedIndex();

		if (selectedTemplateIndex >= 0 && selectedTemplateIndex < templates.getNumTemplates()) {
			String templateText = templateEdit.getText().toString();
			String oldTemplateText = templates.getTemplates().get(selectedTemplateIndex);

			// keep the template if modified
			if (!templateText.equals(oldTemplateText)) {
				templates.getTemplates().set(selectedTemplateIndex, templateText);

				updateSpinnerItems();
				((ArrayAdapter<CharSequence>) spinner.getAdapter()).notifyDataSetChanged();
			}

		}
	}

	@Override
	public void onBackPressed() {
		checkCurrentTemplateDirty();
		templatesDao.saveTemplates(templates);

		// go back to main with the information of selected template:
		Intent i = new Intent(this, MainActivity.class);
		finish();

		// super.onBackPressed(); // and go back to main
	}

	private class AddButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			templates.getTemplates().add(getString(R.string.template_help));

			updateSpinnerItems();
			((ArrayAdapter<CharSequence>) spinner.getAdapter()).notifyDataSetChanged();

			// select the last (new) one:
			spinner.setSelection(templates.getNumTemplates() - 1);

			templateEdit.selectAll(); // select the help text

			String msg = getString(R.string.template_created);
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

		}
	}

	private class RemoveBtnListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			int selectedIndex = spinner.getSelectedItemPosition();
			Log.d("", "selectedIndex=" + selectedIndex);

			if (templates.getNumTemplates() > 1) { // we must retain at least one
																							// template
				templates.removeTemplate(selectedIndex);

				updateSpinnerItems();
				((ArrayAdapter<CharSequence>) spinner.getAdapter()).notifyDataSetChanged();

				// select index-1 (or 0) item
				int newIndex = templates.getSelectedIndex();
				spinner.setSelection(newIndex);
				templateEdit.setText(templates.getSelectedTemplate());

				String msg = getString(R.string.template_removed);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private class TemplateEditOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			// if the editor contains help text, select all:
			String val = templateEdit.getText().toString();
			String helpText = TemplateConfigurationActivity.this.getString(R.string.template_help);
			if (helpText.equals(val))
				templateEdit.selectAll();

			return false;
		}

	}

	private class SpinnerItemSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			Log.d("", String.format("onItemSelected(): pos=%s; id=%s", position, id));

			checkCurrentTemplateDirty();

			templates.setSelectedIndex(position); // newly selected
			templateEdit.setText(templates.getSelectedTemplate());
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			Log.d("", String.format("onNothingSelected(): parent=%s", parent));
		}

	}
}
