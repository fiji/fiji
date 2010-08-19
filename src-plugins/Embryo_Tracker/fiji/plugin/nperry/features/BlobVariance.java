package fiji.plugin.nperry.features;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobVariance <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final Feature FEATURE = Feature.VARIANCE;
	private Image<T> img;
	private double diam;
	private double[] calibration;
	
	public BlobVariance(Image<T> originalImage, double diam, double[] calibration) {
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
		final ArrayList<Double> values = new ArrayList<Double>();
		
		// Create the size array for the ROI cursor
		int size[] = new int[img.getNumDimensions()];
		for (int i = 0; i < size.length; i++) {
			size[i] = (int) (diam / calibration[i]);
		}

		// Adjust the integer coordinates of the spot to set the ROI correctly
		int[] roiCoords = new int[img.getNumDimensions()];
		for (int i = 0; i < origin.length; i++) {
			roiCoords[i] = (int) (origin[i] - (size[i] / 2));  
		}
		
		// Use ROI cursor to search a sphere around the spot's coordinates
		RegionOfInterestCursor<T> roi = cursor.createRegionOfInterestCursor(roiCoords, size);
		//System.out.println();
		//System.out.println("Maximum: " + origin[0] + ", " + origin[1] + ", " + origin[2] + "; ");
		//System.out.println();
		while (roi.hasNext()) {
			roi.next();
			if (inSphere(origin, cursor.getPosition(), diam / 2)) {
				values.add(roi.getType().getRealDouble());
				//System.out.print(cursor.getPosition()[0] + ", " + cursor.getPosition()[1] + ", " + cursor.getPosition()[2] + "; ");
			}
		}
		
		// Compute variance for this blob.
		double[] valuesArr = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			valuesArr[i] = values.get(i).doubleValue();
		}
		double avg = MathLib.computeAverage(valuesArr);
		float var = 0;
		for (int j = 0; j < valuesArr.length; j++) {
			var += Math.pow(valuesArr[j] - avg, 2);
		}
		var /= valuesArr.length;

		// Close cursors
		roi.close();
		cursor.close();
		
		// Add variance as the spot's score. Apply inverse so lower variance receives a higher score.
		spot.addFeature(FEATURE, var);
		//spot.addScore(FEATURE_NAME, 1/var);
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
	private boolean inSphere(float[] origin, int[] coords, double rad) {
		double euclDist = 0;
		for (int i = 0; i < coords.length; i++) {
			euclDist += Math.pow((origin[i] - (double) coords[i]) * calibration[i], 2);
		}
		euclDist = Math.sqrt(euclDist);
		return euclDist <= rad;
	}
}
