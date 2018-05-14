package com.ibisek.outlanded.phonebook;

public class PhonebookEntry {

	private String contactName;
	private String phoneNumber;
	private String email;

	/**
	 * @param contactName
	 * @param phoneNumber
	 * @param email
	 */
	public PhonebookEntry(String contactName, String phoneNumber, String email) {
		this.contactName = contactName;
		this.phoneNumber = phoneNumber;
		this.email = email;
	}

	public String getContactName() {
		return contactName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public String getFormattedPhoneNumber() {
		// TODO regex parsing & formatting
		return String.format("%s", phoneNumber);
	}

	@Override
	public boolean equals(Object o) {

		if (o instanceof PhonebookEntry) {
			PhonebookEntry entry = (PhonebookEntry) o;

			if (contactName != null && contactName.equals(entry.getContactName()) && phoneNumber != null && phoneNumber.equals(entry.getPhoneNumber()))
				return true;
		}

		return false;
	}

}
