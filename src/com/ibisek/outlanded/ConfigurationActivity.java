package com.ibisek.outlanded;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.ibisek.outlanded.components.IbiSpinner;
import com.ibisek.outlanded.utils.Configuration;
import com.ibisek.outlanded.utils.SmsSender.SMS_SENDER;

public class ConfigurationActivity extends Activity {

	final int SPINNER_LAYOUT_A = android.R.layout.simple_spinner_dropdown_item;

	private Configuration config;

	private IbiSpinner smsSenderSpinner;
	private EditText numProximityItems, competitionNo, registrationNo;
	private CheckBox peaksCheckBox, habitableCheckBox, smsFilteringEnabled, locationSharingEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configuration);

		config = Configuration.getInstance(getApplicationContext());

		smsSenderSpinner = (IbiSpinner) findViewById(R.id.smsSenderSpinner);

		List<CharSequence> items = Arrays.asList(new CharSequence[] { SMS_SENDER.VIA_MANAGER.toString(), SMS_SENDER.VIA_INTENT.toString() });
		smsSenderSpinner.setItems(items);
		smsSenderSpinner.setSelection(config.getSmsSender().toString());

		smsSenderSpinner.setOnItemSelectedListener(new SmsSendSpinnerItemSelectedListener());

		numProximityItems = (EditText) findViewById(R.id.numProximityItems);
		numProximityItems.setText(String.valueOf(config.getNumProximityItems()));
		numProximityItems.addTextChangedListener(new NumProximityItemsTextWatcher());

		CBOnCheckedChangeListener l = new CBOnCheckedChangeListener();
		habitableCheckBox = (CheckBox) findViewById(R.id.habitableCheckBox);
		habitableCheckBox.setChecked(config.displayHabitables());
		habitableCheckBox.setOnCheckedChangeListener(l);
		peaksCheckBox = (CheckBox) findViewById(R.id.peaksCheckBox);
		peaksCheckBox.setChecked(config.displayPeaks());
		peaksCheckBox.setOnCheckedChangeListener(l);

		smsFilteringEnabled = (CheckBox) findViewById(R.id.smsFilteringEnabled);
		smsFilteringEnabled.setChecked(config.isSmsFilteringEnabled());
		smsFilteringEnabled.setOnCheckedChangeListener(l);

		// competititon fields:
		competitionNo = (EditText) findViewById(R.id.competitionNo);
		competitionNo.setText(config.getCompetitionNo());
		competitionNo.addTextChangedListener(new CompetitionNoTextWatcher());
		registrationNo = (EditText) findViewById(R.id.registrationNo);
		registrationNo.setText(config.getRegistrationNo());
		registrationNo.addTextChangedListener(new RegistrationNoTextWatcher());
		locationSharingEnabled = (CheckBox) findViewById(R.id.locationSharingEnabled);
		locationSharingEnabled.setChecked(config.isLocationSharingEnabled());
		locationSharingEnabled.setOnCheckedChangeListener(l);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.configuration, menu);
		return true;

	}

	private class SmsSendSpinnerItemSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			SMS_SENDER smsSender = SMS_SENDER.valueOf(smsSenderSpinner.getItems().get(position).toString());
			Configuration.getInstance(getApplicationContext()).setSmsSender(smsSender);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// nix
		}

	}

	private class CBOnCheckedChangeListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton source, boolean isChecked) {
			if (source == habitableCheckBox) {
				ConfigurationActivity.this.config.setDisplayHabitables(isChecked);
			} else if (source == peaksCheckBox) {
				ConfigurationActivity.this.config.setDisplayPeaks(isChecked);
			} else if (source == smsFilteringEnabled) {
				ConfigurationActivity.this.config.setSmsFilteringEnabled(isChecked);
			} else if (source == locationSharingEnabled) {
				ConfigurationActivity.this.config.setLocationSharingEnabled(isChecked);
			}

			boolean displayHabitables = ConfigurationActivity.this.config.displayHabitables();
			boolean displayPeaks = ConfigurationActivity.this.config.displayPeaks();

			if (!displayHabitables && !displayPeaks)
				Toast.makeText(getApplicationContext(), getString(R.string.config_proximity_no_items_selected), Toast.LENGTH_LONG).show();
		}

	}

	private class NumProximityItemsTextWatcher implements TextWatcher {
		@Override
		public void afterTextChanged(Editable e) {
			String s = e.toString();
			if (s != null && !s.trim().isEmpty()) {
				int num = Integer.parseInt(s);
				if (num > 100) {
					Toast.makeText(getApplicationContext(), getString(R.string.config_num_proximity_items_msg), Toast.LENGTH_SHORT).show();
					num = Configuration.DEFAULT_NUM_PROXIMITY_ITEMS;
					numProximityItems.setText(String.valueOf(num));
				}

				Configuration.getInstance(getApplicationContext()).setNumProximityItems(num);
			}
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

	private class CompetitionNoTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable e) {
			String s = e.toString();
			//if (s != null && !s.trim().isEmpty()) 
			Configuration.getInstance(getApplicationContext()).setCompetitionNo(s);
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

	private class RegistrationNoTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable e) {
			String s = e.toString();
			//if (s != null && !s.trim().isEmpty()) 
			Configuration.getInstance(getApplicationContext()).setRegistrationNo(s);
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
