package com.ibisek.outlanded.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;

import com.ibisek.outlanded.navigation.gps.GpsUtils.GPS_FORMAT;
import com.ibisek.outlanded.net.UpdateQuery;
import com.ibisek.outlanded.phonebook.PhonebookEntry;
import com.ibisek.outlanded.storage.InternalStorage;
import com.ibisek.outlanded.utils.SmsSender.SMS_SENDER;

public class Configuration {

	private static final String TAG = Configuration.class.getName();

	private static final String CONFIGURATION_FILENAME = "configuration.bin";

	private static String PROP_GPS_FORMAT = "gpsFormat";
	private static String PROP_SMS_SENDER = "smsSender";
	private static String PROP_NUM_PROXIMITY_ITEMS = "numProximityItems";
	private static String PROP_DISPLAY_HABITABLES = "displayHabitables";
	private static String PROP_DISPLAY_PEAKS = "displayPeaks";
	private static String PROP_LAST_UPDATE_CHECK_DATE = "lastUpdateCheckDate";
	private static String PROP_ON_SERVER_VERSION = "onServerVersion";
	private static String PROP_SMS_FILTERING_ENABLED = "smsFilteringEnabled";
	private static String PROP_LOCATION_SHARING_ENABLED = "locationSharingEnabled";
	private static String PROP_COMPETITION_NO = "competitionNo";
	private static String PROP_REGISTRATION_NO = "registrationNo";
	

	private final static String NULL_VALUE = "NULL";

	// a reasonable default value:
	public final static int DEFAULT_NUM_PROXIMITY_ITEMS = 20;

	// last recently used contacts:
	private static String LRU_CONTACTS = "lruContacts";
	private static int LRU_LIST_MAX_LEN = 10;

	private static Configuration instance;

	private InternalStorage<Properties> storage;
	private Properties properties;

	private boolean dirty;

	/**
	 * @param context
	 */
	private Configuration(Context context) {
		Log.d(TAG, "CONFIGURATION_FILENAME: " + CONFIGURATION_FILENAME);
		Log.d(TAG, "LRU_LIST_MAX_LEN: " + LRU_LIST_MAX_LEN);

		storage = new InternalStorage<Properties>(CONFIGURATION_FILENAME, context);

		properties = storage.load();
		if (properties == null)
			properties = createDefaultProperties();
	}

	/**
	 * @param context
	 *          needs to be created just once, then can be called with null. It can be also given by calling getApplicationContext().
	 * @return instance of {@link Configuration} or null in case given context is null and getInstace has been called for the first time
	 */
	public static Configuration getInstance(Context context) {
		if (instance == null && context == null)
			return null;

		if (instance == null)
			instance = new Configuration(context);

		return instance;
	}

	/**
	 * @return default set of properties
	 */
	private Properties createDefaultProperties() {
		Properties props = new Properties();
		props.setProperty(PROP_GPS_FORMAT, GPS_FORMAT.Geocaching.toString());
		props.setProperty(PROP_SMS_SENDER, SMS_SENDER.VIA_INTENT.toString());

		return props;
	}

	/**
	 * Saves configuration into local storage.
	 */
	public void save() {
		if (dirty)
			storage.save(properties);
	}

	/**
	 * @return by user selected GPS format
	 */
	public GPS_FORMAT getGpsFormat() {
		return GPS_FORMAT.valueOf(properties.getProperty(PROP_GPS_FORMAT));
	}

	public void setGpsFormat(GPS_FORMAT gpsFormat) {
		properties.setProperty(PROP_GPS_FORMAT, gpsFormat.toString());
		dirty = true;
	}

	/**
	 * @return by user/default selected {@link SMS_SENDER}
	 */
	public SMS_SENDER getSmsSender() {
		return SMS_SENDER.valueOf(properties.getProperty(PROP_SMS_SENDER));
	}

	public void setSmsSender(SMS_SENDER smsSender) {
		properties.setProperty(PROP_SMS_SENDER, smsSender.toString());
		dirty = true;
	}

