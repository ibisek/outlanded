package com.ibisek.outlanded.net;

import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpHost;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.ibisek.outlanded.R;
import com.ibisek.outlanded.components.AlertDialogFragment;
import com.ibisek.outlanded.components.ProgressDialogFragment;
import com.ibisek.outlanded.utils.Configuration;

public class UpdateQuery extends AsyncTask<Void, Void, ParsedHttpResponse> {

	private final static String TAG = UpdateQuery.class.getSimpleName();

	private FragmentActivity fragmentActivity;
	private String versionUrl;
	private String currentVersion;
	private MyDownloadDialogOnClickListener dialogOnClickListener;

	private AndroidHttpClient httpClient;
	private DialogFragment busyDialog;
	private boolean backgroundUpdateCheck = false;

	/**
	 * @param fragmentActivity
	 * @param versionUrl
	 * @param currentVersion
	 */
	public UpdateQuery(FragmentActivity fragmentActivity, String versionUrl, String currentVersion) {
		this.fragmentActivity = fragmentActivity;
		this.versionUrl = versionUrl;
		this.currentVersion = currentVersion;

		dialogOnClickListener = new MyDownloadDialogOnClickListener();
	}

	/**
	 * Shows only long {@link Toast}, not a dialog (used by automatic update check).
	 */
	public void performBackgroundUpdateCheckOnly(boolean doBackgroundUpdateCheck) {
		backgroundUpdateCheck = doBackgroundUpdateCheck;
	}

	@Override
	protected ParsedHttpResponse doInBackground(Void... params) {

		// show busy dialog:
		if (!backgroundUpdateCheck) {
			busyDialog = ProgressDialogFragment.newInstance(fragmentActivity.getString(R.string.update_busy_dialog_text));
			busyDialog.show(fragmentActivity.getSupportFragmentManager(), "busyDialog");
		}

		try {
			ResponseHandler<ParsedHttpResponse> responseHandler = new MyHttpResponseHandler();

			httpClient = AndroidHttpClient.newInstance("");

			// set HTTP proxy (if needed):
			if (MyHttpClient.USE_HTTP_PROXY) {
				HttpHost proxy = new HttpHost(MyHttpClient.HTTP_PROXY_HOST, MyHttpClient.HTTP_PROXY_PORT);
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}

			HttpGet req = new HttpGet(versionUrl);
			return httpClient.execute(req, responseHandler);

		} catch (IOException ex) {
			Log.e(TAG, String.format("Cannot connect to '%s', but that' OK when inet down. Err msg: %s", versionUrl, ex.getMessage()));
		}

		return null;
	}

	@Override
	protected void onPostExecute(ParsedHttpResponse response) {
		if (httpClient != null)
			httpClient.close();

		if (backgroundUpdateCheck) {
			if (response != null && response.getStatus() == 200) {
				String onServerVersion = response.getData().trim();
				Configuration.getInstance(null).setLastUpdateCheckDate(new Date());
				Configuration.getInstance(null).setOnServerVersion(onServerVersion);
			}

		} else {
			busyDialog.dismiss();

			if (response == null || response.getStatus() != 200) { // error vole!
				String message = fragmentActivity.getString(R.string.update_failed);
				String positiveLabel = fragmentActivity.getString(R.string.btn_ok);
				DialogFragment dialog = AlertDialogFragment.newInstance(null, message, positiveLabel, null, dialogOnClickListener);
				dialog.show(fragmentActivity.getSupportFragmentManager(), "updateFailedDialog");

			} else {
				String title = fragmentActivity.getString(R.string.update_dialog_title);

				String onServerVersion = response.getData().trim();
				if (currentVersion.equals(onServerVersion)) { // we are up to date
					// store it so the message does not appear after recent update:
					Configuration.getInstance(null).setOnServerVersion(onServerVersion);

					String message = fragmentActivity.getString(R.string.update_not_needed);
					String positiveLabel = fragmentActivity.getString(R.string.btn_ok);

					DialogFragment dialog = AlertDialogFragment.newInstance(title, message, positiveLabel, null, dialogOnClickListener);
					dialog.show(fragmentActivity.getSupportFragmentManager(), "upToDateDialog");

				} else { // new version is available:
					Configuration.getInstance(null).setOnServerVersion(onServerVersion);

					String message = fragmentActivity.getString(R.string.update_download_message);
					String positiveLabel = fragmentActivity.getString(R.string.update_button_download);
					String negativeLabel = fragmentActivity.getString(R.string.update_button_cancel);

					DialogFragment downloadDialog = AlertDialogFragment.newInstance(title, message, positiveLabel, negativeLabel, dialogOnClickListener);
					downloadDialog.show(fragmentActivity.getSupportFragmentManager(), "downloadDialog");
				}

				// set lastUpdateCheckDate when no update available:
				Configuration.getInstance(null).setLastUpdateCheckDate(new Date());
			}
		}

	}

	/**
	 * Dialog listener.
	 */
	private class MyDownloadDialogOnClickListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int buttonId) {
			switch (buttonId) {
			case DialogInterface.BUTTON_POSITIVE:
				// show the download web page:
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Configuration.getDownloadUrl()));
				fragmentActivity.startActivity(i);
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				// no action
				break;

			case DialogInterface.BUTTON_NEUTRAL:
				// no action
				break;

			default:
				// no action
				break;
			}

		}

	}

}
