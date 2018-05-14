package com.ibisek.outlanded.smsReceiver;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsMessage;
import android.util.Log;

import com.ibisek.outlanded.MainActivity;
import com.ibisek.outlanded.R;
import com.ibisek.outlanded.SmsListActivity;
import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.gps.GpsUtils;
import com.ibisek.outlanded.navigation.proximity.ProximityListener;
import com.ibisek.outlanded.navigation.proximity.ProximitySource;
import com.ibisek.outlanded.phonebook.PhonebookUtils;
import com.ibisek.outlanded.utils.CompetitionModeLocationSender;
import com.ibisek.outlanded.utils.Configuration;

/**
 * Filters all incoming SMS messages and searches for GPS coordinates in RAW, Geocaching and WGS-84 formats.
 * 
 * If such coordinate is found, a notification is created, which later on opens list with received messages and lets the user select one and click 'Navigate'
 * button to open external navigation application.
 * 
 * Additionally, if location sharing is enabled, it posts the incoming location to server.
 * 
 * 
 * @see http://androidexample.com/Incomming_SMS_Broadcast_Receiver_-_Android_Example/index.php?view=article_discription&aid=62&aaid=87
 * 
 * @author ibisek
 * @version 2014-05-20
 */
public class SmsReceiver extends BroadcastReceiver {

	private static final String TAG = SmsReceiver.class.getSimpleName();
	public static final int SMS_NOTIFICATION_ID = 666;

