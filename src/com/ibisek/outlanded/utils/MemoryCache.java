package com.ibisek.outlanded.utils;

import android.util.Log;

import com.ibisek.outlanded.navigation.POI;

/**
 * Something like Configuration, just not persistent. Intended to information
 * across Activity transition.. data were getting lost from components when
 * Activity was re-entered (and re-instantiated).
 * 
 * Hopefully this will keep the data more reliably :)
 * 
 * @author ibisek
 */
public class MemoryCache {

	private static final String TAG = MemoryCache.class.getName();

	private static MemoryCache instance;

	private POI selectedPoi;
	private String phoneNumber;
	private String selectedEmail;

	/**
	 * @param context
	 */
	private MemoryCache() {
		Log.d(TAG, "MemoryCache CONSTRTUCTOR");
	}

	public static MemoryCache getInstance() {
		if (instance == null)
			instance = new MemoryCache();

		return instance;
	}

	public void setSelectedPoi(POI poi) {
		this.selectedPoi = poi;
	}

	public POI getSelectedPoi() {
		return selectedPoi;
	}

	public void setSelectedPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getSelectedPhoneNumber() {
		return phoneNumber;
	}

	public void setSelectedEmail(String selectedEmail) {
		this.selectedEmail = selectedEmail;
	}

	public String getSelectedEmail() {
		return selectedEmail;
	}

}
