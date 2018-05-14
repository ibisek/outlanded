package com.ibisek.outlanded.err;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.ibisek.outlanded.net.HttpResponseHandler;
import com.ibisek.outlanded.net.MyHttpClient;
import com.ibisek.outlanded.net.ParsedHttpResponse;
import com.ibisek.outlanded.utils.ZipUtils;

/**
 * Sends the log to a node0 queue.
 */
public class LogSenderService extends Service {

	private final static String TAG = LogSenderService.class.getSimpleName();
	public final static String LOG_KEY = "LOG";

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		Toast.makeText(this, "Error has been reported.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getExtras().containsKey(LOG_KEY)) {
			String theLog = intent.getExtras().getString(LOG_KEY);

			String url = "http://nodenula.appspot.com/sendMessage";
			Map<String, String> postParams = new HashMap<String, String>();
			postParams.put("topic", "outlanded");
			postParams.put("msgType", "REGULAR");

			byte[] zippedMessage = ZipUtils.zip(MyUncaughtExceptionHandler.FILENAME, theLog);
			String message = Base64.encodeToString(zippedMessage, Base64.DEFAULT);
			postParams.put("message", message);

			MyHttpClient httpClient = new MyHttpClient();
			httpClient.doPost(url, postParams, new HttpResponseHandler() {
				@Override
				public Object handleResponse(ParsedHttpResponse httpResponse) {
					Log.d(TAG, String.format("Response status: %s; data: %s", httpResponse.getStatus(), httpResponse.getData()));
					stopSelf(); // stops the service
					return null;
				}
			});

			// We want this service to continue running until it is explicitly stopped, so return sticky:
			return START_STICKY;

		} else {
			stopSelf();
			return START_NOT_STICKY;
		}
	}

}
