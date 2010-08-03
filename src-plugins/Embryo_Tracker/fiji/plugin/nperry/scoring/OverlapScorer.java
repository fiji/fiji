package fiji.plugin.nperry.scoring;

import java.util.ArrayList;
import java.util.Iterator;

import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;

public class OverlapScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "OverlapScorer";
	private double diam;
	private double[] calibration;
	private ArrayList<Spot> spots;
	               
	public OverlapScorer(double diam, double[] calibration, ArrayList<Spot> spots) {
		this.diam = diam;
		this.calibration = calibration;
		this.spots = spots;
	}
	
	@Override
	public String getName() {
		return SCORING_METHOD_NAME;
	}

	@Override
	public void score(Spot spot) {
		final double[] coords = spot.getCoordinates();
		
		int numWithinDiam = 0;
		
		Iterator<Spot> itr = spots.iterator();
		while (itr.hasNext()) {
			Spot otherSpot = itr.next();
			int counter = 0;
			for (int i = 0; i < coords.length; i++) {
				if (Math.abs(coords[i] - otherSpot.getCoordinates()[i]) < (diam / calibration[i])) {
					counter++;
					}
			}
			if (counter == coords.length) {
				numWithinDiam++;
			}
		}
		
		int score = numWithinDiam > 1 ? 1 / (numWithinDiam - 1) : 1;  // Subtract one, because we compared spot against itself.
		spot.addScore(SCORING_METHOD_NAME, score);
	}
	
	@Override
	public boolean isNormalized() {
		return false;
	}
}
