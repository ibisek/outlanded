package com.ibisek.outlanded.navigation;

import java.util.StringTokenizer;

import com.ibisek.outlanded.navigation.gps.GpsMath;
import com.ibisek.outlanded.navigation.gps.GpsUtils;

public class POI implements Comparable<POI> {
	public enum TYPE {
		UNKNOWN, PEAK, HABITABLE
	};

	private float latitude, longitude;
	private String name;
	private TYPE type;
	private int size;
	private float distanceToOrigin;
	private float bearingToOrigin;

	/**
	 * @param latitude
	 * @param longitude
	 */
	public POI(float latitude, float longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		name = "ORIGIN";
		size = 0;
		type = TYPE.UNKNOWN;
	}

	/**
	 * @param line record file line to parse
	 */
	public POI(String line) {
		StringTokenizer st = new StringTokenizer(line, "\t");
		latitude = Float.parseFloat(st.nextToken());
		longitude = Float.parseFloat(st.nextToken());
		type = convertTypeStr(st.nextToken());
		name = st.nextToken();
		size = Integer.parseInt(st.nextToken());
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @param type
	 * @param name
	 * @param size
	 */
	public POI(float latitude, float longitude, String type, String name, int size) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.type = convertTypeStr(type);
		this.name = name;
		this.size = size;
	}

	/**
	 * Converts known POI type string to {@link TYPE}
	 * 
	 * @param typeStr
	 * @return
	 */
	private TYPE convertTypeStr(String typeStr) {
		TYPE type = TYPE.UNKNOWN;

		if ("P".equals(typeStr))
			type = TYPE.PEAK;
		else if ("H".equals(typeStr))
			type = TYPE.HABITABLE;

		return type;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();

		String[] latLon = GpsUtils.formatToGeocaching(Math.toDegrees(latitude), Math.toDegrees(longitude));
		sb.append(latLon[0]).append(" ");
		sb.append(latLon[1]).append(" ");
		sb.append(type).append(" ");
		sb.append(name).append(" ");
		sb.append(size).append(" ");
		sb.append(String.format("%.2fkm", distanceToOrigin)).append(" ");
		sb.append(String.format("%.0f°", bearingToOrigin));

		return sb.toString();
	}

	@Override
	public int compareTo(POI o) {
		float diff = this.getDistanceToOrigin() - o.getDistanceToOrigin();
		if (diff < 0)
			return -1;
		else if (diff > 0)
			return 1;
		else
			return 0;
	}

	/**
	 * @param poi
	 * @return bearing in degrees
	 */
	public float getBearingTo(POI poi) {
		return GpsMath.getBearing(latitude, longitude, poi.getLatitude(), poi.getLongitude());
	}

	/**
	 * @param poi
	 * @return distance to given POI
	 */
	public float getDistanceToInKm(POI poi) {
		return GpsMath.getDistanceInKm(latitude, longitude, poi.getLatitude(), poi.getLongitude());
	}

	/**
	 * @return bearing from origin in degrees
	 */
	public float getBearingFromOrigin() {
		return (bearingToOrigin + 180) % 360;
	}

	public float getLatitude() {
		return latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public String getName() {
		return name;
	}

	public int getSize() {
		return size;
	}

	public TYPE getType() {
		return type;
	}

	public float getDistanceToOrigin() {
		return distanceToOrigin;
	}

	public void setDistanceToOrigin(float distanceToOrigin) {
		this.distanceToOrigin = distanceToOrigin;
	}

	public float getBearingToOrigin() {
		return bearingToOrigin;
	}

	public void setBearingToOrigin(float bearingToOrigin) {
		this.bearingToOrigin = bearingToOrigin;
	}

}
