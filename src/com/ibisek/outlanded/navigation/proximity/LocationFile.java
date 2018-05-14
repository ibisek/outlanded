package com.ibisek.outlanded.navigation.proximity;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.utils.IndexSearch2;

public abstract class LocationFile {
	
	private static final String TAG = "LocationFile";

	// number of POIs to search around found index:
	private final static int INDEX_RANGE = 1000;
	private final static Pattern p = Pattern.compile("-([a-z]{2})[.]");

	private static IndexSearch2 indexSearch = new IndexSearch2(40, 10);

	protected Context context;
	protected String fileName;
	protected String countryCode;

	protected float[][] polygon;
	protected List<POI> poiList;
	protected float[] latitudes; // ordered index of latitudes

	@SuppressWarnings("unused")
	private LocationFile() {
		// nix
	};

	/**
	 * @param fileName
	 * @param context android-related; null otherwise
	 */
	protected LocationFile(String fileName, Context context) {
		Log.d(TAG, "Creating location file with name " + fileName);
		this.fileName = fileName;
		this.context = context;

		// read country code (in form form "osm-parsed-lat-ordered-cz.csv"):
		Matcher m = p.matcher(fileName);
		if (m.find()) {
			countryCode = m.group(1);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("#" + this.getClass().getSimpleName() + ":");
		sb.append("\n fileName: ").append(fileName);
		sb.append("\n countryCode: ").append(countryCode);

		return sb.toString();
	}

	/**
	 * @see https://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
	 * 
	 * @param x latitude in radians!
	 * @param y longitude in radians!
	 * @return true if given coordinates are within this polygon
	 */
	public boolean isInside(float x, float y) {
		if(polygon == null) {
			Log.e(TAG, String.format("Boder polygon for %s not available! WTF?!", fileName));
			return false;
		}
		
		int num = polygon.length;
		int j = num - 1;
		boolean c = false;
		for (int i = 0; i < num; i++) {
			if (((polygon[i][1] > y) != (polygon[j][1] > y))
					&& (x < (polygon[j][0] - polygon[i][0]) * (y - polygon[i][1]) / (polygon[j][1] - polygon[i][1])
							+ polygon[i][0]))
				c = !c;
			j = i;
		}

		return c;
	}

	/**
	 * @param numBytes
	 * @param is
	 * @return decoded integer value
	 */
	protected int readBinaryInt(int numBytes, InputStream is) {
		int i = -1;

		try {
			byte[] bytes = new byte[numBytes];
			is.read(bytes);

			if (numBytes == 2) {
				i = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);

			} else if (numBytes == 4) {
				i = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);

			} else {
				throw new RuntimeException(numBytes + "bytes neumime dekodovat;)");
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return i;
	}

	protected DataInputStream getInputStream() throws IOException {
		DataInputStream dis = null;

		if (context == null) {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		} else {
			dis = new DataInputStream(new BufferedInputStream(context.getAssets().open(fileName)));
		}

		return dis;
	}

	/**
	 * Reads the border polygon from the binary file.
	 */
	protected void readBorderPolygon() {

		try {
			DataInputStream dis = getInputStream();

			// read polygon length (2B):
			int polygonLen = readBinaryInt(2, dis) / 2; // (latitude-longitute tuples)
			// System.out.println("polygonLen=" + polygonLen);

			Log.d(TAG, String.format("Reading border polygon for %s.. length: %s", fileName, polygonLen));
			
			// read the polygon:
			polygon = new float[polygonLen][2];
			for (int i = 0; i < polygonLen; i++) {

				int bits = readBinaryInt(4, dis);
				polygon[i][0] = Float.intBitsToFloat(bits);
				bits = readBinaryInt(4, dis);
				polygon[i][1] = Float.intBitsToFloat(bits);

				// System.out.println(String.format("%s: %.8f %.8f", i, polygon[i][0],
				// polygon[i][1]));
			}

			dis.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @param latitude in radians!
	 * @param longitude in radians!
	 * @param n number of {@link POI}s to be listed
	 * @return N nearest {@link POI}s to given coordinates or an empty list when
	 *         we are out of all our data files.
	 */
	public List<POI> findNearest(float latitude, float longitude, int n) {
		if (poiList == null)
			loadAllPOIs();

		int approxIndex = indexSearch.findIndex(latitude, latitudes);

		int minIndex = approxIndex - INDEX_RANGE;
		minIndex = (minIndex < 0 ? 0 : minIndex);
		int maxIndex = approxIndex + INDEX_RANGE;
		maxIndex = (maxIndex >= latitudes.length ? latitudes.length - 1 : maxIndex);

		POI origin = new POI(latitude, longitude);
		NearestPoints nearestPoints = new NearestPoints(origin, n);

		for (int i = minIndex; i <= maxIndex; i++) {
			nearestPoints.check(poiList.get(i));
		}

		return nearestPoints.getPoints();
	}

	/**
	 * Deletes large data kept by this object;
	 */
	public void discardLargeDataVolume() {
		poiList = null;
		latitudes = null;
		polygon = null;
	}

	/**
	 * Loads all POIs into poisList
	 */
	protected abstract void loadAllPOIs();

}
