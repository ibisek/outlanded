package com.ibisek.outlanded.net;

public class ParsedHttpResponse {

	private int status;
	private String data;

	/**
	 * @param status HTTP response code
	 * @param data string data read from the response
	 */
	public ParsedHttpResponse(int status, String data) {
		this.status = status;
		this.data = data;
	}

	public int getStatus() {
		return status;
	}

	public String getData() {
		return data;
	}

}
