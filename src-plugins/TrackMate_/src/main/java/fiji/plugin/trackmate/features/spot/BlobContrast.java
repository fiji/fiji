package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.cursor.special.DiscCursor;
import net.imglib2.cursor.special.DomainCursor;
import net.imglib2.cursor.special.SphereCursor;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;

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
	/** Utility holder. */
	private double[] coords = new double[3];
	

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
		final double radius = spot.getFeature(Spot.RADIUS);
		final DomainCursor<T> cursor;
		if (img.numDimensions() == 3) 
			cursor = new SphereCursor(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE));
		else
			cursor = new DiscCursor(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE));
		int innerRingVolume = 0;
		int outerRingVolume = 0 ;
		double radius2 = radius * radius;
		double innerRadius2 = radius2 * (1-RAD_PERCENTAGE) * (1-RAD_PERCENTAGE);
		double innerTotalIntensity = 0;
		double outerTotalIntensity = 0;
		double dist2;
		
		while(cursor.hasNext()) {
			cursor.fwd();
			dist2 = cursor.getDistanceSquared();
			if (dist2 > radius2) {
				outerRingVolume++;
				outerTotalIntensity += cursor.get().getRealFloat();				
			} else if (dist2 > innerRadius2) {
				innerRingVolume++;
				innerTotalIntensity += cursor.get().getRealFloat();
			}
		}
		
		double innerMeanIntensity = innerTotalIntensity / innerRingVolume; 
		double outerMeanIntensity = outerTotalIntensity / outerRingVolume;
		return innerMeanIntensity - outerMeanIntensity;
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
