package com.ibisek.outlanded.utils;

public class MathUtils {

	/**
	 * @param value
	 * @param precision
	 * @return value rounded to specified precision
	 */
	public static float round(float value, int precision) {
		if (precision <= 0) {
			throw new IllegalArgumentException("Precision cannot be zero or less.");
		}

		int x = (int) Math.pow(10, precision);
		int i = (int) Math.round(value * x);
		return (float) i / x;
	}
	
}
