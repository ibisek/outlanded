package com.ibisek.outlanded;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibisek.outlanded.navigation.gps.GpsUtils;
import com.ibisek.outlanded.phonebook.PhonebookUtils;
import com.ibisek.outlanded.smsReceiver.BeepingService;
import com.ibisek.outlanded.smsReceiver.Sms;
import com.ibisek.outlanded.smsReceiver.SmsInbox;
import com.ibisek.outlanded.smsReceiver.SmsReceiver;

public class SmsListActivity extends Activity {

	private final static String TAG = SmsListActivity.class.getSimpleName();

	private MyListItemAdapter listItemAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_list);

		final ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(new MyOnClickListener());
		listItemAdapter = new MyListItemAdapter(this);
		listView.setAdapter(listItemAdapter);

		// stop beeping service:
		Intent beepingServiceIntent = new Intent(this, BeepingService.class);
		stopService(beepingServiceIntent);

		// clean notification area:
		SmsReceiver.cancelNotification(this);

		// read all SMSs:
		List<Sms> allTexts = SmsInbox.readInbox(this);
		Log.d(TAG, "Num SMS in the inbox: " + allTexts.size());

		// retain only incoming/received and those with GPS coordinates:
		List<Sms> values = new ArrayList<Sms>();
		Iterator<Sms> allTextsIterator = allTexts.iterator();
		while (allTextsIterator.hasNext()) {
			Sms sms = allTextsIterator.next();
			Float[] coords = GpsUtils.extractGpsCoordinates(sms.getString(Sms.BODY));
			if (coords[0] != null && coords[1] != null && Sms.MESSAGE_TYPE_INBOX.equals(sms.getInteger(Sms.TYPE)))
				values.add(sms);
		}
		Log.d(TAG, "Num SMS with GPS coords: " + values.size());

		// display filtered values in the list:
		listItemAdapter.setValues(values);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sms_list, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		// go to main screen
		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
	}

	private class MyListItemAdapter extends ArrayAdapter<String> {

		private Context context;
		private List<Sms> values;
		private DateFormat dateFormat, timeFormat;

		public MyListItemAdapter(Context context) {
			// new ArrayList<String>(0)
			super(context, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(new String[] { "to display the wait message" })));
			this.context = context;

			// SMS reception date-time format:
			dateFormat = android.text.format.DateFormat.getDateFormat(context);
			timeFormat = android.text.format.DateFormat.getTimeFormat(context);
			// dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.sms_list_item, parent, false);
			TextView lineSender = (TextView) rowView.findViewById(R.id.lineSender);
			TextView lineText = (TextView) rowView.findViewById(R.id.lineText);
			TextView lineDate = (TextView) rowView.findViewById(R.id.lineDate);

			if (values != null && position < values.size()) {
				Sms sms = values.get(position);

				String senderPhoneNumber = sms.getString(Sms.ADDRESS);
				String senderDisplayName = PhonebookUtils.findDisplayName(context, senderPhoneNumber);
				lineSender.setText((senderDisplayName != null ? senderDisplayName : senderPhoneNumber));

				Date date = new Date(sms.getLong(Sms.DATE));
				lineDate.setText(dateFormat.format(date) + " " + timeFormat.format(date));

				lineText.setText(sms.getString(Sms.BODY));

			} else {
				Toast.makeText(getApplicationContext(), getString(R.string.sms_list_no_messages), Toast.LENGTH_LONG).show();
				// ..and go to main screen:
				Intent i = new Intent(SmsListActivity.this, MainActivity.class);
				startActivity(i);
			}

			return rowView;
		}

		public void setValues(List<Sms> values) {
			if (values != null && values.size() > 0) {
				this.values = values;

				// pokud clear() vyhodi UnsupportedOperationException, tak je to tim, ze
				// kolekce/pole co je v kontruktoru super.ArrayAdapter-u nema na sobe
				// clear(!)
				super.clear();

				for (Sms sms : values)
					super.add(sms.getString(Sms.BODY));
			}
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

	private class MyOnClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			if (listItemAdapter.values.size() > position) {
				Sms sms = listItemAdapter.values.get(position);
				Float[] coordinates = GpsUtils.extractGpsCoordinates(sms.getString(Sms.BODY));
				if (coordinates[0] != null && coordinates[1] != null) {
					String uri = String.format("geo:%s,%s", coordinates[0], coordinates[1]);
					showMap(Uri.parse(uri));
				}
			}
		}

		/**
		 * @see https://developer.android.com/guide/components/intents-common.html#Maps
		 * @param geoLocation
		 */
		private void showMap(Uri geoLocation) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(geoLocation);
			if (intent.resolveActivity(getPackageManager()) != null) {
				Log.d(TAG, "showing map for " + geoLocation);
				startActivity(intent);

			} else {
				Log.d(TAG, "No map/navigation app installed.");
				Toast.makeText(getApplicationContext(), getString(R.string.sms_list_no_app), Toast.LENGTH_LONG).show();
			}
		}

	}

}
