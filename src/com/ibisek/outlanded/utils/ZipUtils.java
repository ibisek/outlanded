package com.ibisek.outlanded.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @version 2014-06-03
 */
public class ZipUtils {

	/**
	 * @param zipEntryName
	 * @param text
	 * @return null in case of exception
	 */
	public static byte[] zip(String zipEntryName, String text) {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(baos);
			ZipEntry e = new ZipEntry(zipEntryName);
			zip.putNextEntry(e);

			byte[] data = text.getBytes();
			zip.write(data, 0, data.length);
			zip.closeEntry();

			zip.close();

			byte[] zippedBytes = baos.toByteArray();
			return zippedBytes;

		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * @param zippedBytes
	 * @return unzipped entries as one string
	 */
	public static String unzip(byte[] zippedBytes) {
		StringBuilder sb = new StringBuilder();

		ByteArrayInputStream bais = new ByteArrayInputStream(zippedBytes);
		ZipInputStream unzip = new ZipInputStream(bais);

		try {
			ZipEntry e = null;
			while ((e = unzip.getNextEntry()) != null) {
				System.out.println("Unzipping '" + e.getName() + "'");

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				for (int c = unzip.read(); c != -1; c = unzip.read()) {
					baos.write(c);
				}
				unzip.closeEntry();

				sb.append(new String(baos.toByteArray()));
				baos.close();
			}

			unzip.close();

		} catch (IOException ex) {
			return null;
		}

		return sb.toString();
	}
	
}
