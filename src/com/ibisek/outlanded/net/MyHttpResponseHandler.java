package com.ibisek.outlanded.net;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

import com.ibisek.outlanded.utils.IOUtils;

/**
 * HTTP response handler.
 */
public class MyHttpResponseHandler implements ResponseHandler<ParsedHttpResponse> {

	@Override
	public ParsedHttpResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

		int status = response.getStatusLine().getStatusCode();
		String data = IOUtils.readFromStream(response.getEntity().getContent());

		return new ParsedHttpResponse(status, data);
	}

}
