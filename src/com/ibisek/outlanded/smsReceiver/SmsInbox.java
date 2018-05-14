package com.ibisek.outlanded.smsReceiver;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SmsInbox {
	
	//	private final static String TAG = SmsInbox.class.getSimpleName();

	/**
	 * @param context
	 * @return all SMSs from phone's inbox
	 */
	public static List<Sms> readInbox(Context context) {

		List<Sms> records = new ArrayList<Sms>();

		Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
		
		int numRecords = cursor.getCount();
		if (numRecords > 0) {
			cursor.moveToFirst();
			do {
				Sms sms = new Sms();
				
				for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
					String key = cursor.getColumnName(idx);
					String value = cursor.getString(idx);
					sms.put(key, value);
				}
				
				records.add(sms);
				
			} while (cursor.moveToNext());
		}

		cursor.close();

		return records;
	}
}
