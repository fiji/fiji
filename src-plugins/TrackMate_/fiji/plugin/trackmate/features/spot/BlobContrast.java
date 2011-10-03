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

public class BlobContrast extends IndependentSpotFeatureAnalyzer {

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
	
	protected static final float RAD_PERCENTAGE = .5f;  
	/** Utility holder. */
	private float[] coords = new float[3];
	

	@Override
	public void process(Spot spot) {
		float contrast = getContrast(spot);
		spot.putFeature(CONTRAST, Math.abs(contrast));
	}
	
	/**
	 * Compute the contrast for the given spot.
	 * @param spot
	 * @param diameter  the diameter to search for is in physical units
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected float getContrast(final Spot spot) {
		final float radius = spot.getFeature(Spot.RADIUS);
		final DomainCursor<? extends RealType<?>> cursor;
		if (img.getNumDimensions() == 3) 
			cursor = new SphereCursor(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE), calibration);
		else
			cursor = new DiscCursor(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE), calibration);
		int innerRingVolume = 0;
		int outerRingVolume = 0 ;
		float radius2 = radius * radius;
		float innerRadius2 = radius2 * (1-RAD_PERCENTAGE) * (1-RAD_PERCENTAGE);
		float innerTotalIntensity = 0;
		float outerTotalIntensity = 0;
		double dist2;
		
		while(cursor.hasNext()) {
			cursor.fwd();
			dist2 = cursor.getDistanceSquared();
			if (dist2 > radius2) {
				outerRingVolume++;
				outerTotalIntensity += cursor.getType().getRealFloat();				
			} else if (dist2 > innerRadius2) {
				innerRingVolume++;
				innerTotalIntensity += cursor.getType().getRealFloat();
			}
		}
		cursor.close();
		
		float innerMeanIntensity = innerTotalIntensity / innerRingVolume; 
		float outerMeanIntensity = outerTotalIntensity / outerRingVolume;
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
