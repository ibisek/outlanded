package com.ibisek.outlanded.utils;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.util.Base64;
import android.util.Log;

import com.ibisek.outlanded.net.HttpResponseHandler;
import com.ibisek.outlanded.net.MyHttpClient;
import com.ibisek.outlanded.net.ParsedHttpResponse;

public class CompetitionModeLocationSender implements HttpResponseHandler {

	private final static String TAG = CompetitionModeLocationSender.class.getSimpleName();

	/**
	 * Shares landing location on the server.
	 * 
	 * @param competitionNo
	 * @param registrationNo
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @param nearestPoi
	 */
	public void sendToServer(String competitionNo, String registrationNo, double latitude, double longitude, String nearestPoi) {
		if (competitionNo != null && !"".equals(competitionNo)) {
			// format GPS position:
			String lat = String.format("%.4f", latitude);
			String lon = String.format("%.4f", longitude);

			// message format: "AF;OK-9000;timestamp-in-seconds;latitude;longitude"
			String message = String.format("%s;%s;%s;%s;%s;%s", competitionNo, registrationNo, new Date().getTime() / 1000, lat, lon, nearestPoi);
			Log.d(TAG, "Message to node0 queue: " + message);

			Map<String, String> params = new HashMap<String, String>();
			params.put("topic", Configuration.getNode0Topic());
			params.put("message", Base64.encodeToString(message.getBytes(Charset.forName("utf-8")), Base64.DEFAULT));
			params.put("msgType", "REGULAR");
			new MyHttpClient().doPost(Configuration.getOutlandedQueueUrl(), params, this);
		}
	}

	@Override
	public Object handleResponse(ParsedHttpResponse httpResponse) {
		if (httpResponse != null && httpResponse.getStatus() != 200) {
			// nix
		}

		return null;
	}

}
