package com.ibisek.outlanded.navigation.gps;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.ibisek.outlanded.R;

/**
 * @author ibisek
 * @version 2014-04-18
 */
public class GPSReader2 extends Service implements LocationListener {

	private final static String TAG = GPSReader2.class.getSimpleName();

	// The minimum distance to change Updates in meters
	private static long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // in meters
	// The minimum time between updates in milliseconds
	private static long MIN_TIME_BW_UPDATES = 500; // in milliseconds

	private static final SparseArray<String> GPS_STATUS_REASONS = new SparseArray<String>();

	static {
		GPS_STATUS_REASONS.put(LocationProvider.AVAILABLE, "AVAILABLE");
		GPS_STATUS_REASONS.put(LocationProvider.OUT_OF_SERVICE, "OUT_OF_SERVICE");
		GPS_STATUS_REASONS.put(LocationProvider.TEMPORARILY_UNAVAILABLE, "TEMPORARILY_UNAVAILABLE");
	}

	private static GPSReader2 instance;

	private final Context context;
	private LocationManager locationManager;
	private List<LocationListener> locationListeners = new ArrayList<LocationListener>();
	private int numSatellites;

	/**
	 * @param context
	 * @param minDistance
	 *          in meters; use null to keep default settings
	 * @param minTime
	 *          in milliseconds; use null to keep default settings
	 */
	private GPSReader2(Context context, Long minDistance, Long minTime) {
		log("GPSReader2() constructor START");

		this.context = context;
		if (minDistance != null)
			MIN_DISTANCE_CHANGE_FOR_UPDATES = minDistance;
		if (minTime != null)
			MIN_TIME_BW_UPDATES = minTime;

		initGps(context);

		log("GPSReader2() constructor END");
	}

	/**
	 * @param context
	 * @return
	 */
	public static GPSReader2 getInstance(Context context) {
		if (instance == null)
			instance = new GPSReader2(context, null, null);

		return instance;
	}

	/**
	 * @param minDistance
	 *          in meters; use null to keep default settings
	 * @param minTime
	 *          in milliseconds; use null to keep default settings
	 */
	public void configure(Long minDistance, Long minTime) {
		if (minDistance != null)
			MIN_DISTANCE_CHANGE_FOR_UPDATES = minDistance;
		if (minTime != null)
			MIN_TIME_BW_UPDATES = minTime;

		initGps(context);
	}

	private void initGps(Context context) {
		try {
			locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

			// getting GPS status
			boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			log("isGPSEnabled=" + isGPSEnabled);

			if (!isGPSEnabled)
				showSettingsAlert();

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
			log("GPS reader registered");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to show settings alert dialog
	 */
	private void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

		// Setting Dialog Title
		alertDialog.setTitle(context.getString(R.string.gps_settings_title));

		// Setting Dialog Message
		alertDialog.setMessage(context.getString(R.string.gps_settings_dlg_text));

		// Setting Icon to Dialog
		// alertDialog.setIcon(R.drawable.delete);

		// On pressing Settings button
		alertDialog.setPositiveButton(context.getString(R.string.btn_setting), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(intent);
			}
		});

		// on pressing cancel button
		alertDialog.setNegativeButton(context.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		// Showing Alert Message
		alertDialog.show();
	}

	/**
	 * Reinitializes the GPS source and starts listening.
	 */
	public void resumeUsingGps() {
		log("GPS resume requested.");
		initGps(context);
	}

	/**
	 * Stops using GPS source.
	 */
	public void stopUsingGPS() {
		log("GPS stop requested.");
		if (locationManager != null)
			locationManager.removeUpdates(GPSReader2.this);
	}

	public void addListener(LocationListener ll) {
		locationListeners.add(ll);
	}

	public void removeListener(LocationListener ll) {
		locationListeners.remove(ll);
	}

	@Override
	public void onLocationChanged(Location location) {
		String msg = "onLocationChanged(): " + location;
		log(msg);

		if (locationListeners.size() > 0) {
			// add numSatellites field:
			Bundle extras = location.getExtras();
			if (extras == null)
				extras = new Bundle();
			extras.putInt("satellites", numSatellites);
			location.setExtras(extras);

			// tell all registered listeners:
			for (LocationListener l : locationListeners) {
				l.onLocationChanged(location);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		String msg = "Provider disabled: " + provider;
		log(msg);
	}

	@Override
	public void onProviderEnabled(String provider) {
		String msg = "Enabled new provider: " + provider;
		log(msg);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

		String status2 = GPS_STATUS_REASONS.get(status);
		if (status2 == null)
			status2 = String.format("UNKNOWN (%s)", status);

		numSatellites = extras.getInt("satellites");

		String msg = "onStatusChanged(): provider=" + provider + "; status=" + status2 + "; numSatellites=" + numSatellites;
		// Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		log(msg);
		
		// ..and notify our listeners:
		for (LocationListener listener : locationListeners) {
			listener.onStatusChanged(provider, status, extras);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		String msg = "onBind(): intent=" + intent;
		Log.d(TAG, msg);
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		stopUsingGPS();
	}

	/**
	 * @return last known location or null if not available
	 */
	public Location getLocation() {
		Location location = null;

		locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
		if (locationManager != null) {
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			if (location != null && new Date().getTime() - location.getTime() > 5 * 60 * 1000) // 5 min
				location = null; // this is an old fix!
		}

		return location;
	}

	private void log(String msg) {
		Log.d(TAG, msg);
	}

}
