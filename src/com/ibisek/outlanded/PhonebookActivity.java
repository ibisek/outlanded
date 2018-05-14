package com.ibisek.outlanded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibisek.outlanded.phonebook.PhonebookEntry;
import com.ibisek.outlanded.phonebook.PhonebookUtils;
import com.ibisek.outlanded.utils.Configuration;

public class PhonebookActivity extends Activity {

	private static final String TAG = PhonebookActivity.class.getSimpleName();
	private static final int PICK_CONTACT = 666;

	private MyListItemAdapter listItemAdapter;
	private boolean returningFromPhoneBookWithNoSelection = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phonebook);

		final ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(new MyOnClickListener());
		listItemAdapter = new MyListItemAdapter(this);
		listView.setAdapter(listItemAdapter);

		returningFromPhoneBookWithNoSelection = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.phonebook, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		// load LRU contacts stored in configuration:
		List<PhonebookEntry> list = Configuration.getInstance(getApplicationContext()).getLastRecentlyUsedContacts();
		listItemAdapter.setValues(list);

		// if there are no recent contact, go directly to phone's contacts (1=the phone book item):
		if (!returningFromPhoneBookWithNoSelection && listItemAdapter.values.size() <= 1) {
			goToPhoneContactList();
		}

	}

	@Override
	public void onPause() {
		super.onPause();

		saveLruContacts();
	}

	/**
	 * Saves current value list into properties (so it can be loaded upon onResume() after a number from the phone's list has been selected).
	 */
	private void saveLruContacts() {
		// make a copy so we can remove 0th item from the list:
		List<PhonebookEntry> lruEntries = new ArrayList<PhonebookEntry>();
		lruEntries.addAll(listItemAdapter.getValues());

		// save Last Recently Used contacts to configuration:
		if (lruEntries != null && lruEntries.size() > 1) {
			lruEntries.remove(0); // dummy item
			Configuration.getInstance(null).saveLastRecentlyUsedContacts(lruEntries);
		}
	}

	/**
	 * Opens the phone's internal contact list.
	 */
	private void goToPhoneContactList() {
		Log.d(TAG, "Will go to phone's contact list..");
		Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intent, PICK_CONTACT);
	}

	/**
	 * Redirects to {@link MainActivity} with selected phone number.
	 * 
	 * @param entry
	 */
	private void sendSelectedContactToMainScreen(PhonebookEntry entry) {
		Toast.makeText(getApplicationContext(), entry.getContactName(), Toast.LENGTH_SHORT).show();

		Intent returnIntent = new Intent(PhonebookActivity.this, MainActivity.class);
		setResult(RESULT_OK, returnIntent);
		returnIntent.putExtra(MainActivity.KEY_SELECTED_CONTACT_NUMBER, entry.getFormattedPhoneNumber());
		returnIntent.putExtra(MainActivity.KEY_SELECTED_CONTACT_EMAIL, entry.getEmail());
		finish();
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		Log.d(TAG, String.format("reqCode=%s; resultCode=%s", reqCode, resultCode));

		if (reqCode == PICK_CONTACT) {
			// no contact has been selected and there are no items in the LRU list:
			// (1 .. the first item in the list is phone book "button")
			if (resultCode == Activity.RESULT_CANCELED && listItemAdapter.values.size() <= 1) {
				returningFromPhoneBookWithNoSelection = true;
				Intent i = new Intent(PhonebookActivity.this, MainActivity.class);
				startActivity(i);

			} else if (resultCode == Activity.RESULT_OK) {
				Uri contactUri = data.getData();
				Cursor c = managedQuery(contactUri, null, null, null, null);
				if (c.moveToFirst()) {
					String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

					String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

					if ("1".equals(hasPhone)) {
						Cursor phoneCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
						phoneCursor.moveToFirst();
						String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						String displayName = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
						phoneCursor.close();

						// remove possible padding (comes on some phones):
						if (phoneNumber != null) {
							phoneNumber = phoneNumber.replace(" ", "");
							phoneNumber = phoneNumber.replace("-", "");
						}

						// get the contact's email:
						String email = PhonebookUtils.findEmail(getApplicationContext(), id);

						// add entry to the beginning of the list
						PhonebookEntry entry = new PhonebookEntry(displayName, phoneNumber, email);
						listItemAdapter.addValueToBeginning(entry);

						// save the list, othewise it would be overwritten by onResume()
						// when returning from the phone's phonebook:
						saveLruContacts();

						sendSelectedContactToMainScreen(entry);
					}
				}
			}
		} // if reqCode
	}

	private class MyOnClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

			if (position == 0) { // open the phone's contact list
				goToPhoneContactList();

			} else {
				// move selected entry to the beginning of the list:
				PhonebookEntry entry = listItemAdapter.getValues().get(position);
				listItemAdapter.addValueToBeginning(entry);

				sendSelectedContactToMainScreen(entry);
			}
		}
	}

	private class MyListItemAdapter extends ArrayAdapter<String> {

		private Context context;
		private List<PhonebookEntry> values = new ArrayList<PhonebookEntry>();

		public MyListItemAdapter(Context context) {
			// new ArrayList<String>(0)
			super(context, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(new String[] { "xxx sem neco :)" })));
			this.context = context;

			// setValues(new ArrayList<PhonebookEntry>()); //add the 0th dummy entry
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.phonebook_list_item, parent, false);
			TextView line1 = (TextView) rowView.findViewById(R.id.firstLine);
			TextView line2 = (TextView) rowView.findViewById(R.id.secondLine);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.compass);

			if (position == 0) {
				imageView.setImageResource(R.drawable.phonebook);
			} else {
				imageView.setImageResource(R.drawable.person);
			}

			if (values != null && position < values.size()) {
				line1.setText(values.get(position).getContactName());
				line2.setText(values.get(position).getFormattedPhoneNumber());
			}

			return rowView;
		}

		public void addValueToBeginning(PhonebookEntry phonebookEntry) {
			// if already present.. move it to the beginning:
			if (values.contains(phonebookEntry)) {
				if (values.indexOf(phonebookEntry) > 1) {
					values.remove(phonebookEntry);
					values.add(1, phonebookEntry); // the 0-th item is dummy (!)
				}
			} else { // add to the beginning:
				values.add(1, phonebookEntry); // the 0-th item is dummy (!)
				super.add(phonebookEntry.getContactName());
			}
		}

		public void setValues(List<PhonebookEntry> newValues) {
			if (newValues != null) {
				this.values.clear();
				this.values.addAll(newValues);

				// add (dummy) phone's phone book entry first:
				String item0Line1 = PhonebookActivity.this.getString(R.string.phone_book_item0_line1);
				String item0Line2 = PhonebookActivity.this.getString(R.string.phone_book_item0_line2);
				values.add(0, new PhonebookEntry(item0Line1, item0Line2, null));

				// pokud clear() vyhodi UnsupportedOperationException, tak je to tim, ze
				// kolekce/pole co je v kontruktoru super.ArrayAdapter-u nema na sobe
				// clear(!)
				super.clear();

				for (PhonebookEntry entry : values)
					super.add(entry.getContactName());
			}
		}

		/**
		 * @return values; don't forget to remove the 0-th dummy list item
		 */
		public List<PhonebookEntry> getValues() {
			return values;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}

}
