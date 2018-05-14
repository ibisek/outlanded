package com.ibisek.outlanded.navigation.proximity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.gps.GpsMath;
import com.ibisek.outlanded.utils.MathUtils;

public class NearestPoints {
	private POI origin;
	private int maxCount;
	private List<Float> distances;
	private List<POI> points;

	private double maxDistance = Double.MAX_VALUE;
	private int maxDistanceIndex = 0;

	/**
	 * @param origin
	 * @param maxCount
	 */
	public NearestPoints(POI origin, int maxCount) {
		this.origin = origin;
		this.maxCount = maxCount;
		distances = new ArrayList<Float>(maxCount);
		points = new ArrayList<POI>(maxCount);
	}

	private void findMaxDistance() {
		maxDistance = -1;
		for (int i = 0; i < distances.size(); i++) {
			if (distances.get(i) > maxDistance) {
				maxDistance = distances.get(i);
				maxDistanceIndex = i;
			}
		}
	}

	/**
	 * @param poi
	 * @return distance to given POI
	 */
	public float check(POI poi) {
		double lat1 = origin.getLatitude();
		double lon1 = origin.getLongitude();
		double lat2 = poi.getLatitude();
		double lon2 = poi.getLongitude();
		float dist = GpsMath.getDistanceInKm(lat1, lon1, lat2, lon2);
		dist = MathUtils.round(dist, 2);

		if (dist < maxDistance) {
			poi.setDistanceToOrigin(dist);
			poi.setBearingToOrigin(GpsMath.getBearing(lat1, lon1, lat2, lon2));
			if (distances.size() < maxCount) {
				distances.add(dist);
				points.add(poi);
			} else {
				distances.set(maxDistanceIndex, dist);
				points.set(maxDistanceIndex, poi);
			}

			findMaxDistance();
		}
		
		return dist;
	}

	/**
	 * @return by distance ordered list of {@link POI}s. Nearest comes first.
	 */
	public List<POI> getPoints() {
		Collections.sort(points);
		return points;
	}

}
