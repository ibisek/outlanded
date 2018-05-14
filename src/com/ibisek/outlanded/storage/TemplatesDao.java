package com.ibisek.outlanded.storage;

import java.util.List;

import com.ibisek.outlanded.R;

import android.content.Context;

public class TemplatesDao {

	private static final String MESSAGE_TEMPLATES_FILENAME = "message-templates.bin";

	private Context context;
	private InternalStorage<Templates> storage;

	public TemplatesDao(Context context) {
		this.context = context;
		storage = new InternalStorage<Templates>(MESSAGE_TEMPLATES_FILENAME, context);
	}

	/**
	 * Loads templates from internal storage file. In case the file does not exist
	 * (yet), populates it with default templates.
	 * 
	 * @return
	 */
	public Templates loadTemplates() {
		Templates templates = storage.load();

		if (templates == null) { // populate with default templates
			templates = new Templates();

			List<String> list = templates.getTemplates();
			list.add(context.getString(R.string.template_default_1));
			list.add(context.getString(R.string.template_default_2));
			list.add(context.getString(R.string.template_default_3));

			templates.setSelectedIndex(0);

			saveTemplates(templates);
		}

		return templates;
	}

	public void saveTemplates(Templates templates) {
		storage.save(templates);
	}

}
