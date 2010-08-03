package fiji.plugin.nperry.scoring;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;

public class BlobBrightnessScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "BlobBrightnessScorer";
	private Image<T> img;
	private double diam;
	private double[] calibration;
	
	public BlobBrightnessScorer(Image<T> originalImage, double diam, double[] calibration) {
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
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());
		final double[] coords = spot.getCoordinates();
		final int[] intCoords = doubleCoordsToIntCoords(coords);

		// Create the size array for the ROI cursor
		int size[] = new int[img.getNumDimensions()];
		for (int i = 0; i < size.length; i++) {
			size[i] = (int) (diam / calibration[i]);
		}

		// Adjust the integer coordinates of the spot to set the ROI correctly
		int[] roiCoords = new int[img.getNumDimensions()];
		for (int i = 0; i < intCoords.length; i++) {
			roiCoords[i] = intCoords[i] - (int) (size[i] / 2);  
		}
		
		// Use ROI cursor to search a sphere around the spot's coordinates
		/* need to handle case where ROI is not in image anymore!! */
		double sum = 0;
		RegionOfInterestCursor<T> roi = cursor.createRegionOfInterestCursor(roiCoords, size);
		while (roi.hasNext()) {
			roi.next();
			if (inBlob(cursor.getPosition(), intCoords)) {
				sum += roi.getType().getRealDouble();
			}
		}

		// Close cursors
		roi.close();
		cursor.close();
		
		// Add total intensity.
		spot.addScore(SCORING_METHOD_NAME, sum);
	}
	
	private boolean inBlob(int[] pos, int[] center) {
		for (int i = 0; i < pos.length; i++) {
			if (Math.abs((double) pos[i] - (double) center[i]) / (diam / calibration[i] / 2) > 1) return false; 
		}
		return true;
	}
	
	private int[] doubleCoordsToIntCoords(double doubleCoords[]) {
		int intCoords[] = new int[doubleCoords.length];
		for (int i = 0; i < doubleCoords.length; i++) {
			intCoords[i] = (int) Math.round(doubleCoords[i]);
		}
		return intCoords;
	}

}