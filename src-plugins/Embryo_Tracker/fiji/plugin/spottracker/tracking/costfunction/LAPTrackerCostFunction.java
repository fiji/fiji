package fiji.plugin.spottracker.tracking.costfunction;

import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.tracking.costmatrix.LAPTrackerCostMatrixCreator;

public abstract class LAPTrackerCostFunction implements CostFunctions {

	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected final static double BLOCKED = LAPTrackerCostMatrixCreator.BLOCKED;

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
