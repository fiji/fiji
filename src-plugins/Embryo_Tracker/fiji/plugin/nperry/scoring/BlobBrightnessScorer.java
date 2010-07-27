package fiji.plugin.nperry.scoring;

import java.util.ArrayList;
import java.util.Iterator;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Spot;

public class BlobBrightnessScorer <T extends RealType<T>> extends IndependentScorer {

	private static final String SCORING_METHOD_NAME = "BlobVarianceScorer";
	private Image<T> img;
	private int rad;
	
	public BlobBrightnessScorer(Image<T> filteredImage, double diam) {
		this.img = filteredImage;
		
		this.rad = (int) MathLib.round(diam/2);
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
		final LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor();
		final double[] coords = spot.getCoordinates();
		final int[] intCoords = new int[coords.length];
		final ArrayList<Double> values = new ArrayList<Double>();
		
		// Convert spot's coords (double[]) to int[]
		for (int i = 0; i < intCoords.length; i++) {
			intCoords[i] = (int) Math.round(coords[i]) - rad;
		}
		
		// Create the size array for the ROI cursor
		int size[] = new int[img.getNumDimensions()];
		for (int i = 0; i < size.length; i++) {
			size[i] = rad * 2;
		}

		// Use ROI cursor to search a sphere around the spot's coordinates, and store the brightness of each pixel
		RegionOfInterestCursor<T> roi = cursor.createRegionOfInterestCursor(intCoords, size);
		while (roi.hasNext()) {
			roi.next();
			values.add(roi.getType().getRealDouble());
		}
		
		// Compute the brightness for this blob.
		double brightness = 0;
		Iterator<Double> itr = values.iterator();
		while (itr.hasNext()) {
			brightness += itr.next().doubleValue();
		}
		
		// Close cursors
		roi.close();
		cursor.close();
		
		// Add total brightness as the spot's score.
		spot.addScore(SCORING_METHOD_NAME, brightness);
	}

}