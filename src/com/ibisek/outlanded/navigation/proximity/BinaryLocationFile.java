package com.ibisek.outlanded.navigation.proximity;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.utils.Configuration;

/**
 * Wraps a single data file with locations. Works with the binary format of
 * data.
 * 
 * <pre>
 * Binary file structure: 
 * [2B polygon length] 
 * [nB polygon lat<float;4B>lon<float;4B> ..]
 * [lat<float;4B>]
 * [lon<float;4b>]
 * [type <byte;1B>]
 * [nameLen<byte;1B>]
 * [name<utf-string>]
 * [size<short;2B>]
 * ..
 * eof
 * </pre>
 * 
 * @see https://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
 * 
 * @author ibisek
 * @version 2013-11-19
 */
public class BinaryLocationFile extends LocationFile {
	public final static String TAG = BinaryLocationFile.class.getSimpleName(); 

	/**
	 * @param fileName
	 */
	public BinaryLocationFile(String fileName) {
		super(fileName, null);

		readBorderPolygon();
	}

	/**
	 * @param fileName
	 * @param context android-related, null otherwise
	 */
	public BinaryLocationFile(String fileName, Context context) {
		super(fileName, context);

		readBorderPolygon();
	}

	/**
	 * Reads all coordinates and creates a list of POIs for all records in the
	 * data file.
	 */
	@Override
	protected void loadAllPOIs() {
		Log.d(TAG, "Loading POIs from " + fileName);
		poiList = new ArrayList<POI>();

		Configuration c = Configuration.getInstance(null);
		boolean displayHabitables = c.displayHabitables();
		boolean displayPeaks = c.displayPeaks();

		try {
			DataInputStream dis = getInputStream();

			// skip polygon bytes:
			int polygonLen = readBinaryInt(2, dis);
			byte[] buf = new byte[polygonLen * 4]; // 4 bytes per float
			dis.readFully(buf);
			buf = null;

			// start reading location records:
			poiList = new ArrayList<POI>();
			while (dis.available() > 0) {
				// (dis.readFloat() did not work):
				float latitude = Float.intBitsToFloat(readBinaryInt(4, dis));
				float longitude = Float.intBitsToFloat(readBinaryInt(4, dis));
				Character.toString((char) 65);
				String type = Character.toString((char) (dis.readByte() & 0xFF));
				int nameLen = dis.readByte() & 0xFF;
				byte[] nameBuf = new byte[nameLen];
				dis.read(nameBuf);
				String name = new String(nameBuf, "UTF-8");
				int size = readBinaryInt(2, dis);
				// System.out.println(String.format("%.8f %.8f %s %s %s", latitude,
				// longitude,
				// type, name, size));

				POI poi = new POI(latitude, longitude, type, name, size);

				if (displayPeaks && poi.getType() == POI.TYPE.PEAK)
					poiList.add(poi);
				else if (displayHabitables && poi.getType() == POI.TYPE.HABITABLE)
					poiList.add(poi);
				else {
					// the list will be empty ;)
				}
			}

			dis.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// fill also the latitudes list for quick index search:
		latitudes = new float[poiList.size()];
		int i = 0;
		for (POI poi : poiList)
			latitudes[i++] = poi.getLatitude();
	}

}
