package fiji.plugin.nperry.scoring;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;


public class BlobContrastScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "BlobContrastScorer";
	private static final double BORDER_LAYER_WIDTH_PERCENTAGE = .2;  // Percentage of radius we should average around the border to decide the contrast different. For example, if this is set to .1, then .1 * rad pixels within the radius of the blob is treated as the blob's internal edge, while .1 * rad pixels are considered the outside.
	private static final double IDEAL_BLOB_RADIUS = 5.0;
	private Image<T> img;
	private double rad;
	private double[] downsampleFactors;
	
	public BlobContrastScorer(Image<T> originalImage, double diam) {
		this.img = originalImage;
		this.rad = Math.min((int) MathLib.round(diam/2), IDEAL_BLOB_RADIUS);
		this.downsampleFactors = new double[originalImage.getNumDimensions()];
		
		for (int i = 0; i < originalImage.getNumDimensions(); i++) {
			downsampleFactors[i] = 1.0;
		}
	}
	
	public BlobContrastScorer(Image<T> originalImage, double diam, double[] downsampleFactors) {
		this.img = originalImage;
		this.rad = Math.min((int) MathLib.round(diam/2), IDEAL_BLOB_RADIUS);
		this.downsampleFactors = downsampleFactors;
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
		final LocalizableByDimCursor<T> cursorInner = img.createLocalizableByDimCursor();
		final LocalizableByDimCursor<T> cursorOuter = img.createLocalizableByDimCursor();
		double[] origin = spot.getCoordinates();
		final int[] innerCoords = new int[origin.length];
		final int[] outerCoords = new int[origin.length];
		final ArrayList<Double> innerRadiusValues = new ArrayList<Double>();
		final ArrayList<Double> outerRadiusValues = new ArrayList<Double>();
		
		final double innerRad = rad - (rad * BORDER_LAYER_WIDTH_PERCENTAGE);
		final double outerRad = (rad + 1) + (rad * BORDER_LAYER_WIDTH_PERCENTAGE);
		
		// Convert spot's potentially downsized coords to the original image's coordinate scale
		origin = convertDownsampledImgCoordsToOriginalCoords(origin);
		
		// Convert spot's coords (double[]) to int[], and reposition for ROI cursor
		for (int i = 0; i < innerCoords.length; i++) {
			innerCoords[i] = (int) Math.round(origin[i] - rad);
		}
		for (int i = 0; i < outerCoords.length; i++) {
			outerCoords[i] = (int) Math.round(origin[i] - outerRad);
		}
		
		// Create the size array for the ROI Inner cursor
		int innerSize[] = new int[img.getNumDimensions()];
		for (int i = 0; i < innerSize.length; i++) {
			innerSize[i] = (int) (rad * 2);
		}
		
		// Create the size array for the ROI Outer cursor
		int outerSize[] = new int[img.getNumDimensions()];
		for (int i = 0; i < outerSize.length; i++) {
			outerSize[i] = (int) (outerRad * 2);
		}
		
		int[] curr = new int[origin.length];
		// Average intensity *inside* blob border
		RegionOfInterestCursor<T> roiInner = cursorInner.createRegionOfInterestCursor(innerCoords, innerSize);

		//System.out.println("###Inner###");
		//System.out.println("Inner rad: " + innerRad + ", rad: " + rad);
		while (roiInner.hasNext()) {
			roiInner.next();
			cursorInner.getPosition(curr);
			if (isInRing(origin, curr, innerRad, rad)) {
				innerRadiusValues.add(roiInner.getType().getRealDouble());
				//System.out.println("Coords: " + MathLib.printCoordinates(curr));
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
		
		//System.out.println("###Outer###");
		//System.out.println("rad + 1: " + (rad + 1) + ", Outer rad: " + outerRad);
		while (roiOuter.hasNext()) {
			roiOuter.next();
			cursorOuter.getPosition(curr);
			if (isInRing(origin, curr, rad + 1.0, outerRad)) {
				outerRadiusValues.add(roiOuter.getType().getRealDouble());
				//System.out.println("Coords: " + MathLib.printCoordinates(curr));
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

	private boolean isInRing(double[] origin, int[] coords, double innerBorder, double outerBorder) {
		double euclDist = 0;
		for (int i = 0; i < origin.length; i++) {
			euclDist += Math.pow(origin[i] - (double) coords[i], 2);
		}
		euclDist = Math.sqrt(euclDist);
 		return euclDist >= innerBorder && euclDist <= outerBorder;
	}
	
	private double[] convertDownsampledImgCoordsToOriginalCoords(double downsizedCoords[]) {
		double scaledCoords[] = new double[downsizedCoords.length];
		for (int i = 0; i < downsizedCoords.length; i++) {
			scaledCoords[i] = downsizedCoords[i] * downsampleFactors[i];
		}
		return scaledCoords;
	}
}
