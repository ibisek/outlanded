package com.ibisek.outlanded.storage;

import android.content.Context;

public class LastUsedMessageDao {

	private static final String LAST_USED_MESSAGE_FILENAME = "last-used-message.bin";

	private InternalStorage<String> storage;

	public LastUsedMessageDao(Context context) {
		storage = new InternalStorage<String>(LAST_USED_MESSAGE_FILENAME, context);
	}

	public String getLastUsedMessage() {
		return storage.load();
	}

	public void saveLastUsedMessage(String message) {
		message = message.replaceAll("\\[(.*?)\\]", "[]"); // do not store current GPS fix
		message = message.replaceAll("\\{(.*?)\\}", "{}"); // do not store current location
		storage.save(message);
	}

}
