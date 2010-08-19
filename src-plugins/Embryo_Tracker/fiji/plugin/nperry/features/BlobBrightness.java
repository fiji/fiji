package fiji.plugin.nperry.features;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobBrightness <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final Feature FEATURE = Feature.BRIGHTNESS;
	private Image<T> img;
	private float diam;
	private float[] calibration;
	
	public BlobBrightness(Image<T> originalImage, float diam, float[] calibration) {
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
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		final float[] origin = spot.getCoordinates();

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
			if (inSphere(origin, cursor.getPosition(), diam / 2)) {
				sum += roi.getType().getRealDouble();
				//System.out.print(cursor.getPosition()[0] + ", " + cursor.getPosition()[1] + ", " + cursor.getPosition()[2] + "; ");
			}
		}

		// Close cursors
		roi.close();
		cursor.close();
		
		// Add total intensity.
		spot.addFeature(FEATURE, sum);
		//spot.addScore(FEATURE_NAME, sum);
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
	private boolean inSphere(float[] origin, int[] coords, float rad) {
		double euclDist = 0;
		for (int i = 0; i < coords.length; i++) {
			euclDist += Math.pow((origin[i] - (float) coords[i]) * calibration[i], 2);
		}
		euclDist = Math.sqrt(euclDist);
		return euclDist <= rad;
	}
}