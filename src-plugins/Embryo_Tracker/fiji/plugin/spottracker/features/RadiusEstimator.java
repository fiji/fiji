package fiji.plugin.spottracker.features;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import fiji.plugin.spottracker.Featurable;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;

public class RadiusEstimator <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	private static final float MIN_DIAMETER_RATIO = 0.4f;
	private static final float MAX_DIAMETER_RATIO = 2;
	
	/** The number of different diameters to try. */
	private int nDiameters;
	private Image<T> img;
	private float diam;
	private float[] calibration;
	/** Utility holder. */
	private float[] coords;

	/**
	 * Create a feature analyzer that will return the best estimated diameter for a 
	 * spot. Estimated diameter is obtained by finding the diameter that gives the 
	 * maximum contrast, as calculated by the {@link BlobContrast} feature analyzer.
	 * Searched diameters are linearly spread between <code>diameter</code> * {@value #MIN_DIAMTER_RATIO}
	 * and <code>diameter</code> * {@value #MAX_DIAMETER_RATIO}. The optimum is them calculated by doing an interpolation
	 * over calculated values.
	 *  
	 * @param originalImage  the image to get data from 
	 * @param diameter  the diameter scale to search around
	 * @param nDiameters  the number of different diameter to compute
	 * @param calibration  the spatial calibration array containing the pixel size in X, Y, Z
	 */
	public RadiusEstimator(Image<T> originalImage, float diameter, int nDiameters,  float[] calibration) {
		this.img = originalImage;
		this.diam = diameter;
		this.nDiameters = nDiameters;
		this.calibration = calibration;
		this.coords = new float[img.getNumDimensions()];
	}

	private static final Feature FEATURE = Feature.ESTIMATED_DIAMETER;
	
	@Override
	public Feature getFeature() {
		return FEATURE;
	}

	@Override
	public void process(Featurable spot) {
		for (int i = 0; i < coords.length; i++)
			coords[i] = spot.getFeature(Featurable.POSITION_FEATURES[i]);

		// Get diameter array and radius squared
		final float[] diameters = prepareDiameters(diam, nDiameters);
		final float[] r2 = new float[nDiameters];
		for (int i = 0; i < r2.length; i++) 
			r2[i] = diameters[i] * diameters[i] / 4 ;
		
		// Calculate total intensity in balls
		final float[] ring_intensities = new float[nDiameters];
		final int[]    ring_volumes = new int[nDiameters];

		final DomainCursor<T> cursor;
		if (img.getNumDimensions() == 3)
			cursor = new SphereCursor<T>(img, coords, diameters[nDiameters-2]/2, calibration);
		else
			cursor = new DiscCursor<T>(img, coords, diameters[nDiameters-2]/2, calibration);
		double d2;
		int i;
		while(cursor.hasNext())  {
			cursor.fwd();
			d2 = cursor.getDistanceSquared();
			for(i = 0 ; i < nDiameters-1 && d2 > r2[i] ; i++) {}
			ring_intensities[i] += cursor.getType().getRealDouble();
			ring_volumes[i]++;
		}

		// Calculate mean intensities from ring volumes
		final float[] mean_intensities = new float[diameters.length];
		for (int j = 0; j < mean_intensities.length; j++) 
			mean_intensities[j] = ring_intensities[j] / ring_volumes[j];
		
		// Calculate contrasts as minus difference between outer and inner rings mean intensity
		final float[] contrasts = new float[diameters.length - 1];
		for (int j = 0; j < contrasts.length; j++) {
			contrasts[j] = - ( mean_intensities[j+1] - mean_intensities[j] );
//			System.out.println(String.format("For diameter %.1f, found contrast of %.1f", diameters[j], contrasts[j])); 
		}
		
		// Find max contrast
		float maxConstrast = Float.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int j = 0; j < contrasts.length; j++) {
			if (contrasts[j] > maxConstrast) {
				maxConstrast = contrasts[j];
				maxIndex = j;
			}
		}
		
		float bestDiameter;
		if ( 1 >= maxIndex || contrasts.length-1 == maxIndex) {
			bestDiameter = diameters[maxIndex];
		} else {
			bestDiameter = quadratic1DInterpolation(
					diameters[maxIndex-1], contrasts[maxIndex-1],
					diameters[maxIndex], contrasts[maxIndex],
					diameters[maxIndex+1], contrasts[maxIndex+1]);
		}
		spot.putFeature(FEATURE, bestDiameter);		
	}
	
	private static final float quadratic1DInterpolation(float x1, float y1, float x2, float y2, float x3, float y3) {
		final float d2 = 2 * ( (y3-y2)/(x3-x2) - (y2-y1)/(x2-x1) ) / (x3-x1);
		if (d2==0)
			return x2;
		else {
			final float d1 = (y3-y2)/(x3-x2) - d2/2 * (x3-x2);
			return x2 -d1/d2;
		}
	}
	
	private static final float[] prepareDiameters(float centralDiameter, int nDiameters) {
		final float[] diameters = new float[nDiameters];
		for (int i = 0; i < diameters.length; i++) {
			diameters[i] = centralDiameter * ( MIN_DIAMETER_RATIO   
				+ i * (MAX_DIAMETER_RATIO - MIN_DIAMETER_RATIO)/(nDiameters-1) );
		}
		return diameters;
	}
	
	
	/**
	 * For testing purposes
	 */
	public static void main(String[] args) {
		
		final byte on = (byte) 255;
		Spot s1 = new Spot(new float[] {100, 100, 100});
		Spot s2 = new Spot(new float[] {100, 100, 200});
		Spot s3 = new Spot(new float[] {100, 100, 300});
		Spot[] spots = new Spot[] {s1, s2, s3};
		float[] radiuses = new float[]  {12, 20, 32};
		float[] calibration = null; //new float[] {1, 1, 1};
		
		// Create 3 spots image
		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
					new UnsignedByteType(),
					new ArrayContainerFactory()
				).createImage(new int[] {200, 200, 400});

		SphereCursor<UnsignedByteType> cursor;
		int index = 0;
		for (Spot s : spots) {
			cursor = new SphereCursor<UnsignedByteType>(
					testImage,
					s.getPosition(null),
					radiuses[index],
					calibration);
			while (cursor.hasNext())
				cursor.next().set(on);
			cursor.close();
			index++;			
		}
				
		ij.ImageJ.main(args);
		ij.ImagePlus imp = mpicbg.imglib.image.display.imagej.ImageJFunctions.copyToImagePlus(testImage);
		imp.show();
		
		// Apply the estimator
		RadiusEstimator<UnsignedByteType> es = new RadiusEstimator<UnsignedByteType>(
				testImage, 
				40.5f, 
				20, 
				calibration);
		
		Spot s;
		double r;
		long start, stop;
		for (int i = 0; i < spots.length; i++) {
			s = spots[i];
			r = radiuses[i];
			start = System.currentTimeMillis();
			es.process(s);
			stop = System.currentTimeMillis();
			System.out.println(String.format("For spot %d, found diameter %.1f, real value was %.1f.", i, s.getFeatures().get(FEATURE), 2*r));
			System.out.println("Computing time: "+(stop-start)+" ms.");
		}
	}
}
