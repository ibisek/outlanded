package com.ibisek.outlanded.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.message.BasicNameValuePair;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

public class MyHttpClient {

	private final static String TAG = MyHttpClient.class.getSimpleName();

	// XXX for dev only!!
	public final static boolean USE_HTTP_PROXY = false;
	public final static String HTTP_PROXY_HOST = "158.234.170.80";
	public final static int HTTP_PROXY_PORT = 3128;

	/**
	 * @param url
	 * @param postParams
	 * @param postResponseHandler
	 *          may be null if you don't care about the response
	 */
	public void doPost(String url, Map<String, String> postParams, HttpResponseHandler postResponseHandler) {
		new PostAsyncTask(url, postParams, postResponseHandler).execute();
	}

	private class PostAsyncTask extends AsyncTask<Void, Void, ParsedHttpResponse> {

		private String url;
		private Map<String, String> postParams;
		private HttpResponseHandler postResponseHandler;

		/**
		 * @param url
		 * @param postParams
		 * @param postResponseHandler
		 *          may be null if you don't care about the response
		 */
		public PostAsyncTask(String url, Map<String, String> postParams, HttpResponseHandler postResponseHandler) {
			this.url = url;
			this.postParams = postParams;
			this.postResponseHandler = postResponseHandler;
		}

		@Override
		protected ParsedHttpResponse doInBackground(Void... params) {
			ParsedHttpResponse response = null;

			AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
			try {

				// set HTTP proxy (if needed):
				if (USE_HTTP_PROXY) {
					HttpHost proxy = new HttpHost(HTTP_PROXY_HOST, HTTP_PROXY_PORT);
					httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
					Log.d(TAG, String.format("Proxy set to %s:%s", HTTP_PROXY_HOST, HTTP_PROXY_PORT));
				}

				Log.d(TAG, "Will do POST to " + url);
				HttpPost post = new HttpPost(url);

				List<NameValuePair> pairs = new ArrayList<NameValuePair>();

				if (postParams != null && postParams.size() > 0) {
					for (String key : postParams.keySet()) {
						pairs.add(new BasicNameValuePair(key, postParams.get(key)));
					}
					post.setEntity(new UrlEncodedFormEntity(pairs));
				}

				response = httpClient.execute(post, new MyHttpResponseHandler());
				Log.d(TAG, "Response code: " + response.getStatus());
				if (response.getStatus() != 200)
					Log.d(TAG, "Response data: " + response.getData());

			} catch (UnknownHostException ex) {
				Log.d(TAG, "Network is not reachable.. that is OK. " + ex.getMessage());
			} catch (IOException ex) {
				Log.d(TAG, "Error when attempting to perform HTTP POST request.", ex);
			
			} finally {
				httpClient.close();
			}

			return response;
		}

		@Override
		protected void onPostExecute(ParsedHttpResponse response) {
			if (postResponseHandler != null) {
				postResponseHandler.handleResponse(response);
			}
		}

	}

}