	private static final String CSV_SEPARATOR = "|";

	/**
	 * @return list of Recently Used Contacts for the phone book
	 */
	public List<PhonebookEntry> getLastRecentlyUsedContacts() {
		List<PhonebookEntry> list = new ArrayList<PhonebookEntry>();

		if (properties.containsKey(LRU_CONTACTS)) {
			String csv = properties.getProperty(LRU_CONTACTS);

			StringTokenizer st = new StringTokenizer(csv, CSV_SEPARATOR);
			while (st.hasMoreElements()) {
				String contactName = st.nextToken();
				
				String phoneNumber = st.nextToken();
				if(NULL_VALUE.equals(phoneNumber))
					phoneNumber = null;
				
				String email = st.nextToken();
				if (NULL_VALUE.equals(email))
					email = null;
				
				list.add(new PhonebookEntry(contactName, phoneNumber, email));
			}
		}

		return list;
	}

	public void saveLastRecentlyUsedContacts(List<PhonebookEntry> lastRecentlyUsedContacts) {
		if (lastRecentlyUsedContacts.size() > 0) {
			StringBuilder csv = new StringBuilder();

			int i = 0;
			for (PhonebookEntry entry : lastRecentlyUsedContacts) {
				csv.append(entry.getContactName());
				csv.append(CSV_SEPARATOR);

				csv.append((entry.getPhoneNumber() != null ? entry.getPhoneNumber() : NULL_VALUE));
				csv.append(CSV_SEPARATOR);

				csv.append((entry.getEmail() != null ? entry.getEmail() : NULL_VALUE));
				csv.append(CSV_SEPARATOR);

				if (++i == LRU_LIST_MAX_LEN)
					break; // store no more entries than MAX
			}

			properties.put(LRU_CONTACTS, csv.toString());
			dirty = true;
		}
	}

	/**
	 * @return number if items in the proximity display list
	 */
	public int getNumProximityItems() {
		if (!properties.containsKey(PROP_NUM_PROXIMITY_ITEMS)) {
			setNumProximityItems(DEFAULT_NUM_PROXIMITY_ITEMS);
		}

		return Integer.parseInt(properties.getProperty(PROP_NUM_PROXIMITY_ITEMS));
	}

	/**
	 * @param num
	 *          number of items in the proximity display list
	 */
	public void setNumProximityItems(int num) {
		properties.setProperty(PROP_NUM_PROXIMITY_ITEMS, String.valueOf(num));
		dirty = true;
	}

	/**
	 * @return true if habitable places should be displayed on the proximity display list
	 */
	public boolean displayHabitables() {
		if (!properties.containsKey(PROP_DISPLAY_HABITABLES)) {
			setDisplayHabitables(true); // yes by default
		}

		return Boolean.parseBoolean(properties.getProperty(PROP_DISPLAY_HABITABLES));
	}

	/**
	 * @return true if peaks should be displayed on the proximity display list
	 */
	public boolean displayPeaks() {
		if (!properties.containsKey(PROP_DISPLAY_PEAKS)) {
			setDisplayPeaks(true); // yes by default
		}

		return Boolean.parseBoolean(properties.getProperty(PROP_DISPLAY_PEAKS));
	}

	/**
	 * @param doDisplay
	 */
	public void setDisplayHabitables(boolean doDisplay) {
		properties.setProperty(PROP_DISPLAY_HABITABLES, String.valueOf(doDisplay));
		dirty = true;
	}

	/**
	 * @param doDisplay
	 */
	public void setDisplayPeaks(boolean doDisplay) {
		properties.setProperty(PROP_DISPLAY_PEAKS, String.valueOf(doDisplay));
		dirty = true;
	}

	/**
	 * @param date
	 *          when last update check was performed
	 */
	public void setLastUpdateCheckDate(Date date) {
		properties.setProperty(PROP_LAST_UPDATE_CHECK_DATE, String.valueOf(date.getTime()));
		dirty = true;
	}

