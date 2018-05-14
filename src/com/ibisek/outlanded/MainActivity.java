package com.ibisek.outlanded;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ibisek.outlanded.components.ProximityDisplayComponent;
import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.gps.GPSReader2;
import com.ibisek.outlanded.navigation.gps.GpsUtils;
import com.ibisek.outlanded.navigation.gps.GpsUtils.GPS_FORMAT;
import com.ibisek.outlanded.navigation.proximity.ProximityListener;
import com.ibisek.outlanded.navigation.proximity.ProximitySource;
import com.ibisek.outlanded.net.UpdateQuery;
import com.ibisek.outlanded.storage.LastUsedMessageDao;
import com.ibisek.outlanded.storage.TemplatesDao;
import com.ibisek.outlanded.utils.CompetitionModeLocationSender;
import com.ibisek.outlanded.utils.Configuration;
import com.ibisek.outlanded.utils.MemoryCache;
import com.ibisek.outlanded.utils.SmsSender;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "MainActivity";

	public final static int SELECT_CONTACT_REQ_CODE = 1;
	public final static int SELECT_PROXIMITY_ITEM_REQ_CODE = 2;
	public final static int SELECT_TEMPLATE_REQ_CODE = 3;

	public final static String KEY_SELECTED_POI_INDEX = "SELECTED_POI_INDEX";
	public final static String KEY_SELECTED_CONTACT_NUMBER = "SELECTED_CONTACT_NUMBER";
	public final static String KEY_SELECTED_CONTACT_EMAIL = "SELECTED_CONTACT_EMAIL";
	// used when starting activity for result and need the requestCode in there:
	public final static String KEY_REQUEST_CODE = "requestCode";

	private TextView textLongitude;
	private TextView textLatitude;
	private TextView textAltitude;
	private ProximityDisplayComponent proximityDisplayComponent;
	private EditText smsText;
	private ImageButton selectContactBtn;
	private EditText editPhoneNumber;
	private ImageButton sendMsgBtn;

	private GPSReader2 gpsReader;
	private GPS_FORMAT gpsFormat;
	private boolean gpsLocationFound = false;
	private GPSLocationListener gpsLocationListener;
	double latitude, longitude, altitude; // last known position for general use

	private SmsSender smsSender;
	private TemplatesDao templatesDao;
	private LastUsedMessageDao lastUsedMessageDao;

	private ProximitySource proximitySource;

	/**
	 * @see https://developer.android.com/reference/android/os/StrictMode.html
	 */
	@SuppressWarnings("unused")
	private void enableDeveloperMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		// .detectDiskReads()
		// .detectDiskWrites()
		// .detectNetwork() // or .detectAll() for all detectable problems
				.detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
		// .detectLeakedClosableObjects()
				.penaltyLog().penaltyDeath().build());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// enableDeveloperMode();
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");

		setContentView(R.layout.activity_main);

		textLongitude = (TextView) findViewById(R.id.textLongitude);
		textLatitude = (TextView) findViewById(R.id.textLatitude);
		// add GPS format dialog:
		textLongitude.setOnTouchListener(new GpsFormatChangeOnTouchListener());
		textLatitude.setOnTouchListener(new GpsFormatChangeOnTouchListener());

		textAltitude = (TextView) findViewById(R.id.textAltitude);
		proximityDisplayComponent = (ProximityDisplayComponent) findViewById(R.id.proximityDisplayComponent);

		selectContactBtn = (ImageButton) findViewById(R.id.selectContactBtn);
		selectContactBtn.setOnClickListener(new PhoneNumberSelector());

		editPhoneNumber = (EditText) findViewById(R.id.editPhoneNumber);
		editPhoneNumber.setOnTouchListener(new PhoneNumberOnTouchListener());

		templatesDao = new TemplatesDao(this);
		lastUsedMessageDao = new LastUsedMessageDao(this);

		smsText = (EditText) findViewById(R.id.editText);
		String lastUsedMessage = lastUsedMessageDao.getLastUsedMessage();
		if (lastUsedMessage != null)
			smsText.setText(lastUsedMessage);
		else
			smsText.setText(templatesDao.loadTemplates().getSelectedTemplate());

		sendMsgBtn = (ImageButton) findViewById(R.id.sendSmsBtn);
		sendMsgBtn.setOnClickListener(new SendMessageBtnListener());
		enableSendMsgBtn(false);

		gpsReader = GPSReader2.getInstance(this);
		gpsLocationListener = new GPSLocationListener();
		gpsReader.addListener(gpsLocationListener);

		gpsFormat = Configuration.getInstance(this).getGpsFormat();

		smsSender = new SmsSender(this);

		// init proximity source:
		int minDistance = 4; // 4m
		int numPoints = Configuration.getInstance(getApplicationContext()).getNumProximityItems();
		proximitySource = ProximitySource.getInstance();
		proximitySource.init(this, minDistance, numPoints);
		gpsReader.addListener(new ProximitySourceGpsLocationListener());
		proximitySource.addListener(new MyProximityListener());

		// show proximity display
		proximityDisplayComponent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, ProximityDisplayActivity.class);
				i.putExtra("requestCode", SELECT_PROXIMITY_ITEM_REQ_CODE);
				startActivityForResult(i, SELECT_PROXIMITY_ITEM_REQ_CODE);
			}
		});

		// to shutdown cleanly:
		Runtime.getRuntime().addShutdownHook(new MyShutdownHook());

		// to log unexpected exceptions:
		// (does not really work yet)
		// Thread.currentThread().setUncaughtExceptionHandler(new MyUncaughtExceptionHandler(getApplicationContext()));

		// check for updates:
		String currentVersion = Configuration.getAppVersion();
		String onServerVersion = Configuration.getInstance(this).getOnServerVersion();
		if (onServerVersion != null && !currentVersion.equals(onServerVersion))
			Toast.makeText(this, this.getString(R.string.update_available_toast), Toast.LENGTH_LONG).show();

		else { // we might have current version but; still, check when it is time:
			long updateCheckDelta = 14 * 24 * 60 * 60 * 1000; // 14 days in millis
			Date lastUpdateCheckDate = Configuration.getInstance(this).getLastUpdateCheckDate();
			if (lastUpdateCheckDate == null || (new Date().getTime() - lastUpdateCheckDate.getTime()) > updateCheckDelta) {
				UpdateQuery x = new UpdateQuery(MainActivity.this, Configuration.getVersionUrl(), Configuration.getAppVersion());
				x.performBackgroundUpdateCheckOnly(true);
				x.execute();
			}
		}

		// force competition-related data to are filled-in:
		// String competitionNo = Configuration.getInstance(this).getCompetitionNo();
		// if (competitionNo == null || competitionNo.isEmpty()) {
		// LayoutableDialogFragmentController controller = new CompetitionFormController();
		// LayoutableDialogFragment dialog = LayoutableDialogFragment.create(getString(R.string.config_competition_title), R.layout.fragment_competition_form,
		// false, controller);
		// dialog.show(getSupportFragmentManager(), "competitionDialog");
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");

		smsSender.onResume();
		gpsReader.resumeUsingGps();

		// onResume() most of the components lose values; update:
		// (1) update the proximityDisp, otherwise it seems not to have a location
		if (proximitySource.getNearestPointsList() != null) {
			POI selectedPoi = MemoryCache.getInstance().getSelectedPoi();
			POI nearestPoi = null;
			if (selectedPoi != null)
				nearestPoi = selectedPoi;
			else {
				List<POI> pois = proximitySource.getNearestPointsList();
				if (pois.size() > 0)
					nearestPoi = pois.get(0);
			}

			if (nearestPoi != null) {
				proximityDisplayComponent.updatePosition(nearestPoi);
				updateSmsText(null, nearestPoi);
			}
		}
		// (2) update selected phone number (if any):
		String selectedPhoneNumber = MemoryCache.getInstance().getSelectedPhoneNumber();
		if (selectedPhoneNumber != null)
			editPhoneNumber.setText(selectedPhoneNumber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, String.format("onPause(): isFinishing=%s", isFinishing()));

		gpsReader.stopUsingGPS();

		smsSender.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "onStop()");
		// gpsReader.stopUsingGPS();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
	}

	@Override
	public void onBackPressed() {
		// end the app:
		moveTaskToBack(true); // this should do the job
		System.exit(0); // just for sure
	}

	/**
	 * Handles menu onClick event.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_template_configuration:
			Intent templateConfigIntent = new Intent(this, TemplateConfigurationActivity.class);
			startActivityForResult(templateConfigIntent, SELECT_TEMPLATE_REQ_CODE);
			return true;

		case R.id.action_map:
			if (gpsLocationFound) {
				Location loc = gpsReader.getLocation();
				String uri = String.format(Locale.ENGLISH, "geo:0,0?q=%f,%f", loc.getLatitude(), loc.getLongitude());
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				startActivity(intent);
			} else
				Toast.makeText(this, getString(R.string.gps_no_gps_fix), Toast.LENGTH_SHORT).show();
			return true;

		case R.id.action_configuration:
			Intent configIntent = new Intent(this, ConfigurationActivity.class);
			startActivity(configIntent);
			return true;

		case R.id.action_about:
			// XXX debug location insertion !!REMOVE!!
			// Location l = new Location("XXX");
			// l.setLatitude(49.385372); // u Krizanova
			// l.setLongitude(16.095213);
			// l.setLatitude(49.228192); // Zlin
			// l.setLongitude(17.658439);
			// gpsReader.onLocationChanged(l);

			// Log.d(TAG, "### before the exception ###");
			// throw new RuntimeException("Test exception.");

			// Intent i = new Intent(this, LogSenderService.class);
			// i.putExtra(LogSenderService.LOG_KEY, "ahojvole!");
			// startService(i);

			Intent aboutIntent = new Intent(this, AboutActivity.class);
			startActivity(aboutIntent);
			return true;

		case R.id.action_update:
			UpdateQuery x = new UpdateQuery(MainActivity.this, Configuration.getVersionUrl(), Configuration.getAppVersion());
			x.performBackgroundUpdateCheckOnly(false); // requested by user - show_dialogs
			x.execute();
			return true;

			// case R.id.help:
			// showHelp();
			// return true;

		case R.id.action_sms_list:
			Intent smsListIntent = new Intent(this, SmsListActivity.class);
			startActivity(smsListIntent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null) {
			Bundle receiveBundle = data.getExtras();

			if (requestCode == SELECT_CONTACT_REQ_CODE && resultCode == RESULT_OK) {
				if (receiveBundle.containsKey(KEY_SELECTED_CONTACT_NUMBER)) {
					String selectedPhoneNumber = receiveBundle.getString(KEY_SELECTED_CONTACT_NUMBER);
					data.removeExtra(KEY_SELECTED_CONTACT_NUMBER);
					MemoryCache.getInstance().setSelectedPhoneNumber(selectedPhoneNumber);
					editPhoneNumber.setText(selectedPhoneNumber);
				}
				if (receiveBundle.containsKey(KEY_SELECTED_CONTACT_EMAIL)) {
					String selectedEmail = receiveBundle.getString(KEY_SELECTED_CONTACT_EMAIL);
					data.removeExtra(KEY_SELECTED_CONTACT_EMAIL);
					MemoryCache.getInstance().setSelectedEmail(selectedEmail);
				}

			} else if (requestCode == SELECT_PROXIMITY_ITEM_REQ_CODE) {
				// handle selected proximity item, i.e. update selected location in the
				// ProximityDisplayComponent:

				if (receiveBundle.containsKey(KEY_SELECTED_POI_INDEX)) {
					int selectedPoiIndex = receiveBundle.getInt(KEY_SELECTED_POI_INDEX);
					this.getIntent().removeExtra(KEY_SELECTED_POI_INDEX);

					List<POI> nearestPointsList = proximitySource.getNearestPointsList();
					if (nearestPointsList != null && nearestPointsList.size() > 0) {
						if (selectedPoiIndex == -1) { // clear selectedPoi, use nearest:
							MemoryCache.getInstance().setSelectedPoi(null);

						} else {
							if (selectedPoiIndex < nearestPointsList.size()) {
								POI selectedPoi = nearestPointsList.get(selectedPoiIndex);
								MemoryCache.getInstance().setSelectedPoi(selectedPoi);
							}
						}
					}
				}
			}

		} else {// results which don't need data:
			if (requestCode == SELECT_TEMPLATE_REQ_CODE) {
				// use selected template:
				smsText.setText(templatesDao.loadTemplates().getSelectedTemplate());
			}
		}

	}

	private void enableSendMsgBtn(boolean enabled) {
		if (enabled) {
			sendMsgBtn.setVisibility(View.VISIBLE);
			sendMsgBtn.setEnabled(true);

		} else {
			sendMsgBtn.setVisibility(View.INVISIBLE);
			sendMsgBtn.setEnabled(false);
		}
	}

	/**
	 * Update either GPS coordinates or nearest POI information in the SMS template. May one or the other be null, only valid info will be updated.
	 * 
	 * @param gpsCoords [latitude, longitude] formatted GPS coordinates
	 * @param nearestPoi nearest or selected POI
	 */
	private void updateSmsText(String[] gpsCoords, POI nearestPoi) {

		// store cursor position:
		int cursorPosition = smsText.getSelectionStart();

		if (gpsCoords != null) {
			String sms = smsText.getText().toString();
			String pattern = "\\[(.*?)\\]";
			String updatedSms = sms.replaceAll(pattern, String.format("[%s %s]", gpsCoords[0], gpsCoords[1]));
			smsText.setText(updatedSms);
		}

		if (nearestPoi != null) {
			String name = nearestPoi.getName();
			float bearing = nearestPoi.getBearingToOrigin();
			String direction = new GpsUtils(MainActivity.this).bearingToDirection(bearing);
			float distance = nearestPoi.getDistanceToOrigin();

			String format = getResources().getString(R.string.locationFormatScreen);
			String sms = smsText.getText().toString();
			String pattern = "\\{(.*?)\\}";
			format = getResources().getString(R.string.locationFormatSMS);
			String updatedSms = sms.replaceAll(pattern, String.format("{" + format + "}", distance, direction, name));
			smsText.setText(updatedSms);
		}

		String competitionNo = Configuration.getInstance(this).getCompetitionNo();
		if (competitionNo != null && !competitionNo.isEmpty()) {
			String sms = smsText.getText().toString();
			String pattern = "\\((.*?)\\)";
			String updatedSms = sms.replaceAll(pattern, competitionNo);
			smsText.setText(updatedSms);
		}

		// restore cursor position:
		int textLen = smsText.getText().toString().length();
		cursorPosition = (cursorPosition < textLen ? cursorPosition : textLen - 1);
		smsText.setSelection(cursorPosition);
	}

	private class GPSLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (location == null)
				return;

			longitude = location.getLongitude();
			latitude = location.getLatitude();
			// float accuracy = location.getAccuracy();
			altitude = location.getAltitude();
			// float bearing = location.getBearing();
			// float speed = location.getSpeed();
			// long time = location.getTime();

			String[] gpsCoords = GpsUtils.format(gpsFormat, latitude, longitude);

			Log.d(TAG, String.format("location update: %s %s %s", gpsCoords[0], gpsCoords[1], altitude));

			// update the GUI:
			textLatitude.setText(gpsCoords[0]);
			textLongitude.setText(gpsCoords[1]);
			if (textAltitude != null) // not present in landscape mode
				textAltitude.setText(String.format("%.0f", altitude));

			updateSmsText(gpsCoords, null);

			if (!gpsLocationFound) {
				gpsLocationFound = true;
				enableSendMsgBtn(true);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	}

	private class SendMessageBtnListener implements OnClickListener {

		private boolean validPhoneNumberSelected(String phoneNumber) {
			Pattern p = Pattern.compile("^[+]{0,1}[0-9]+$");
			Matcher m = p.matcher(phoneNumber);
			if (m.find())
				return true;
			else
				return false;
		}

		private void sendSMS(String phoneNumber, String message) {
			if (!validPhoneNumberSelected(phoneNumber)) {
				String msg = getString(R.string.main_no_contact_selected);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
				return;
			}
			smsSender.sendSMS(phoneNumber, message);
		}

		private void sendEmail(String toMail, String subject, String message) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/html");
			i.putExtra(Intent.EXTRA_EMAIL, toMail);
			i.putExtra(Intent.EXTRA_SUBJECT, subject);
			i.putExtra(Intent.EXTRA_TEXT, message);
			startActivity(Intent.createChooser(i, getString(R.string.sendMsg)));
		}

		private void sendAnotherWay(String toNumber, String toMail, String subject, String message) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_EMAIL, toMail); // neprojevi se
			i.putExtra(Intent.EXTRA_PHONE_NUMBER, toNumber); // neprojevi se
			i.putExtra("address", toNumber); // funguje pro sms, jinak asi ne
			i.putExtra(Intent.EXTRA_SUBJECT, subject);
			i.putExtra(Intent.EXTRA_TEXT, message);
			i.putExtra("sms_body", message); // melo by fungovat i pro hangouts
			i.setType("text/plain");
			startActivity(Intent.createChooser(i, getString(R.string.sendMsg)));
		}

		@Override
		public void onClick(View v) {

			// display msg send options: SMS/email/other:
			Context context = MainActivity.this;
			AlertDialog.Builder builder = new AlertDialog.Builder(context);

			// Setting Dialog Title
			builder.setTitle(context.getString(R.string.sendMsgMethod));

			CharSequence[] items = { context.getString(R.string.sendMsgMethod1), context.getString(R.string.sendMsgMethod2), context.getString(R.string.sendMsgMethod3) };
			builder.setItems(items, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String message = smsText.getText().toString();
					message = message.replaceAll("[\\[\\]\\{\\}]", "");
					message = message.replaceAll("[°]", " ");

					// share landing location:
					if (Configuration.getInstance(MainActivity.this).isLocationSharingEnabled()) {
						String nearestPoi = null;
						List<POI> nearestPois = proximitySource.getNearestPointsList();
						if (nearestPois != null && nearestPois.size() > 0)
							nearestPoi = nearestPois.get(0).getName();

						Configuration config = Configuration.getInstance(MainActivity.this);
						new CompetitionModeLocationSender().sendToServer(config.getCompetitionNo(), config.getRegistrationNo(), latitude, longitude, nearestPoi);
					}

					String toMail = MemoryCache.getInstance().getSelectedEmail();
					String toNumber = editPhoneNumber.getText().toString();
					String subject = getString(R.string.app_name);

					switch (which) {
					case 0:
						sendSMS(toNumber, message);
						break;
					case 1:
						sendEmail(toMail, subject, message);
						break;
					case 2:
						sendAnotherWay(toNumber, toMail, subject, message);
						break;
					default:
						Toast.makeText(getApplicationContext(), "Unknown send msg option", Toast.LENGTH_LONG).show();
					}
				}
			});

			builder.show();
		}
	}

	private class PhoneNumberSelector implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(MainActivity.this, PhonebookActivity.class);
			startActivityForResult(i, SELECT_CONTACT_REQ_CODE);
		}
	}

	private class PhoneNumberOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			// Select the entire default text so it does not need to be deleted
			// manually when directly entering the phone number:
			String val = editPhoneNumber.getText().toString();
			String defaultVal = MainActivity.this.getString(R.string.selectContact);
			if (defaultVal.equals(val))
				editPhoneNumber.selectAll();

			view.performClick();
			return false;
		}

	}

	private class GpsFormatChangeOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP)
				showGpsFormatDialog();

			view.performClick();
			return true; // event consumed
		}

		/**
		 * Displays GPS format selector.
		 */
		private void showGpsFormatDialog() {
			Context context = MainActivity.this;
			AlertDialog.Builder builder = new AlertDialog.Builder(context);

			// Setting Dialog Title
			builder.setTitle(context.getString(R.string.gps_format_title));

			CharSequence[] items = { GPS_FORMAT.Geocaching.toString(), GPS_FORMAT.WGS84.toString(), GPS_FORMAT.RAW.toString() };
			builder.setItems(items, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					gpsFormat = GPS_FORMAT.values()[which];
					Configuration.getInstance(null).setGpsFormat(gpsFormat);

					// redraw the coordinates in new format:
					gpsLocationListener.onLocationChanged(gpsReader.getLocation());
				}
			});

			builder.show();
		}

	}

	private class ProximitySourceGpsLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			double longitude = location.getLongitude();
			double latitude = location.getLatitude();

			longitude = Math.toRadians(longitude);
			latitude = Math.toRadians(latitude);

			proximitySource.updateLocation((float) latitude, (float) longitude);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// nix
		}

		@Override
		public void onProviderEnabled(String provider) {
			// nix
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// nix
		}
	}

	private class MyProximityListener implements ProximityListener {

		@Override
		public void notify(List<POI> nearestPoints) {
			if (nearestPoints.size() > 0) {
				POI nearestPoi = null;

				POI selectedPoi = MemoryCache.getInstance().getSelectedPoi();
				if (selectedPoi != null)
					nearestPoi = selectedPoi;
				else
					nearestPoi = nearestPoints.get(0);

				// update location in the ProximityDisplayComponent:
				proximityDisplayComponent.updatePosition(nearestPoi);

				// update location in SMS:
				updateSmsText(null, nearestPoi);
			}
		}
	}

	private class MyShutdownHook extends Thread {

		@Override
		public void run() {
			Log.d(TAG, "Shutdown hook initiated");

			gpsReader.stopUsingGPS();
			Configuration.getInstance(getApplicationContext()).save();

			// save last used message:
			String message = smsText.getText().toString();
			lastUsedMessageDao.saveLastUsedMessage(message);

			Log.d(TAG, "Shutdown hook finished");

			finish();
		}
	}

}