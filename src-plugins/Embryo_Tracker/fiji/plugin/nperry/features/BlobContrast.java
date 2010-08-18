package fiji.plugin.nperry.features;

import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobContrast <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final Feature FEATURE = Feature.CONTRAST;
	protected static final float RAD_PERCENTAGE = .2f;  // Percentage of radius we should average around the border to decide the contrast different. For example, if this is set to .1, then .1 * rad pixels within the radius of the blob is treated as the blob's internal edge, while .1 * rad pixels are considered the outside.
	protected Image<T> img;
	protected float diam;
	protected float[] calibration;
	
	public BlobContrast(Image<T> originalImage, float diam, float[] calibration) {
		this.img = originalImage;
		this.diam = diam;
		this.calibration = calibration;
	}
	
	@Override
	public Feature getFeature() {
		return FEATURE;
	}

	@Override
	public boolean isNormalized() {
		return false;
	}

	@Override
	public void process(Spot spot) {
		double contrast = getContrast(spot, diam);
		spot.addFeature(FEATURE, Math.abs(contrast));
	}

	/**
	 * 
	 * @param spot
	 * @param diameter  the diameter to search for is in physical units
	 * @return
	 */
	protected float getContrast(final Spot spot, float diameter) {
		SphereCursor<T> cursor = new SphereCursor<T>(img, spot.getCoordinates(), diameter/2 * (1+RAD_PERCENTAGE), calibration);
		int innerRingVolume = 0;
		int outerRingVolume = 0 ;
		float radius2 = diameter * diameter / 4;
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
