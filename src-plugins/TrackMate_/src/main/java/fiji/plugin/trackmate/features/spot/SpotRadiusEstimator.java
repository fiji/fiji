package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER;

import java.util.Iterator;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotRadiusEstimator<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	/*
	 * FIELDS
	 */

	private static final double MIN_DIAMETER_RATIO = 0.1f;
	private static final double MAX_DIAMETER_RATIO = 2;
	
	/** The number of different diameters to try. */
	protected int nDiameters = 20;

	/**
	 * Create a feature analyzer that will return the best estimated diameter for a 
	 * spot. Estimated diameter is obtained by finding the diameter that gives the 
	 * maximum contrast, as calculated by difference in mean intensity in successive rings.
	 * Searched diameters are linearly spread between <code>diameter</code> * {@value #MIN_DIAMETER_RATIO}
	 * and <code>diameter</code> * {@value #MAX_DIAMETER_RATIO}. The optimum is them calculated by doing an interpolation
	 * over calculated values.
	 */
	public SpotRadiusEstimator(ImgPlus<T> img, Iterator<Spot> spots) {
		super(img, spots);
	}
	
	@Override
	public final void process(final Spot spot) {
		
		// Get diameter array and radius squared
		final double radius = spot.getFeature(Spot.RADIUS);
		final double[] diameters = prepareDiameters(radius*2, nDiameters);
		final double[] r2 = new double[nDiameters];
		for (int i = 0; i < r2.length; i++) {
			r2[i] = diameters[i] * diameters[i] / 4 ;
		}
		
		// Calculate total intensity in balls
		final double[] ring_intensities = new double[nDiameters];
		final int[]    ring_volumes 	= new int[nDiameters];

		// A tmp spot we will use to iterate around the real spot
		double[] coords = new double[3];
		TMUtils.localize(spot, coords);
		Spot tmpSpot = new Spot(coords);
		tmpSpot.putFeature(Spot.RADIUS, diameters[nDiameters-1]/2);

		SpotNeighborhood<T> neighborhood = new SpotNeighborhood<T>(tmpSpot , img);
		SpotNeighborhoodCursor<T> cursor = neighborhood.cursor();
		double d2, val;
		int i;
		while(cursor.hasNext())  {
			cursor.fwd();
			d2 = cursor.getDistanceSquared();
			val = cursor.get().getRealDouble();
			for(i = 0 ; i < nDiameters && d2 > r2[i] ; i++) {
				ring_intensities[i] += val;
				ring_volumes[i]++;
			}
		}
		
		// Calculate mean intensities from ring volumes
		final double[] mean_intensities = new double[diameters.length];
		for (int j = 0; j < mean_intensities.length; j++) 
			mean_intensities[j] = ring_intensities[j] / ring_volumes[j];
		
		// Calculate contrasts as minus difference between outer and inner rings mean intensity
		final double[] contrasts = new double[diameters.length - 1];
		for (int j = 0; j < contrasts.length-1; j++) {
			contrasts[j+1] = - ( mean_intensities[j+1] - mean_intensities[j] );
//			System.out.println(String.format("For diameter %.1f, found contrast of %.1f", diameters[j], contrasts[j])); //DEBUG
		}
		
		// Find max contrast
		double maxConstrast = Float.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int j = 0; j < contrasts.length; j++) {
			if (contrasts[j] > maxConstrast) {
				maxConstrast = contrasts[j];
				maxIndex = j;
			}
		}
		
		double bestDiameter;
		if ( 1 >= maxIndex || contrasts.length-1 == maxIndex) {
			bestDiameter = diameters[maxIndex];
		} else {
			bestDiameter = quadratic1DInterpolation(
					diameters[maxIndex-1], contrasts[maxIndex-1],
					diameters[maxIndex], contrasts[maxIndex],
					diameters[maxIndex+1], contrasts[maxIndex+1]);
		}
		spot.putFeature(ESTIMATED_DIAMETER, bestDiameter);		
	}
	
	private static final double quadratic1DInterpolation(double x1, double y1, double x2, double y2, double x3, double y3) {
		final double d2 = 2 * ( (y3-y2)/(x3-x2) - (y2-y1)/(x2-x1) ) / (x3-x1);
		if (d2==0)
			return x2;
		else {
			final double d1 = (y3-y2)/(x3-x2) - d2/2 * (x3-x2);
			return x2 -d1/d2;
		}
	}
	
	private static final double[] prepareDiameters(double centralDiameter, int nDiameters) {
		final double[] diameters = new double[nDiameters];
		for (int i = 0; i < diameters.length; i++) {
			diameters[i] = centralDiameter * ( MIN_DIAMETER_RATIO   
				+ i * (MAX_DIAMETER_RATIO - MIN_DIAMETER_RATIO)/(nDiameters-1) );
		}
		return diameters;
	}
}
