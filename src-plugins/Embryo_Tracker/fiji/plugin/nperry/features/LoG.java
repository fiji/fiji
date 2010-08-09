package fiji.plugin.nperry.features;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class LoG <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final Feature FEATURE = Feature.LOG_VALUE;
	private Image<T> img;
	private LocalizableByDimCursor<T> cursor;
	private double downsampleFactors[];

	public LoG(Image<T> filteredImage) {
		this.img = filteredImage;
		this.cursor = img.createLocalizableByDimCursor();
		this.downsampleFactors = new double[filteredImage.getNumDimensions()];
		
		for (int i = 0; i < downsampleFactors.length; i++) {
			downsampleFactors[i] = 1;
		}
	}
	
	public LoG(Image<T> filteredImage, double downsampleFactors[]) {
		this.img = filteredImage;
		this.cursor = img.createLocalizableByDimCursor();
		this.downsampleFactors = downsampleFactors;
	}

	@Override
	public Feature getFeature() {
		return FEATURE;
	}

	@Override
	public void process(Spot spot) {
		final double[] coords = spot.getCoordinates();
		final double[] scaledCoords = toDownsampledCoords(coords);
		final int[] intCoords = doubleCoordsToIntCoords(scaledCoords);
		
		cursor.setPosition(intCoords);
		//spot.addScore(FEATURE_NAME, cursor.getType().getRealDouble());
		spot.addFeature(FEATURE, cursor.getType().getRealDouble());
	}
	
	@Override
	public boolean isNormalized() {
		return false;
	}

	private double[] toDownsampledCoords(double downsizedCoords[]) {
		double scaledCoords[] = new double[downsizedCoords.length];
		for (int i = 0; i < downsizedCoords.length; i++) {
			scaledCoords[i] = downsizedCoords[i] / downsampleFactors[i];
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
