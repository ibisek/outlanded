package com.ibisek.outlanded;

import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.ibisek.outlanded.components.EditText2;
import com.ibisek.outlanded.components.LayoutableDialogFragmentController;
import com.ibisek.outlanded.utils.Configuration;

/**
 * Uses the fragment_competition_form.xml layout.
 * 
 * @author ibisek
 * @version 2015-02-27
 */
public class CompetitionFormController extends LayoutableDialogFragmentController {

	private Context context;

	private Map<String, EditText2> formItemsForValidation = new TreeMap<String, EditText2>();

	private EditText2 competitionNo, registrationNo;

	@Override
	public void onCreateView(View view) {
		this.context = view.getContext();

		Button saveButton = (Button) view.findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new SaveButtonOnClickListener());

		Button skipButton = (Button) view.findViewById(R.id.skipButton);
		skipButton.setOnClickListener(new SkipButtonOnClickListener());

		competitionNo = (EditText2) view.findViewById(R.id.competitionNo);
		registrationNo = (EditText2) view.findViewById(R.id.registrationNo);
		
		formItemsForValidation.put("competitionNo", competitionNo);
		formItemsForValidation.put("registrationNo", registrationNo);
		
		Configuration config = Configuration.getInstance(context);
		competitionNo.setText(config.getCompetitionNo());
		registrationNo.setText(config.getRegistrationNo());
	}

	/**
	 * Handles the submit button events.
	 */
	class SaveButtonOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {

			if (isFormValid()) {
				Configuration config = Configuration.getInstance(context);

				config.setCompetitionNo(competitionNo.getText().toString().toUpperCase());
				config.setRegistrationNo(registrationNo.getText().toString().toUpperCase());
				
				dialog.dismiss();
			}

		}

		/**
		 * Validates values in the form and highlights invalid fields.
		 */
		private boolean isFormValid() {
			String errMsg = context.getString(R.string.form_err_missing_or_wrong_value);

			for (String field : formItemsForValidation.keySet()) {
				EditText2 formItem = formItemsForValidation.get(field);

				if (formItem.isEmpty()) {
					formItem.setError(errMsg);
					formItem.requestFocus();
					return false;
				}
			}

			return true;
		}

	}

	/**
	 * Handles the skip button event.
	 */
	class SkipButtonOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			dialog.dismiss();
		}
	}

}
