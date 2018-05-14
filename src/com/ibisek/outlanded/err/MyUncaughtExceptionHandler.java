package com.ibisek.outlanded.err;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.ibisek.outlanded.utils.ExternalStorage;

/**
 * Reports uncaught exceptions via email.
 * 
 * @author ibisek
 * @version 2014-06-02
 * 
 */
public class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private final static String TAG = MyUncaughtExceptionHandler.class.getSimpleName();

	private final static String PATH = "/";
	public final static String FILENAME = "outlanded.error.log";

	private Context context;

	public MyUncaughtExceptionHandler(Context context) {
		this.context = context;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e(TAG, "Caught unexpected exception:", ex);

		String lineSeparator = System.getProperty("line.separator");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		String date = sdf.format(new Date());

		StringBuilder theLog = new StringBuilder();

		theLog.append("Date: ").append(date);
		theLog.append(lineSeparator).append("Device: " + getDeviceName());
		theLog.append(lineSeparator).append("thread name: " + thread.getName());
		theLog.append(lineSeparator).append(ex.toString());

		// log the stack:
		for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
			theLog.append(lineSeparator).append("  ").append(stackTraceElement);
		}
		theLog.append(lineSeparator).append(lineSeparator);

		// dump all available logs from logcat:

		// @see http://www.herongyang.com/Android/Debug-adb-logcat-Command-Argument-Output-Filter.html
		Process mLogcatProc = null;
		BufferedReader reader = null;
		try {
			// mLogcatProc = Runtime.getRuntime().exec(new String[] { "logcat", "-v time", "-d", "AndroidRuntime:V *:V" });
			mLogcatProc = Runtime.getRuntime().exec(new String[] { "logcat", "-d", "*:V" });

			reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				theLog.append(line).append(lineSeparator);
			}

		} catch (IOException e) {
			Log.e(TAG, e.getMessage());

		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage());
				}
		}

		// write the aggregated logs into a file:
		if (ExternalStorage.isExternalStorageAvailable() && ExternalStorage.isExternalStorageWriteable()) {
			ExternalStorage.writeToSDFile(PATH, FILENAME, theLog.toString(), true);

			String message = String.format("Error log written to %s%s", PATH, FILENAME);
			Log.d(TAG, message);
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		}

		// send log to a node0 queue:
		if (true) { // TODO konfigurovatelny parametr v nastaveni
			Intent i = new Intent(context, LogSenderService.class);
			i.putExtra(LogSenderService.LOG_KEY, theLog.toString());
			context.startService(i);
		}

		// show some info about what just happened:
		// TODO toto se nezobrazi.. ale PROC?
		Toast.makeText(context, "err log written to " + FILENAME, Toast.LENGTH_LONG).show();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		//throw new RuntimeException(ex); // re-throw
		System.exit(1);
	}

	private String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}

}
