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


public class BlobContrast <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final Feature FEATURE = Feature.CONTRAST;
	protected static final double RAD_PERCENTAGE = .2;  // Percentage of radius we should average around the border to decide the contrast different. For example, if this is set to .1, then .1 * rad pixels within the radius of the blob is treated as the blob's internal edge, while .1 * rad pixels are considered the outside.
	protected Image<T> img;
	protected double diam;
	protected double[] calibration;
	
	public BlobContrast(Image<T> originalImage, double diam, double[] calibration) {
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

	protected double getContrast(final Spot spot, double diameter) {
		final LocalizableByDimCursor<T> cursorInner = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());	// ROI cursor which searches the pixels inside the blob's border
		final LocalizableByDimCursor<T> cursorOuter = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());	// ROI cursor which searches the pixels outside the blob's border
		final double[] origin = spot.getCoordinates();								// The coordinates for the center of the blob
		final int innerSize[] = new int[img.getNumDimensions()];						// The size of the ROI for the inner ROI cursor
		final int outerSize[] = new int[img.getNumDimensions()];						// The size of the ROI for the outer ROI cursor
		final int[] innerROICoords = new int[origin.length];					// The starting position for the inner ROI cursor
		final int[] outerROICoords = new int[origin.length];					// The starting position for the outer ROI cursor
		final ArrayList<Double> innerRadiusValues = new ArrayList<Double>();	// The intensities for pixels found just inside the blob's border
		final ArrayList<Double> outerRadiusValues = new ArrayList<Double>();	// The intensities for pixels found just outside the blob's border 
		final int[] curr = new int[origin.length];
		
		// --------------------- //
		// ------  Inside ------ //
		// --------------------- //
		
		// Create the size array for the ROI Inner cursor
		for (int i = 0; i < innerSize.length; i++) {
			innerSize[i] = (int) (diameter / calibration[i]);
		}
		
		// Convert spot's coordinates (which are a double[]) to int[], and reposition for ROI cursor
		for (int i = 0; i < innerROICoords.length; i++) {
			innerROICoords[i] = (int) Math.round(origin[i] - innerSize[i] / 2);
		}
		
		// Find points just inside the blob's border (must be less than radius from center, but further than 0.8 * radius from center)
		RegionOfInterestCursor<T> roiInner = cursorInner.createRegionOfInterestCursor(innerROICoords, innerSize);
		while (roiInner.hasNext()) {
			roiInner.next();
			cursorInner.getPosition(curr);
			if (inRing(origin, curr, diameter / 2, (diameter - diameter * RAD_PERCENTAGE) / 2)) {
				innerRadiusValues.add(roiInner.getType().getRealDouble());
			}
		}
		
		// Compute the average intensity for the pixels in this ring.
		double[] innerRadiusValuesArr = new double[innerRadiusValues.size()];
		for (int i = 0; i < innerRadiusValues.size(); i++) {
			innerRadiusValuesArr[i] = innerRadiusValues.get(i).doubleValue();
		}
		double innerAvg = MathLib.computeAverage(innerRadiusValuesArr);
		roiInner.close();
		
		// ---------------------- //
		// ------  Outside ------ //
		// ---------------------- //
		
		// Create the size array for the ROI Outer cursor
		for (int i = 0; i < outerSize.length; i++) {
			outerSize[i] = (int) ( (diameter + diameter * RAD_PERCENTAGE) / calibration[i] );
		}
		
		// Convert spot's coordinates (which are a double[]) to int[], and reposition for ROI cursor
		for (int i = 0; i < outerROICoords.length; i++) {
			outerROICoords[i] = (int) Math.round(origin[i] - outerSize[i] / 2);
		}
		
		// Find points just outside the blob's border
		RegionOfInterestCursor<T> roiOuter = cursorOuter.createRegionOfInterestCursor(outerROICoords, outerSize);
		while (roiOuter.hasNext()) {
			roiOuter.next();
			cursorOuter.getPosition(curr);
			if (inRing(origin, curr, (diameter + diameter * RAD_PERCENTAGE) / 2, diam / 2)) {
				outerRadiusValues.add(roiOuter.getType().getRealDouble());
			}
		}
		
		// Compute the average intensity for the pixels in this ring.
		double[] outerRadiusValuesArr = new double[outerRadiusValues.size()];
		for (int i = 0; i < outerRadiusValues.size(); i++) {
			outerRadiusValuesArr[i] = outerRadiusValues.get(i).doubleValue();
		}
		double outerAvg = MathLib.computeAverage(outerRadiusValuesArr);
		roiOuter.close();
		
		return innerAvg - outerAvg;		
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
	protected boolean inRing(double[] origin, int[] coords, double max, double min) {
		double euclDist = 0;
		for (int i = 0; i < coords.length; i++) {
			euclDist += (origin[i] - coords[i]) * (origin[i] - coords[i]) * calibration[i] * calibration[i];
		}
		euclDist = Math.sqrt(euclDist);
		return euclDist >= min && euclDist <= max;
	}
}
