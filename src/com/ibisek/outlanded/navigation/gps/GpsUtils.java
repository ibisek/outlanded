package com.ibisek.outlanded.navigation.gps;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;

import com.ibisek.outlanded.R;

/**
 * POZOR! floor(-12.01) udela -13(!)
 * 
 * @author ibisek
 * @version 2014-07-13
 */
public class GpsUtils {

	public static enum GPS_FORMAT {
		Geocaching, WGS84, RAW
	};

	// (1) raw: N49.123 E16.123
	// (2) Geocaching: N49 12.345 E16 12.345 = N49.20575 E16.20575
	// (3) WGS-84: N49 23'45" E16 23'45" = N49.39583 E16.39583
	private static Pattern PATTERN_RAW = Pattern.compile("([NS])[ ]{0,1}([0-9]+[.,][0-9]+)\\s([EW])[ ]{0,1}([0-9]+[.,][0-9]+)");
	private static Pattern PATTERN_GEOCACHING = Pattern.compile("([NS])[ ]{0,1}([0-9]+)[ °?]{1,2}([0-9]+[.,][0-9]+)['][ ]*([EW])[ ]{0,1}([0-9]+)[ °?]{1,2}([0-9]+[.,][0-9]+)[']");
	private static Pattern PATTERN_WGS84 = Pattern.compile("([NS])[ ]{0,1}([0-9]+)[ °?]{1,2}([0-9]+)[']([0-9]+)[\"]\\s([EW])[ ]{0,1}([0-9]+)[ °?]{1,2}([0-9]+)[']([0-9]+)[\"]");

	// (4) 49.123N 16.123E
	private static Pattern PATTERN_RAW_REVERSED = Pattern.compile("([0-9]+[.,][0-9]+)[ ]{0,1}([NS])\\s{0,1}([0-9]+[.,][0-9]+)[ ]{0,1}([EW])");
	// (5) 49°23.30'N 015°54.38'E
	private static Pattern PATTERN_GEOCACHING_REVERSED = Pattern.compile("([0-9]+)[ °?]{1,2}([0-9]+[.,][0-9]+)[']\\s{0,1}([NS])[ ]*([0-9]+)[ °?]{1,2}([0-9]+[.,][0-9]+)['][ ]{0,1}([EW])");
	// (6) 49 23'45"N 16 23'45"E
	private static Pattern PATTERN_WGS84_REVERSED = Pattern.compile("([0-9]+)[ °?]{1,2}([0-9]+)[']([0-9]+)[\"][ ]{0,1}([NS])\\s{0,1}([0-9]+)[ °?]{1,2}([0-9]+)[']([0-9]+)[\"][ ]{0,1}([EW])");

	// other patterns encountered so far:
	// (6b) 49 23'45N 16 23'45E
	private static Pattern PATTERN_WGS84_BASED_REVERSED_2 = Pattern
			.compile("([0-9]+)[ °?]{1,2}([0-9]+)['’]([0-9]+)[\"]{0,1}[ ]{0,1}([NS])\\s{0,1}([0-9]+)[ °?]{1,2}([0-9]+)['’]([0-9]+)[\"]{0,1}[ ]{0,1}([EW])");

	private String[] DIRECTIONS;

	/**
	 * @param context
	 */
	public GpsUtils(Context context) {
		String directions = context.getResources().getString(R.string.directionMarkers);
		DIRECTIONS = directions.split(",");
	}

	public static String formatToDegMin(double coord) {
		double deg = (int) coord; // get the integer part
		double min = Math.abs((coord - deg) * 60);

		// return String.format("%2.0f°%2.3f'", deg, min);
		return String.format("%2.0f°%s'", Math.abs(deg), new DecimalFormat("00.000").format(min));
	}

