package com.ibisek.outlanded.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Templates implements Serializable {

	private static final long serialVersionUID = -6237314433463841153L;

	private List<String> templates = new ArrayList<String>();
	private int selectedIndex = 0;

	public List<String> getTemplates() {
		return templates;
	}

	public void setSelectedIndex(int selectedIndex) {
		this.selectedIndex = selectedIndex;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public String getSelectedTemplate() {
		return templates.get(selectedIndex);
	}

	public int getNumTemplates() {
		return templates.size();
	}

	public void removeTemplate(int index) {
		templates.remove(index);
		selectedIndex = (selectedIndex > 0 ? selectedIndex - 1 : 0);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("#Templates: num=" + getNumTemplates());
		for (int i = 0; i < templates.size(); i++) {
			String template = templates.get(i);
			sb.append(String.format("\n [%s] %s", i, template));
		}

		return sb.toString();
	}

}
