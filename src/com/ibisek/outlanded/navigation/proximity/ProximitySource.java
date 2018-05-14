package com.ibisek.outlanded.navigation.proximity;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.util.Log;

import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.gps.GpsMath;

public class ProximitySource {

	private static final String TAG = ProximitySource.class.getSimpleName();

	private static ProximitySource instance;
	private boolean initialised = false;
	private Context context;
	private int minDistance, numPoints;

	private double prevLat = -1, prevLon = -1;
	private List<ProximityListener> listeners = new ArrayList<ProximityListener>(1);

	private LocationEngine locationEngine;
	private List<POI> nearestPoisList;

	private Vector<LocationResolutionRequest> locationResolutionQueue = new Vector<LocationResolutionRequest>();
	private volatile String locationResolutionQueueLock = "QUEUE_LOCK";
	
	public static ProximitySource getInstance() {
		if (instance == null)
			instance = new ProximitySource();

		return instance;
	}

	/**
	 * This NEEDS to be called before first use. Otherwise the locationEngine will be null (!)
	 * 
	 * @param minDistance minimal distance in metres to notify listeners
	 * @param numPoints number of vicinity POIs
	 * @param filePath
	 */
	public void init(Context context, int minDistance, int numPoints) {
		if (!initialised) { // only first init counts
			this.context = context;
			this.minDistance = minDistance;
			this.numPoints = numPoints;

			new Thread(new Runnable() {
				@Override
				public void run() {
					locationEngine = new LocationEngine(ProximitySource.this.context);
					initialised = true;
				}
			}).start();
		}
	}

	/**
	 * Init of locationEngine may take some time..
	 * @return false when not ready for use
	 */
	public boolean isInitialised() {
		return initialised;
	}

	/**
	 * The entry point where this class is updated with actual GPS coords from an
	 * external source.
	 * 
	 * Intended for continuous location resolutions.
	 * 
	 * @param latitude in radians
	 * @param longitude in radians
	 */
	public void updateLocation(float latitude, float longitude) {
		float dist = Float.MAX_VALUE;
		if (prevLat != -1 && prevLon != -1) // is set already
			dist = GpsMath.getDistanceInM(prevLat, prevLon, latitude, longitude);

		Log.d(TAG, String.format("dist=%s; minDistance=%s", dist, minDistance));
		if (dist > minDistance) {
			
			synchronized (locationResolutionQueueLock) {
				nearestPoisList = locationEngine.findNearest(latitude, longitude, numPoints);
			}

			if (nearestPoisList.size() > 0)
				for (ProximityListener l : listeners)
					l.notify(nearestPoisList);

			prevLat = latitude;
			prevLon = longitude;
		}
	}
	
	/**
	 * Intended for one-shot location resolutions.
	 * 
	 * @param latitude in radians
	 * @param longitude in radians
	 * @param listener to receive the POI list
	 */
	public void resolveLocation(float latitude, float longitude, ProximityListener listener) {
		
		// add the request to a queue:
		LocationResolutionRequest req = new LocationResolutionRequest(latitude, longitude, listener);
		locationResolutionQueue.add(req);
		
		// process items in the queue:
		while(locationResolutionQueue.size() > 0) {
			synchronized (locationResolutionQueueLock) {
				LocationResolutionRequest lrr = locationResolutionQueue.remove(0);
				List<POI> nearestPoisList = locationEngine.findNearest(lrr.getLatitude(), lrr.getLongitude(), numPoints);
				lrr.getListener().notify(nearestPoisList);
			}
		}
	}

	/**
	 * Adds a proximity listener which will be notified with updated list of
	 * nearest points when location changes at least the minDistance.
	 * 
	 * @param proximityListener
	 */
	public void addListener(ProximityListener proximityListener) {
		listeners.add(proximityListener);
	}

	/**
	 * @param proximityListener
	 */
	public void removeListener(ProximityListener proximityListener) {
		listeners.remove(proximityListener);
	}

	/**
	 * @return list of lastly recorded nearest points list. May return null if no
	 *         GPS fix no nearest points found yet.
	 */
	public List<POI> getNearestPointsList() {
		return nearestPoisList;
	}

	/**
	 * This is a data wrapper to store lookup requests in a queue.
	 */
	private class LocationResolutionRequest {

		private float latitude, longitude;
		private ProximityListener listener;
		
		public LocationResolutionRequest(float latitude, float longitude, ProximityListener listener) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.listener = listener;
		}
		
		public float getLatitude() {
			return latitude;
		}

		public float getLongitude() {
			return longitude;
		}

		public ProximityListener getListener() {
			return listener;
		}
	}
	
}
