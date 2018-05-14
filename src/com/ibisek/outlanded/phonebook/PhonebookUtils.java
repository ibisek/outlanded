package com.ibisek.outlanded.phonebook;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;

public class PhonebookUtils {

	/**
	 * @param context
	 * @param phoneNumber
	 * @return display name if found, null if not
	 */
	public static String findDisplayName(Context context, String phoneNumber) {

		String displayName = null;

		// Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
		// ContactsContract.CommonDataKinds.Phone.NUMBER + " = " + phoneNumber, null, null);
		Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		}
		cursor.close();

		return displayName;
	}

	/**
	 * @param context
	 * @param contactId
	 *          contact ID from previous phone book lookup
	 * @return email if set, null otherwise
	 */
	public static String findEmail(Context context, String contactId) {
		String email = null;

		Cursor emailCursor = context.getContentResolver()
				.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
		if (emailCursor.getCount() > 0) {
			emailCursor.moveToFirst();
			email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
		}
		emailCursor.close();

		return email;
	}

}
