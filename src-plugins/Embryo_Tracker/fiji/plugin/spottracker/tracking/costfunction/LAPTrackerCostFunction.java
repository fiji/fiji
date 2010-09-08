package fiji.plugin.spottracker.tracking.costfunction;

import fiji.plugin.spottracker.Spot;

/**
 * 
 * @author Nicholas Perry
 *
 */
public abstract class LAPTrackerCostFunction implements CostFunctions {

	/*
	 * Determines the Euclidean distance between two Spots.
	 */
	protected static double euclideanDistance(Spot i, Spot j) {
		final float[] coordsI = i.getCoordinates();
		final float[] coordsJ = j.getCoordinates();
		double eucD = 0;

		for (int k = 0; k < coordsI.length; k++) {
			eucD += (coordsI[k] - coordsJ[k]) * (coordsI[k] - coordsJ[k]);
		}
		eucD = Math.sqrt(eucD);

		return eucD;
	}
}
