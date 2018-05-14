package com.ibisek.outlanded.navigation.proximity;

import java.util.List;

import com.ibisek.outlanded.navigation.POI;

public interface ProximityListener {
	
	/**
	 * @param nearestPoints
	 */
	void notify(List<POI> nearestPoints);

}
