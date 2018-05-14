package com.ibisek.outlanded.navigation.proximity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.ibisek.outlanded.navigation.POI;

public class LocationEngine {

	// files are explored in THIS order:
	private final static String[] BINARY_FILES = { "osm-parsed-lat-ordered-cz.bin", "osm-parsed-lat-ordered-sk.bin",
			"osm-parsed-lat-ordered-at.bin", "osm-parsed-lat-ordered-pl.bin", "osm-parsed-lat-ordered-de.bin", "osm-parsed-lat-ordered-se.bin",
			"osm-parsed-lat-ordered-ch.bin", "osm-parsed-lat-ordered-es.bin", "osm-parsed-lat-ordered-fi.bin", "osm-parsed-lat-ordered-fr.bin",
			"osm-parsed-lat-ordered-it.bin", "osm-parsed-lat-ordered-no.bin", "osm-parsed-lat-ordered-pt.bin", "osm-parsed-lat-ordered-uk.bin",
			"osm-parsed-lat-ordered-hu.bin", "osm-parsed-lat-ordered-nz.bin", "osm-parsed-lat-ordered-au.bin", "osm-parsed-lat-ordered-cs.bin" };

	private static final String TAG = LocationEngine.class.getSimpleName();

	private List<LocationFile> files;
	private LocationFile currentAreaFile;

	// public LocationEngine() {
	// files = new ArrayList<LocationFile>();
	//
	// // text files:
	// // for (String file : FILES) {
	// // files.add(new LocationFile("data/" + file));
	// // }
	//
	// // binary files:
	// for (String file : BINARY_FILES) {
	// files.add(new BinaryLocationFile("data/bin/" + file, null));
	// }
	// }

	/**
	 * Constructor for Android operation.
	 * 
	 * @param context
	 */
	public LocationEngine(Context context) {
		List<String> allAssets = new ArrayList<String>(0);
		try {
			allAssets = Arrays.asList(context.getResources().getAssets().list(""));
		} catch (IOException ex) {
			Log.e(TAG, "Cannot list assets: " + ex.getMessage());
		}

		files = new ArrayList<LocationFile>(BINARY_FILES.length);
		for (String fileName : BINARY_FILES) {
			if (allAssets.contains(fileName))
				files.add(new BinaryLocationFile(fileName, context));
			else
				Log.d(TAG, "Asset "+fileName+" not present; ignoring.");
		}
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return location file containing given coordinates
	 */
	private LocationFile findCurrentAreaFile(float latitude, float longitude) {
		for (LocationFile f : files) {
			if (f.isInside(latitude, longitude))
				return f;
			else
				f.discardLargeDataVolume();
		}
		return null;
	}

	/**
	 * @param latitude in radians!
	 * @param longitude in radians!
	 * @param n number of {@link POI}s to be listed
	 * @return N nearest {@link POI}s to given coordinates or an empty list when
	 *         we are out of all our data files.
	 */
	public List<POI> findNearest(float latitude, float longitude, int n) {
		if (currentAreaFile == null || !currentAreaFile.isInside(latitude, longitude)) {
			LocationFile newAreaFile = findCurrentAreaFile(latitude, longitude);
			if (currentAreaFile != null)
				currentAreaFile.discardLargeDataVolume();
			currentAreaFile = newAreaFile;
		}

		if (currentAreaFile != null)
			return currentAreaFile.findNearest(latitude, longitude, n);

		return new ArrayList<POI>(0);
	}

	// public static void main(String[] args) {
	//
	// // cesta na Sviny:
	// float latitude = (float) Math.toRadians(49.385012878);
	// float longitude = (float) Math.toRadians(16.094326973);
	// // boras, letiste:
	// // float latitude = Math.toRadians(57.697853);
	// // float longitude = Math.toRadians(12.849516);
	// // wasserkuppe, ASW:
	// // float latitude = Math.toRadians(50.490528);
	// // float longitude = Math.toRadians(9.85015);
	//
	// LocationEngine locationEngine = new LocationEngine();
	//
	// for (POI poi : locationEngine.findNearest(latitude, longitude, 20)) {
	// System.out.println(poi);
	// }
	// }
}
