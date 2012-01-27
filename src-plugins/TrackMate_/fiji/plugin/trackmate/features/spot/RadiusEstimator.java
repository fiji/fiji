package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;

public class RadiusEstimator extends IndependentSpotFeatureAnalyzer {

	/*
	 * CONSTANT
	 */
	
	/** The single feature key name that this analyzer computes. */
	public static final String						ESTIMATED_DIAMETER = "ESTIMATED_DIAMETER";
	private static final ArrayList<String> 			FEATURES = new ArrayList<String>(1);
	private static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	static {
		FEATURES.add(ESTIMATED_DIAMETER);
		FEATURE_NAMES.put(ESTIMATED_DIAMETER, "Estimated diameter");
		FEATURE_SHORT_NAMES.put(ESTIMATED_DIAMETER, "Diam.");
		FEATURE_DIMENSIONS.put(ESTIMATED_DIAMETER, Dimension.LENGTH);
	}
	
	private static final float MIN_DIAMETER_RATIO = 0.1f;
	private static final float MAX_DIAMETER_RATIO = 2;
	
	
	/*
	 * FIELDS
	 */
	
	/** Utility holder. */
	private float[] coords;
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
	public RadiusEstimator() { }	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void process(Spot spot) {
		coords = new float[img.getNumDimensions()];
		for (int i = 0; i < coords.length; i++) {
			coords[i] = spot.getFeature(Spot.POSITION_FEATURES[i]);
		}

		// Get diameter array and radius squared
		final float radius = spot.getFeature(Spot.RADIUS);
		final float[] diameters = prepareDiameters(radius*2, nDiameters);
		final float[] r2 = new float[nDiameters];
		for (int i = 0; i < r2.length; i++) {
			r2[i] = diameters[i] * diameters[i] / 4 ;
		}
		
		// Calculate total intensity in balls
		final float[] ring_intensities = new float[nDiameters];
		final int[]    ring_volumes = new int[nDiameters];

		final DomainCursor<? extends RealType<?>> cursor;
		if (img.getNumDimensions() == 3)
			cursor = new SphereCursor(img, coords, diameters[nDiameters-2]/2, calibration);
		else
			cursor = new DiscCursor(img, coords, diameters[nDiameters-2]/2, calibration);
		double d2;
		int i;
		while(cursor.hasNext())  {
			cursor.fwd();
			d2 = cursor.getDistanceSquared();
			for(i = 0 ; i < nDiameters-1 && d2 > r2[i] ; i++) {}
			ring_intensities[i] += cursor.getType().getRealDouble();
			ring_volumes[i]++;
		}

		// Calculate mean intensities from ring volumes
		final float[] mean_intensities = new float[diameters.length];
		for (int j = 0; j < mean_intensities.length; j++) 
			mean_intensities[j] = ring_intensities[j] / ring_volumes[j];
		
		// Calculate contrasts as minus difference between outer and inner rings mean intensity
		final float[] contrasts = new float[diameters.length - 1];
		for (int j = 0; j < contrasts.length; j++) {
			contrasts[j] = - ( mean_intensities[j+1] - mean_intensities[j] );
//			System.out.println(String.format("For diameter %.1f, found contrast of %.1f", diameters[j], contrasts[j])); 
		}
		
		// Find max contrast
		float maxConstrast = Float.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int j = 0; j < contrasts.length; j++) {
			if (contrasts[j] > maxConstrast) {
				maxConstrast = contrasts[j];
				maxIndex = j;
			}
		}
		
		float bestDiameter;
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
	
	private static final float quadratic1DInterpolation(float x1, float y1, float x2, float y2, float x3, float y3) {
		final float d2 = 2 * ( (y3-y2)/(x3-x2) - (y2-y1)/(x2-x1) ) / (x3-x1);
		if (d2==0)
			return x2;
		else {
			final float d1 = (y3-y2)/(x3-x2) - d2/2 * (x3-x2);
			return x2 -d1/d2;
		}
	}
	
	private static final float[] prepareDiameters(float centralDiameter, int nDiameters) {
		final float[] diameters = new float[nDiameters];
		for (int i = 0; i < diameters.length; i++) {
			diameters[i] = centralDiameter * ( MIN_DIAMETER_RATIO   
				+ i * (MAX_DIAMETER_RATIO - MIN_DIAMETER_RATIO)/(nDiameters-1) );
		}
		return diameters;
	}
	
	@Override
	public Collection<String> getFeatures() {
		return FEATURES;
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	}
}
