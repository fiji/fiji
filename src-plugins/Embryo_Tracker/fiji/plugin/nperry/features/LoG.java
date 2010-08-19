package fiji.plugin.nperry.features;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class LoG <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	/*
	 * FIELDS
	 */
	
	private static final Feature FEATURE = Feature.LOG_VALUE;
	private Image<T> img;
	private LocalizableByDimCursor<T> cursor;
	private float[] downsampleFactors;
	private float[] calibration;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public LoG(Image<T> filteredImage) {
		this.img = filteredImage;
		this.cursor = img.createLocalizableByDimCursor();
		this.calibration = filteredImage.getCalibration();
		this.downsampleFactors = new float[filteredImage.getNumDimensions()];
		
		for (int i = 0; i < downsampleFactors.length; i++) {
			downsampleFactors[i] = 1;
		}
	}
	
	public LoG(Image<T> filteredImage, float[] downsampleFactors) {
		this.img = filteredImage;
		this.cursor = img.createLocalizableByDimCursor();
		this.downsampleFactors = downsampleFactors;
		this.calibration = filteredImage.getCalibration();
	}
	
	public LoG(Image<T> filteredImage, float[] downsampleFactors, float[] calibration) {
		this.img = filteredImage;
		this.downsampleFactors = downsampleFactors;
		this.cursor = img.createLocalizableByDimCursor();
		this.calibration = calibration;
	}

	/*
	 * PUBLIC METHODS 
	 */
	
	@Override
	public Feature getFeature() {
		return FEATURE;
	}

	@Override
	public void process(Spot spot) {
		final float[] coords = spot.getCoordinates().clone();
		
		// 1 - Convert physical coords to pixel coords
		for (int i = 0; i < coords.length; i++) {
			coords[i] = coords[i] / calibration[i];
		}
		
		// 2 - Downsample pixel coords, since we are using the downsampled image.
		for (int i = 0; i < coords.length; i++) {
			coords[i] = coords[i] / downsampleFactors[i];
		}
		
		// 3 - Store the float[] coords as a int[] to set the cursor with
		final int[] intCoords = new int[coords.length];
		for (int i = 0; i < intCoords.length; i++) {
			intCoords[i] = (int) coords[i];
		}
		
		// 4 - Get the intensity at the spot's coordinates
		cursor.setPosition(intCoords);
		spot.addFeature(FEATURE, cursor.getType().getRealFloat());
	}
	
	@Override
	public boolean isNormalized() {
		return false;
	}
}
