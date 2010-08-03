package fiji.plugin.nperry.scoring;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;


public class BlobContrastScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "BlobContrastScorer";
	private static final double RAD_PERCENTAGE = .2;  // Percentage of radius we should average around the border to decide the contrast different. For example, if this is set to .1, then .1 * rad pixels within the radius of the blob is treated as the blob's internal edge, while .1 * rad pixels are considered the outside.
	private Image<T> img;
	private double diam;
	private double[] calibration;
	
	public BlobContrastScorer(Image<T> originalImage, double diam, double[] calibration) {
		this.img = originalImage;
		this.diam = diam;
		this.calibration = calibration;
	}
	
	@Override
	public String getName() {
		return SCORING_METHOD_NAME;
	}

	@Override
	public boolean isNormalized() {
		return false;
	}

	@Override
	public void score(Spot spot) {
		final LocalizableByDimCursor<T> cursorInner = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		final LocalizableByDimCursor<T> cursorOuter = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		double[] origin = spot.getCoordinates();
		final int[] innerCoords = new int[origin.length];
		final int[] outerCoords = new int[origin.length];
		final ArrayList<Double> innerRadiusValues = new ArrayList<Double>();
		final ArrayList<Double> outerRadiusValues = new ArrayList<Double>();

		// Create the size array for the ROI Inner cursor
		int innerSize[] = new int[img.getNumDimensions()];
		for (int i = 0; i < innerSize.length; i++) {
			innerSize[i] = (int) (diam / calibration[i]);
		}
		
		// Create the size array for the ROI Outer cursor
		int outerSize[] = new int[img.getNumDimensions()];
		for (int i = 0; i < outerSize.length; i++) {
			outerSize[i] = (int) ( (diam + diam * RAD_PERCENTAGE) / calibration[i] );
		}
		
		// Convert spot's coords (double[]) to int[], and reposition for ROI cursor
		for (int i = 0; i < innerCoords.length; i++) {
			innerCoords[i] = (int) Math.round(origin[i] - innerSize[i] / 2);
		}
		for (int i = 0; i < outerCoords.length; i++) {
			outerCoords[i] = (int) Math.round(origin[i] - outerSize[i] / 2);
		}
		
		int[] curr = new int[origin.length];
		// Average intensity *inside* blob border
		RegionOfInterestCursor<T> roiInner = cursorInner.createRegionOfInterestCursor(innerCoords, innerSize);

		while (roiInner.hasNext()) {
			roiInner.next();
			cursorInner.getPosition(curr);
			if (inside(origin, curr, diam / 2) && outside(origin, curr, (diam / 2) - (diam / 2) * RAD_PERCENTAGE)) {
				innerRadiusValues.add(roiInner.getType().getRealDouble());
			}
		}
		
		double[] innerRadiusValuesArr = new double[innerRadiusValues.size()];
		for (int i = 0; i < innerRadiusValues.size(); i++) {
			innerRadiusValuesArr[i] = innerRadiusValues.get(i).doubleValue();
		}
		double innerAvg = MathLib.computeAverage(innerRadiusValuesArr);
		roiInner.close();
		
		// Average intensity *outside* blob border
		RegionOfInterestCursor<T> roiOuter = cursorOuter.createRegionOfInterestCursor(outerCoords, outerSize);
		
		while (roiOuter.hasNext()) {
			roiOuter.next();
			cursorOuter.getPosition(curr);
			if (outside(origin, curr, diam / 2) && inside(origin, curr, (diam / 2) + (diam / 2) * RAD_PERCENTAGE)) {
				outerRadiusValues.add(roiOuter.getType().getRealDouble());
			}
		}
		
		double[] outerRadiusValuesArr = new double[outerRadiusValues.size()];
		for (int i = 0; i < outerRadiusValues.size(); i++) {
			outerRadiusValuesArr[i] = outerRadiusValues.get(i).doubleValue();
		}
		double outerAvg = MathLib.computeAverage(outerRadiusValuesArr);
		roiOuter.close();
		
		// Add average contrast different along border as the spot's score.
		spot.addScore(SCORING_METHOD_NAME, Math.abs(innerAvg - outerAvg));
	}

	private boolean inside(double[] origin, int[] coords, double rad) {
		for (int i = 0; i < coords.length; i++) {
			if (Math.abs(coords[i] - origin[i]) / (rad / calibration[i]) > 1) return false; 
		}
		return true;
	}

	private boolean outside(double[] origin, int[] coords, double rad) {
		for (int i = 0; i < coords.length; i++) {
			if (Math.abs(coords[i] - origin[i]) / (rad / calibration[i]) <= 1) return false; 
		}
		return true;
	}
}
