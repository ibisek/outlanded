package com.ibisek.outlanded.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class IOUtils {

	private final static String TAG = IOUtils.class.getSimpleName();

	/**
	 * Reads fully data from given inputStream.
	 * 
	 * @param inputStream
	 * @return String read from the input stream
	 */
	public static String readFromStream(InputStream inputStream) {
		BufferedReader reader = null;
		StringBuffer data = new StringBuffer();
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = "";
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}

		} catch (IOException e) {
			Log.e(TAG, "IOException when reading data from stream.");

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// no action
				}
			}
		}
		return data.toString();
	}

}
