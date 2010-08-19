package fiji.plugin.nperry.features;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobBrightness <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	/*
	 * FIELDS
	 */
	
	/** The {@link Feature} that this FeatureAnalyzer extracts. */
	private static final Feature FEATURE = Feature.BRIGHTNESS;
	/** The original image that is analyzed. */
	private Image<T> img;
	/** The diameter of the blob, in physical units. */
	private float diam;
	/** The calibration of the image, used to convert from physical units to pixel units. */
	private float[] calibration;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public BlobBrightness(Image<T> originalImage, float diam, float[] calibration) {
		this.img = originalImage;
		this.diam = diam;
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
	public boolean isNormalized() {
		return false;
	}

	@Override
	public void process(Spot spot) {
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		final float[] origin = spot.getCoordinates();  // physical coordinates
		
		// Convert physical coordinates to pixel coordinates
		final int[] pOrigin = new int[origin.length];
		for (int i = 0; i < pOrigin.length; i++) {
			pOrigin[i] = (int) (origin[i] / calibration[i]);
		}

		// Create the size array for the ROI cursor
		int size[] = new int[img.getNumDimensions()];
		for (int i = 0; i < size.length; i++) {
			size[i] = (int) (diam / calibration[i]);  // convert back to pixel units
		}

		// Adjust the integer coordinates of the spot to set the ROI correctly
		int[] roiCoords = new int[img.getNumDimensions()];
		for (int i = 0; i < origin.length; i++) {
			roiCoords[i] = (int) (origin[i] - (size[i] / 2));  
		}
		
		// Use ROI cursor to search a sphere around the spot's coordinates
		/* need to handle case where ROI is not in image anymore!! */
		double sum = 0;
		RegionOfInterestCursor<T> roi = cursor.createRegionOfInterestCursor(roiCoords, size);
		//System.out.println();
		//System.out.println("Maximum: " + origin[0] + ", " + origin[1] + ", " + origin[2] + "; ");
		//System.out.println();
		while (roi.hasNext()) {
			roi.next();
			if (inSphere(pOrigin, cursor.getPosition())) {
				sum += roi.getType().getRealDouble();
				//System.out.print(cursor.getPosition()[0] + ", " + cursor.getPosition()[1] + ", " + cursor.getPosition()[2] + "; ");
			}
		}

		// Close cursors
		roi.close();
		cursor.close();
		
		// Add total intensity.
		spot.addFeature(FEATURE, sum);
	}
	
	/**
	 * Determines if the coordinate coords is at least min distance away from the
	 * origin, but within max distance. The distance metric used is Euclidean
	 * distance.
	 * 
	 * @param origin 
	 * @param coords
	 * @param max
	 * @param min
	 * @return
	 */
	private boolean inSphere(int[] pOrigin, int[] coords) {
		double euclDist = 0;
		for (int i = 0; i < coords.length; i++) {
			euclDist += Math.pow((pOrigin[i] - coords[i]), 2);
		}
		euclDist = Math.sqrt(euclDist);
		return euclDist <= (diam / 2);
	}
}