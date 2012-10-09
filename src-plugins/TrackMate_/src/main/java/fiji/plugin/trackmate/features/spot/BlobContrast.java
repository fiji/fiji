package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;

public class BlobContrast<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	/** The single feature key name that this analyzer computes. */
	public static final String						CONTRAST = "CONTRAST";
	private static final ArrayList<String> 			FEATURES = new ArrayList<String>(1);
	private static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(1);
	private static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(1);
	static {
		FEATURES.add(CONTRAST);
		FEATURE_NAMES.put(CONTRAST, "Contrast");
		FEATURE_SHORT_NAMES.put(CONTRAST, "Contrast");
		FEATURE_DIMENSIONS.put(CONTRAST, Dimension.NONE);
	}
	
	protected static final double RAD_PERCENTAGE = .5f;  
	

	@Override
	public void process(Spot spot) {
		double contrast = getContrast(spot);
		spot.putFeature(CONTRAST, Math.abs(contrast));
	}
	
	/**
	 * Compute the contrast for the given spot.
	 * @param spot
	 * @param diameter  the diameter to search for is in physical units
	 * @return
	 */
	protected double getContrast(final Spot spot) {

		final SpotNeighborhood<T> neighborhood = new SpotNeighborhood<T>(spot, img);
		
		final double radius = spot.getFeature(Spot.RADIUS);
		long innerRingVolume = 0;
		long outerRingVolume = 0 ;
		double radius2 = radius * radius;
		double innerRadius2 = radius2 * (1-RAD_PERCENTAGE) * (1-RAD_PERCENTAGE);
		double innerTotalIntensity = 0;
		double outerTotalIntensity = 0;
		double dist2;
		
		SpotNeighborhoodCursor<T> cursor = neighborhood.cursor();
		while(cursor.hasNext()) {
			cursor.fwd();
			dist2 = cursor.getDistanceSquared();
			if (dist2 > radius2) {
				outerRingVolume++;
				outerTotalIntensity += cursor.get().getRealDouble();		
			} else if (dist2 > innerRadius2) {
				innerRingVolume++;
				innerTotalIntensity += cursor.get().getRealDouble();
			}
		}
		
		double innerMeanIntensity = innerTotalIntensity / innerRingVolume; 
		double outerMeanIntensity = outerTotalIntensity / outerRingVolume;
		return innerMeanIntensity - outerMeanIntensity;
	}
}
