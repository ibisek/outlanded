package com.ibisek.outlanded.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.ibisek.outlanded.R;

public class SmsSender {

	public enum SMS_SENDER {
		VIA_MANAGER, VIA_INTENT
	};

	private static final String TAG = "SmsSender";

	private static final String SENT = "SMS_SENT";
	private static final String DELIVERED = "SMS_DELIVERED";

	private Context context;
	private PendingIntent sentIntent, deliveryIntent;
	private BroadcastReceiver sentReceiver, deliveredReceiver;

	/**
	 * @param context
	 */
	public SmsSender(Context context) {
		this.context = context;

		sentIntent = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
		sentReceiver = new SentBroadcastReceiver();
		
		deliveryIntent = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);
		deliveredReceiver = new DeliveredBroadcastReceiver();
	}

	/**
	 * Must be called by owing Activity!
	 */
	public void onResume() {
		context.registerReceiver(sentReceiver, new IntentFilter(SENT));
		context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED));
	}
	
	/**
	 * Must be called by owing Activity!
	 */
	public void onPause() {
		context.unregisterReceiver(sentReceiver);
		context.unregisterReceiver(deliveredReceiver);
	}
	
	/**
	 * @param phoneNumber
	 * @param message
	 */
	public void sendSMS(String phoneNumber, String message) {
		Log.d(TAG, String.format("sendSMS(%s, %s)", phoneNumber, message));
		
		Configuration c = Configuration.getInstance(context);
		if (c != null) {
			switch (c.getSmsSender()) {

			case VIA_MANAGER:
				sendSmsViaManager(phoneNumber, message);
				break;

			case VIA_INTENT:
				sendSmsViaIntent(phoneNumber, message);
				break;

			default:
				Toast.makeText(context, "NO SMS SENDER SELECTED. This should never happen.", Toast.LENGTH_LONG).show();
				break;
			}
		}

	}

	/**
	 * Sends an SMS viam SmsManager with registered sent- and delivery- intents.
	 * 
	 * @param phoneNumber
	 * @param message
	 */
	private void sendSmsViaManager(String phoneNumber, String message) {
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);
	}

	// private void sendSmsViaManager(String phoneNumber, String message) {
	// Intent intentSent = new Intent(MainActivity.this, MsgSentActivity.class);
	// Intent intentDelivered = new Intent(MainActivity.this,
	// MsgDeliveredActivity.class);
	//
	// PendingIntent sentIntent = PendingIntent.getActivity(MainActivity.this, 0,
	// intentSent, 0);
	// PendingIntent deliveryIntent = PendingIntent.getActivity(MainActivity.this,
	// 0, intentDelivered, 0);
	//
	// SmsManager smsManager = SmsManager.getDefault();
	// smsManager.sendTextMessage(phoneNumber, null, message, sentIntent,
	// deliveryIntent);
	// }

	private void sendSmsViaIntent(String phoneNumber, String message) {
		Intent smsIntent = new Intent(Intent.ACTION_VIEW);

		smsIntent.putExtra("sms_body", message);
		smsIntent.putExtra("address", phoneNumber);
		smsIntent.setType("vnd.android-dir/mms-sms");

		context.startActivity(smsIntent);
	}

	private class SentBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d("", "sentBroadcasetReceiver.intent.action=" + action);

			int resultCode = getResultCode();
			switch (resultCode) {
			case Activity.RESULT_OK:
				Toast.makeText(context, context.getString(R.string.sms_sent), Toast.LENGTH_LONG).show();
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				Toast.makeText(context, "Generic failure", Toast.LENGTH_LONG).show();
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				Toast.makeText(context, "No service", Toast.LENGTH_LONG).show();
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				Toast.makeText(context, "Null PDU", Toast.LENGTH_LONG).show();
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				Toast.makeText(context, "Radio off", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}

	private class DeliveredBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d("", "deliveredBroadcasetReceiver.intent.action=" + action);

			Toast.makeText(context, context.getString(R.string.sms_delivered), Toast.LENGTH_LONG).show();

			// final String PDU_FIELD_NAME = "pdu";
			// Bundle receiveBundle = ((Activity) context).getIntent().getExtras();
			// if (receiveBundle != null && receiveBundle.containsKey(PDU_FIELD_NAME))
			// {
			// int pduVal = receiveBundle.getInt(PDU_FIELD_NAME);
			// Toast.makeText(context, "pduVal=" + pduVal, Toast.LENGTH_LONG).show();
			// } else {
			// Toast.makeText(context, "no pdu available", Toast.LENGTH_LONG).show();
			// }
		}
	}

}
