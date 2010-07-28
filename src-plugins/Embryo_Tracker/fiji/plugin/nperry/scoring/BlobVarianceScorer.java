package fiji.plugin.nperry.scoring;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;

public class BlobVarianceScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "BlobVarianceScorer";
	private Image<T> img;
	private double diam;
	private double[] downsampleFactors;
	
	public BlobVarianceScorer(Image<T> originalImage, double diam) {
		this.img = originalImage;
		this.diam = diam;
		this.downsampleFactors = new double[originalImage.getNumDimensions()];
		
		for (int i = 0; i < downsampleFactors.length; i++) {
			downsampleFactors[i] = 1.0;
		}
	}
	
	public BlobVarianceScorer(Image<T> originalImage, double diam, double[] downsampleFactors) {
		this.img = originalImage;
		this.diam = diam;
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
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		final double[] coords = spot.getCoordinates();
		final double[] scaledCoords = convertDownsampledImgCoordsToOriginalCoords(coords);
		final int[] intCoords = doubleCoordsToIntCoords(scaledCoords);
		final ArrayList<Double> values = new ArrayList<Double>();
		
		// Calculate inscribed cube length for blob
		double length = diam / Math.sqrt(2);
		
		// Create the size array for the ROI cursor
		int size[] = new int[img.getNumDimensions()];
		for (int i = 0; i < size.length; i++) {
			size[i] = (int) length;
		}

		// Adjust the integer coordinates of the spot to set the ROI correctly
		for (int i = 0; i < intCoords.length; i++) {
			intCoords[i] = intCoords[i] - (int) (length / 2);  
		}
		
		// Use ROI cursor to search a sphere around the spot's coordinates
		/* need to handle case where ROI is not in image anymore!! */
		RegionOfInterestCursor<T> roi = cursor.createRegionOfInterestCursor(intCoords, size);
		while (roi.hasNext()) {
			roi.next();
			values.add(roi.getType().getRealDouble());
		}
		
		// Compute variance for this blob.
		double[] valuesArr = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			valuesArr[i] = values.get(i).doubleValue();
		}
		double avg = MathLib.computeAverage(valuesArr);
		double var = 0;
		for (int j = 0; j < valuesArr.length; j++) {
			var += Math.pow(valuesArr[j] - avg, 2);
		}
		var /= valuesArr.length;

		// Close cursors
		roi.close();
		cursor.close();
		
		// Add variance as the spot's score. Apply inverse so lower variance receives a higher score.
		spot.addScore(SCORING_METHOD_NAME, 1/var);
	}
	
	private double[] convertDownsampledImgCoordsToOriginalCoords(double downsizedCoords[]) {
		double scaledCoords[] = new double[downsizedCoords.length];
		for (int i = 0; i < downsizedCoords.length; i++) {
			scaledCoords[i] = downsizedCoords[i] * downsampleFactors[i];
		}
		return scaledCoords;
	}
	
	private int[] doubleCoordsToIntCoords(double doubleCoords[]) {
		int intCoords[] = new int[doubleCoords.length];
		for (int i = 0; i < doubleCoords.length; i++) {
			intCoords[i] = (int) Math.round(doubleCoords[i]);
		}
		return intCoords;
	}

}