	/**
	 * @return date when updates were checked for the last time
	 */
	public Date getLastUpdateCheckDate() {
		if (properties.containsKey(PROP_LAST_UPDATE_CHECK_DATE)) {
			long millis = Long.valueOf((String) properties.get(PROP_LAST_UPDATE_CHECK_DATE));
			return new Date(millis);
		}

		return null;
	}

	/**
	 * @param onServerVersion
	 *          current version of application obtained from the {@link UpdateQuery} result
	 */
	public void setOnServerVersion(String onServerVersion) {
		properties.put(PROP_ON_SERVER_VERSION, onServerVersion);
		dirty = true;
	}

	/**
	 * @return last known app version obtained from the server via {@link UpdateQuery}.
	 */
	public String getOnServerVersion() {
		if (properties.containsKey(PROP_ON_SERVER_VERSION)) {
			return (String) properties.get(PROP_ON_SERVER_VERSION);
		}

		return null;
	}

	/**
	 * @return false if the SMS should not be filtered, true otherwise or even when the preference is not set
	 */
	public boolean isSmsFilteringEnabled() {
		if (properties.containsKey(PROP_SMS_FILTERING_ENABLED)) {
			return Boolean.parseBoolean(properties.getProperty(PROP_SMS_FILTERING_ENABLED));
		}

		return true;
	}

	/**
	 * @param enabled
	 *          false to disable incoming SMS filtering
	 */
	public void setSmsFilteringEnabled(boolean enabled) {
		properties.put(PROP_SMS_FILTERING_ENABLED, Boolean.toString(enabled));
		dirty = true;
	}

	/**
	 * @return true if location sharing is enabled
	 */
	public boolean isLocationSharingEnabled() {
		if (properties.containsKey(PROP_LOCATION_SHARING_ENABLED)) {
			return Boolean.parseBoolean(properties.getProperty(PROP_LOCATION_SHARING_ENABLED));
		}

		return false; // do not share by default
	}
	
	/**
	 * @param enabled
	 */
	public void setLocationSharingEnabled(boolean enabled) {
		properties.put(PROP_LOCATION_SHARING_ENABLED, Boolean.toString(enabled));
		dirty = true;
	}
	
	/**
	 * @return registration number, e.g. OK-9000
	 */
	public String getRegistrationNo() {
		if (properties.containsKey(PROP_REGISTRATION_NO)) {
			return properties.getProperty(PROP_REGISTRATION_NO);
		}

		return null;
	}
	
	/**
	 * @param registrationNo
	 */
	public void setRegistrationNo(String registrationNo) {
		properties.put(PROP_REGISTRATION_NO, registrationNo);
		dirty = true;
	}
	
	/**
	 * @return competition number, e.g. AF
	 */
	public String getCompetitionNo() {
		if (properties.containsKey(PROP_COMPETITION_NO)) {
			return properties.getProperty(PROP_COMPETITION_NO);
		}

		return null;
	}
	
	/**
	 * @param competitionNo
	 */
	public void setCompetitionNo(String competitionNo) {
		properties.put(PROP_COMPETITION_NO, competitionNo);
		dirty = true;
	}
	
	/* STATIC STUFF */
	
	/**
	 * @return URL of the 'outlanded' message queue
	 */
	public static String getOutlandedQueueUrl() {
		return "http://nodenula.appspot.com/sendMessage";
	}
	
	/**
	 * @return node0 topic name 
	 */
	public static String getNode0Topic() {
		return "outlanded";
	}

	/**
	 * @return URL where to obtain version of the most up-to-date release
	 */
	public static String getVersionUrl() {
		return "http://www.ibisek.com/data/outlanded.version";
	}

	/**
	 * @return URL where to download new release
	 */
	public static String getDownloadUrl() {
		return "http://www.ibisek.com/outlanded/download";
	}

	/**
	 * Needs to be updated manually.
	 * 
	 * @return application version
	 */
	public static String getAppVersion() {
		return "2016-02-02";
	}

}
