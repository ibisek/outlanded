package com.ibisek.outlanded.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

/**
 * Currently used for saving exception dump log when application crashes.
 * 
 * @see https://stackoverflow.com/questions/8330276/write-a-file-in-external-storage-in-android
 * @see http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
 * 
 * @author internet
 */
public class ExternalStorage {

	private final static String TAG = ExternalStorage.class.getSimpleName();

	private static boolean externalStorageAvailable = false;
	private static boolean externalStorageWriteable = false;

	static {
		checkExternalMedia();
	}

	public static boolean isExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	public static boolean isExternalStorageWriteable() {
		return externalStorageWriteable;
	}

	/**
	 * Check whether external media available and writable.
	 */
	private static void checkExternalMedia() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// Can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// Can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Can't read or write
			externalStorageAvailable = externalStorageWriteable = false;
		}

		Log.d(TAG, "External Media: readable=" + externalStorageAvailable + " writable=" + externalStorageWriteable);
	}

	/**
	 * @param path
	 * @param filename
	 * @param message
	 * @param append
	 */
	public static void writeToSDFile(String path, String filename, String message, boolean append) {

		File root = android.os.Environment.getExternalStorageDirectory();
		Log.d(TAG, "External file system root: " + root);

		// See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

		File dir = new File(root.getAbsolutePath() + path);
		if (!dir.exists())
			dir.mkdirs();

		try {
			File file = new File(dir, filename);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
			writer.append(message);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException ex) {
			Log.e(TAG, "Error when writing to file: " + ex.getMessage());
		} catch (IOException ex) {
			Log.e(TAG, ex.getMessage());
		}
	}

}
