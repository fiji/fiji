package fiji.plugin.trackmate.features.spot;

import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;

public class BlobContrast <T extends RealType<T>> extends IndependentSpotFeatureAnalyzer {

	private static final SpotFeature FEATURE = SpotFeature.CONTRAST;
	protected static final float RAD_PERCENTAGE = .2f;  
	protected Image<T> img;
	protected float[] calibration;
	/** Utility holder. */
	private float[] coords;
	
	
	public BlobContrast(Image<T> originalImage, float[] calibration) {
		this.img = originalImage;
		this.calibration = calibration;
		this.coords = new float[3];
	}
	
	@Override
	public SpotFeature getFeature() {
		return FEATURE;
	}

	@Override
	public void process(Spot spot) {
		float contrast = getContrast(spot);
		spot.putFeature(FEATURE, Math.abs(contrast));
	}
	
	/**
	 * 
	 * @param spot
	 * @param diameter  the diameter to search for is in physical units
	 * @return
	 */
	protected float getContrast(final Spot spot) {
		final float radius = spot.getFeature(SpotFeature.RADIUS);
		final DomainCursor<T> cursor;
		if (img.getNumDimensions() == 3) 
			cursor = new SphereCursor<T>(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE), calibration);
		else
			cursor = new DiscCursor<T>(img, spot.getPosition(coords), radius * (1+RAD_PERCENTAGE), calibration);
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

}