	public static String formatToDegMinSec(double coord) {
		double deg = (int) coord; // get the integer part
		double tmpMin = Math.abs((coord - deg) * 60);
		double min = (int) tmpMin;
		double sec = (tmpMin - min) * 60;

		return String.format("%2.0f°%.0f'%.0f\"", Math.abs(deg), min, sec);
	}

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @return [latitude, longitude] in Geocaching format
	 */
	public static String[] formatToGeocaching(double latitude, double longitude) {
		String latLetter = (latitude > 0 ? "N" : "S");
		String longLetter = (longitude > 0 ? "E" : "W");
		String latitudeStr = String.format("%s %s", latLetter, formatToDegMin(latitude));
		String longitudeStr = String.format("%s %s", longLetter, formatToDegMin(longitude));

		return new String[] { latitudeStr, longitudeStr };
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return [latitude, longitude] in WGS84 format
	 */
	public static String[] formatToWGS84(double latitude, double longitude) {
		String latLetter = (latitude > 0 ? "N" : "S");
		String longLetter = (longitude > 0 ? "E" : "W");
		String latitudeStr = String.format("%s %s", latLetter, formatToDegMinSec(latitude));
		String longitudeStr = String.format("%s %s", longLetter, formatToDegMinSec(longitude));

		return new String[] { latitudeStr, longitudeStr };
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return [latitude, longitude] in RAW (decimal) format
	 */
	public static String[] formatToRaw(double latitude, double longitude) {
		String latLetter = (latitude > 0 ? "N" : "S");
		String longLetter = (longitude > 0 ? "E" : "W");
		String latitudeStr = String.format("%s %2.4f", latLetter, Math.abs(latitude));
		String longitudeStr = String.format("%s %2.4f", longLetter, Math.abs(longitude));

		return new String[] { latitudeStr, longitudeStr };
	}

	/**
	 * 
	 * @param format
	 * @param latitude
	 * @param longitude
	 * @return [latitude, longitude] in requested format (RAW by default)
	 */
	public static String[] format(GPS_FORMAT format, double latitude, double longitude) {
		if (format == GPS_FORMAT.Geocaching)
			return formatToGeocaching(latitude, longitude);
		else if (format == GPS_FORMAT.WGS84)
			return formatToWGS84(latitude, longitude);
		else
			return formatToRaw(latitude, longitude);
	}

	/**
	 * @param logitudeLetter E or W
	 * @return +1 for E and -1 for W
	 */
	private static float getLongitudeSign(String logitudeLetter) {
		return "E".equals(logitudeLetter) ? 1f : -1f;
	}

	/**
	 * @param latitudeLetter N or E
	 * @return +1 for N and -1 for S
	 */
	private static float getLatitudeSign(String latitudeLetter) {
		return "N".equals(latitudeLetter) ? 1f : -1f;
	}

	/**
	 * Searches for three formats of GPS coordinates (RAW, Geocaching, WGS-84) in given text. Returns the first found coordinates (lat+lon).
	 * 
	 * @param text
	 * @return array of [latitude, longitude] [in degrees] in case of success, [null, null] otherwise
	 */
	public static Float[] extractGpsCoordinates(String text) {
		if (text.contains("Rasna")) {
			System.out.println("TED!!");
		}

		Float longitude = null, latitude = null;
		text = text.replaceAll(",", ".").toUpperCase(); // just for sure

		// (1)
		Matcher m = PATTERN_RAW.matcher(text);
		if (m.find()) { // raw
			latitude = getLatitudeSign(m.group(1)) * Float.parseFloat(m.group(2));
			longitude = getLongitudeSign(m.group(3)) * Float.parseFloat(m.group(4));
			return new Float[] { latitude, longitude };
		}

		// (2)
		m = PATTERN_GEOCACHING.matcher(text);
		if (m.find()) { // geocaching
			latitude = getLatitudeSign(m.group(1)) * (Float.parseFloat(m.group(2)) + Float.parseFloat(m.group(3)) / 60);
			longitude = getLongitudeSign(m.group(4)) * (Float.parseFloat(m.group(5)) + Float.parseFloat(m.group(6)) / 60);
			return new Float[] { latitude, longitude };
		}

		// (3)
		m = PATTERN_WGS84.matcher(text);
		if (m.find()) { // WGS-84
			latitude = getLatitudeSign(m.group(1)) * (Float.parseFloat(m.group(2)) + Float.parseFloat(m.group(3)) / 60 + Float.parseFloat(m.group(4)) / 60 / 60);
			longitude = getLongitudeSign(m.group(5)) * (Float.parseFloat(m.group(6)) + Float.parseFloat(m.group(7)) / 60 + Float.parseFloat(m.group(8)) / 60 / 60);
			return new Float[] { latitude, longitude };
		}

		// (4)
		m = PATTERN_RAW_REVERSED.matcher(text);
		if (m.find()) { // raw reversed
			latitude = getLatitudeSign(m.group(2)) * Float.parseFloat(m.group(1));
			longitude = getLongitudeSign(m.group(4)) * Float.parseFloat(m.group(3));
			return new Float[] { latitude, longitude };
		}

		// (5)
		m = PATTERN_GEOCACHING_REVERSED.matcher(text);
		if (m.find()) { // geocaching reversed
			latitude = getLatitudeSign(m.group(3)) * (Float.parseFloat(m.group(1)) + Float.parseFloat(m.group(2)) / 60);
			longitude = getLongitudeSign(m.group(6)) * (Float.parseFloat(m.group(4)) + Float.parseFloat(m.group(5)) / 60);
			return new Float[] { latitude, longitude };
		}

		// (6)
		m = PATTERN_WGS84_REVERSED.matcher(text);
		if (m.find()) { // WGS-84 reversed
			latitude = getLatitudeSign(m.group(4)) * (Float.parseFloat(m.group(1)) + Float.parseFloat(m.group(2)) / 60 + Float.parseFloat(m.group(3)) / 60 / 60);
			longitude = getLongitudeSign(m.group(8)) * (Float.parseFloat(m.group(5)) + Float.parseFloat(m.group(6)) / 60 + Float.parseFloat(m.group(7)) / 60 / 60);
			return new Float[] { latitude, longitude };
		}

		// (6b)
		m = PATTERN_WGS84_BASED_REVERSED_2.matcher(text);
		if (m.find()) { // WGS-84-based reversed
			latitude = getLatitudeSign(m.group(4)) * (Float.parseFloat(m.group(1)) + Float.parseFloat(m.group(2)) / 60 + Float.parseFloat(m.group(3)) / 60 / 60);
			longitude = getLongitudeSign(m.group(8)) * (Float.parseFloat(m.group(5)) + Float.parseFloat(m.group(6)) / 60 + Float.parseFloat(m.group(7)) / 60 / 60);
			return new Float[] { latitude, longitude };
		}

		return new Float[] { latitude, longitude };
	}

	public static String bearingToDirectionEN(float bearing) {
		String[] DIRECTIONS = new String[] { "N", "NE", "E", "SE", "E", "S", "SW", "W", "NW" };

		if (bearing > 337)
			bearing -= 337;
		int segment = (int) Math.round((bearing - 22.5) / 45); // deleni po 22.5
		// stupne

		String direction = (segment < DIRECTIONS.length ? DIRECTIONS[segment] : "??");

		return direction;
	}

	public String bearingToDirection(float bearing) {
		if (bearing > 337)
			bearing -= 337;
		int segment = (int) Math.round((bearing - 22.5) / 45); // deleni po 22.5 stupne

		String direction = (segment < DIRECTIONS.length ? DIRECTIONS[segment] : "??");

		return direction;
	}

}
