package com.ibisek.outlanded.smsReceiver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Sms {

	public static final String ADDRESS = "address";
	public static final String BODY = "body";
	public static final String SEEN = "seen";
	public static final String DATE = "date";
	public static final String TYPE = "type";

	//@see https://stackoverflow.com/questions/8447735/android-sms-type-constants
	public static final Integer MESSAGE_TYPE_ALL    = 0;
	public static final Integer MESSAGE_TYPE_INBOX  = 1;
	public static final Integer MESSAGE_TYPE_SENT   = 2;
	public static final Integer MESSAGE_TYPE_DRAFT  = 3;
	public static final Integer MESSAGE_TYPE_OUTBOX = 4;
	public static final Integer MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
	public static final Integer MESSAGE_TYPE_QUEUED = 6; // for messages to send later
	
	Map<String, String> values = new HashMap<String, String>();

	public String toString() {
		StringBuilder sb = new StringBuilder("#");
		sb.append(this.getClass().getSimpleName()).append(":");

		Iterator<String> keys = values.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			String value = values.get(key);
			sb.append(" \n").append(key).append(": ").append(value);
		}

		return sb.toString();
	}

	public void put(String key, String value) {
		values.put(key, value);
	}

	public String getString(String key) {
		return values.get(key);
	}

	/**
	 * @param key
	 * @return null if there is no such field
	 */
	public Boolean getBool(String key) {
		Boolean retVal = null;
		if (values.containsKey(key)) {
			retVal = Boolean.parseBoolean(values.get(key));
		}

		return retVal;
	}

	/**
	 * @param key
	 * @return null if there is no such field
	 */
	public Long getLong(String key) {
		Long retVal = null;

		if (values.containsKey(key)) {
			retVal = Long.parseLong(values.get(key));
		}

		return retVal;
	}

	/**
 	 * @param key
	 * @return null if there is no such field
	 */
	public Integer getInteger(String key) {
		Integer retVal = null;

		if (values.containsKey(key)) {
			retVal = Integer.parseInt(values.get(key));
		}

		return retVal;
	}

}
