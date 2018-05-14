package com.ibisek.outlanded.smsReceiver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.ibisek.outlanded.R;

/**
 * @see https://developer.android.com/guide/topics/media/mediaplayer.html
 */
public class BeepingService extends Service implements Runnable {

	private final static String TAG = BeepingService.class.getSimpleName();

	private MediaPlayer mediaPlayer;
	private Vibrator vibrator;
	private boolean running = false;

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		// nix
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		Log.d(TAG, "onStartCommand");

		if (!running) {
			mediaPlayer = MediaPlayer.create(this, R.raw.two_bubbley);
			vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

			running = true;
			new Thread(this).start();
		}

		return startid;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		if (mediaPlayer != null) {
			running = false;
			mediaPlayer.stop();
			mediaPlayer.release();
		}
	}

	@Override
	public void run() {
		while (running) {
			if (mediaPlayer != null)
				mediaPlayer.start();

			if (vibrator != null)
				vibrator.vibrate(500);

			pause(4000);
		}

		stopSelf();
	}

	private void pause(long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

}