	private static Pattern PATTERN_COMPETITION_NO = Pattern.compile("([A-Z]{1,3})[ ]");
	private static Pattern PATTERN_REGISTRATION_NO = Pattern.compile("([A-Z]+[-][0-9]+)");

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "SMS RECIVER ACTION: " + intent.getAction());

		// check if SMS reception is enabled:
		if (!Configuration.getInstance(context).isSmsFilteringEnabled()) {
			Log.d(TAG, "SMS reception not allowed.");
			return;
		}

		try {
			final Bundle bundle = intent.getExtras();

			if (bundle != null) {
				String senderNumber, senderName = null;
				
				// aggregate all the message parts to one string:
				StringBuilder message = new StringBuilder();
				final Object[] pdusObj = (Object[]) bundle.get("pdus");
				for (int i = 0; i < pdusObj.length; i++) {
					SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
					String messagePart = currentMessage.getDisplayMessageBody();
					message.append(messagePart);
					
					// lookup the sender's name in the phonebook:
					senderNumber = currentMessage.getDisplayOriginatingAddress();
					senderName = PhonebookUtils.findDisplayName(context, senderNumber);
					if (senderName == null)
						senderName = senderNumber;
					
					Log.d(TAG, String.format("PDU info: senderName: %s; senderNumber:%s; messagePart:%s", senderName, senderNumber, messagePart));
				}
				
				if(message.toString().length() > 0) {
					Log.d(TAG, "Incoming SMS: " + message);

					// N49.390856 E16.099912 na hristi u krizanova
					// message = "aaa N49.390856 E16.099912 bbb";

					Float[] coords = GpsUtils.extractGpsCoordinates(message.toString());

					// if there are coordinates in the SMS message:
					if (coords[0] != null && coords[1] != null) {
						Log.d(TAG, String.format("GPS coords: %.4f %.4f", coords[0], coords[1]));

						// create BASIC notification in the upper area:
						setNotification(context, context.getString(R.string.app_name), senderName, R.drawable.outlanded);
						// .. and if the sender is within recipient's polygon area a better notification will be created by the proximity listener..

						Float latitude = (float) Math.toRadians(coords[0]);
						Float longitude = (float) Math.toRadians(coords[1]);

						// try to find competition and registration numbers:
						String competitionNo = null, registrationNo = null;
						Matcher m = PATTERN_COMPETITION_NO.matcher(message);
						if (m.find())
							competitionNo = m.group(1);
						m = PATTERN_REGISTRATION_NO.matcher(message);
						if (m.find())
							registrationNo = m.group();

						// find the location in on proximity display:
						// new Thread(new ProximityQueryStarter(context, senderName, latitude, longitude, competitionNo, registrationNo)).start();
						new ProximityQueryAsyncTask(context, senderName, latitude, longitude, competitionNo, registrationNo).execute();

						// start beeping service:
						Intent beepingServiceIntent = new Intent(context, BeepingService.class);
						context.startService(beepingServiceIntent);

					} // else ignore the message
				}
			}

		} catch (Exception ex) {
			if (ex != null) { // uz se stalo ze ex.getMessage() udelalo NPE(!!)
				StringBuilder sb = new StringBuilder(ex.getMessage());
				StackTraceElement[] items = ex.getStackTrace();
				for (StackTraceElement item : items) {
					sb.append("\n").append(item);
				}

				Log.e(TAG, sb.toString());
			}
		}
	}

	/**
	 * Adds a notification icon & message to the notification area.
	 * 
	 * @param title
	 * @param text
	 * @param icon R.drawable..
	 * 
	 * @see https ://developer.android.com/guide/topics/ui/notifiers/notifications .html #CreateNotification
	 */
	private void setNotification(Context context, String title, String text, int icon) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(icon).setContentTitle(title).setContentText(text);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(context, SmsListActivity.class);

		// The stack builder object will contain an artificial back stack for
		// the started Activity. This ensures that navigating backward from the
		// Activity leads out of your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(SMS_NOTIFICATION_ID, mBuilder.build());
	}

	/**
	 * Cancels our notification in the upper screen area.
	 * 
	 * @param context
	 */
	public static void cancelNotification(Context context) {
		if (Context.NOTIFICATION_SERVICE != null) {
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(ns);
			notificationManager.cancel(SMS_NOTIFICATION_ID);
		}
	}

	private class ProximityQueryAsyncTask extends AsyncTask<Void, Void, String> {

		private Context context;
		private String senderName;
		private Float latitude, longitude;
		private String competitionNo, registrationNo;

		/**
		 * @param context
		 * @param senderName
		 * @param latitude
		 * @param longitude
		 * @param competitionNo
		 * @param registrationNo
		 */
		public ProximityQueryAsyncTask(Context context, String senderName, Float latitude, Float longitude, String competitionNo, String registrationNo) {
			this.context = context;
			this.senderName = senderName;
			this.latitude = latitude;
			this.longitude = longitude;
			this.competitionNo = competitionNo;
			this.registrationNo = registrationNo;
		}

		@Override
		protected String doInBackground(Void... params) {
			ProximitySource proximitySource = ProximitySource.getInstance();
			if (!proximitySource.isInitialised())
				proximitySource.init(context, 4, 1); // 4m, 1point

			//proximitySource.addListener(new MyProximityListener(context, senderName, latitude, longitude, competitionNo, registrationNo));
			while (!proximitySource.isInitialised()) { // give it some time
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// don't care
				}
			}
			
			proximitySource.resolveLocation(latitude, longitude, new MyProximityListener(context, senderName, latitude, longitude, competitionNo, registrationNo));

			return null;
		}
	}

	// /**
	// * Needs to be done in such complicated way as we need to wait until the ProximitySource is initialised and that takes time.. while we cannot do
	// * Thread.sleep() on the main app thread.
	// */
	// private class ProximityQueryStarter implements Runnable {
	//
	// private Context context;
	// private String senderName;
	// private Float latitude, longitude;
	// private String competitionNo, registrationNo;
	//
	// /**
	// * @param context
	// * @param senderName
	// * @param latitude in radians
	// * @param longitude in radians
	// * @param registrationNo
	// * @param competitionNo
	// */
	// public ProximityQueryStarter(Context context, String senderName, Float latitude, Float longitude, String competitionNo, String registrationNo) {
	// this.context = context;
	// this.senderName = senderName;
	// this.latitude = latitude;
	// this.longitude = longitude;
	// this.competitionNo = competitionNo;
	// this.registrationNo = registrationNo;
	// }
	//
	// @Override
	// public void run() {
	// ProximitySource proximitySource = ProximitySource.getInstance();
	// proximitySource.init(context, 4, 1); // 4m, 1point
	// proximitySource.addListener(new MyProximityListener(context, senderName, latitude, longitude, competitionNo, registrationNo));
	// while (!proximitySource.isInitialised()) { // give it some time
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException e) {
	// // don't care
	// }
	// }
	// proximitySource.updateLocation(latitude, longitude);
	// }
	// }

	private class MyProximityListener implements ProximityListener {

		private Context context;
		private String senderName;
		private float latitude, longitude;
		private String competitionNo, registrationNo;

		public MyProximityListener(Context context, String senderName, float latitude, float longitude, String competitionNo, String registrationNo) {
			this.context = context;
			this.senderName = senderName;
			this.latitude = latitude;
			this.longitude = longitude;
			this.competitionNo = competitionNo;
			this.registrationNo = registrationNo;
		}

		@Override
		public void notify(List<POI> nearestPoints) {
			POI nearestPoi = nearestPoints.get(0);

			// get direction to nearest point:
			String name = nearestPoi.getName();
			float bearing = nearestPoi.getBearingToOrigin();
			String direction = new GpsUtils(context).bearingToDirection(bearing);

			// format the "2km SW from XX" direction string:
			String format = context.getResources().getString(R.string.locationFormatSMS);
			String directionString = String.format(format, nearestPoi.getDistanceToOrigin(), direction, name);

			// create the notification in the upper area:
			String title = String.format("%s: %s", context.getString(R.string.app_name), senderName);
			String notificationMessage = String.format("%s", directionString);
			setNotification(context, title, notificationMessage, R.drawable.outlanded);

			// share incoming location:
			if (Configuration.getInstance(context).isLocationSharingEnabled()) {
				new CompetitionModeLocationSender().sendToServer(competitionNo, registrationNo, Math.toDegrees(latitude), Math.toDegrees(longitude), nearestPoi.getName());
			}
		}
	}

}
