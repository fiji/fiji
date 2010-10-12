package fiji.plugin.trackmate.features;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.SpotImp;

public class BlobDescriptiveStatistics <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	/*
	 * FIELDS
	 */
	
	/** The original image that is analyzed. */
	private Image<T> img;
	/** The number of pixels in the sphere or disc that will be iterated through tp build statistics. */
	private int npixels;
	private DomainCursor<T> cursor;
	/** Utility holder. */
	private float[] coords;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public BlobDescriptiveStatistics(Image<T> originalImage, float radius, float[] calibration) {
		this.img = originalImage;
		if (img.getNumDimensions() == 3) {
			this.cursor = new SphereCursor<T>(img, new float[3], radius, calibration);
			coords = new float[3];
		} else { 
			this.cursor = new DiscCursor<T>(img, new float[2], radius, calibration);
			coords = new float[2];
		}
		this.npixels = cursor.getNPixels();
	}

	public BlobDescriptiveStatistics(Image<T> originalImage, float radius) {
		this(originalImage, radius, originalImage.getCalibration());
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public Feature getFeature() {
		return Feature.MEAN_INTENSITY;
	}

	/**
	 * Compute descriptive statistics items for this spot. Implementation follows
	 * {@link http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance}.
	 */
	@Override
	public void process(Spot spot) {
		// For variance, kurtosis and skewness 
		float sum = 0;
		float sum_sqr = 0;
		
		float mean = 0;
	    float M2 = 0;
	    float M3 = 0;
	    float M4 = 0;
	    float delta, delta_n, delta_n2;
	    float term1;
	    int n1;
		
	    // Others
		float val;
		final float[] pixel_values = new float[npixels];
		int n = 0;
		
		for (int i = 0; i < coords.length; i++)
			coords[i] = spot.getFeature(Spot.POSITION_FEATURES[i]);
		
		cursor.moveCenterToCoordinates(coords);
		while (cursor.hasNext()) {
			cursor.next();
			val = cursor.getType().getRealFloat();
			// For median, min and max
			pixel_values[n] = val;
			
			// For variance and mean
			sum += val;
			sum_sqr += val*val;
			
			// For kurtosis
			n1 = n;
			n++;
			delta = val - mean;
			delta_n = delta / n;
			delta_n2 = delta_n * delta_n;
			term1 = delta * delta_n * n1;
			mean = mean + delta_n;
			M4 = M4 + term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
	        M3 = M3 + term1 * delta_n * (n - 2) - 3 * delta_n * M2;
	        M2 = M2 + term1;
		}

		MathLib.quicksort(pixel_values, 0, npixels-1);
		float median = pixel_values[npixels/2];
		float min = pixel_values[0];
		float max = pixel_values[npixels-1];
		mean = sum / npixels;
		float variance = M2 / (npixels-1);
		float kurtosis = (n*M4) / (M2*M2) - 3;
		float skewness = (float) ( Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0) );
		
		spot.putFeature(Feature.MEDIAN_INTENSITY, median);
		spot.putFeature(Feature.MIN_INTENSITY, min);
		spot.putFeature(Feature.MAX_INTENSITY, max);
		spot.putFeature(Feature.MEAN_INTENSITY, mean);
		spot.putFeature(Feature.VARIANCE, variance);
		spot.putFeature(Feature.STANDARD_DEVIATION, (float) Math.sqrt(variance));
		spot.putFeature(Feature.TOTAL_INTENSITY, sum);
		spot.putFeature(Feature.KURTOSIS, kurtosis);
		spot.putFeature(Feature.SKEWNESS, skewness);
	}
		
	/*
	 * MAIN METHOD
	 */

	public static void main(String[] args) {

		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {200, 200, 40} );
		
		float[] center = new float[]  {20, 20, 20};
		SpotImp s1 = new SpotImp(center);
		float radius = 5;
		s1.setName("Test spot with radius = "+radius);

		float[] calibration = new float[] {0.2f, 0.2f, 1};
		SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(
				testImage, 
				s1.getPosition(null), 
				radius, // µm
				calibration);
		while(cursor.hasNext()) {
			cursor.fwd();
			if (new java.util.Random().nextBoolean()) cursor.getType().set(1);
//			cursor.getType().set(new java.util.Random().nextInt(101));
//			cursor.getType().set((int) (100 + new java.util.Random().nextGaussian()*10));
		}
		cursor.close();
		
		BlobDescriptiveStatistics<UnsignedByteType> bb = new BlobDescriptiveStatistics<UnsignedByteType>(testImage, 2*radius, calibration);
		bb.process(s1);
		System.out.println(s1);
		System.out.println("Volume parsed: "+cursor.getNPixels());
		
	}
}