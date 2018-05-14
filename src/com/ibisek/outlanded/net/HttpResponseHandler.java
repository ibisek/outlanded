package com.ibisek.outlanded.net;


public interface HttpResponseHandler {
	
	/**
	 * @param httpResponse
	 * @return possible return value or simply null
	 */
	public Object handleResponse(ParsedHttpResponse httpResponse);

}
