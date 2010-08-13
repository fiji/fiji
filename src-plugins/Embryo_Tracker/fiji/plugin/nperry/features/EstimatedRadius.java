package fiji.plugin.nperry.features;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.Ball3DCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class EstimatedRadius <T extends RealType<T>> extends BlobContrast<T> {

	private static final double MIN_DIAMETER_RATIO = 0.4;
	private static final double MAX_DIAMETER_RATIO = 2;
	
	/** The number of different diameters to try. */
	private int nDiameters;

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
	public EstimatedRadius(Image<T> originalImage, double diameter, int nDiameters,  double[] calibration) {
		super(originalImage, diameter, calibration);
		this.nDiameters = nDiameters;
	}

	private static final Feature FEATURE = Feature.ESTIMATED_DIAMETER;
	
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
		
		// Get diameter array
		double[] diameters = prepareDiameters(diam, nDiameters);
		
		// Calculate total intensity in balls
		Ball3DCursor<T> cursor;
		double[] total_intensities = new double[diameters.length + 1];
		total_intensities[0] = 0;
		for (int i = 1; i <= diameters.length; i++) {
			cursor = new Ball3DCursor<T>(img, spot.getCoordinates(), diameters[i-1]/2, calibration);
			total_intensities[i] = 0;
			while(cursor.hasNext()) 
				total_intensities[i] += cursor.next().getRealDouble();
			cursor.close();
		}
		
		// Calculate diff intensities -> will get intensities in rings
		double[] diff_intensities = new double[diameters.length];
		for (int i = 0; i < diff_intensities.length; i++) 
			diff_intensities[i] = total_intensities[i+1] - total_intensities[i];
		
		// Prepare radius array
		double[] radiuses = new double[diameters.length+1];
		radiuses[0] = 0;
		for (int i = 1; i <= diameters.length; i++) {
			radiuses[i] = diameters[i-1];
		}

		// Calculate mean intensities from ring volumes
		double[] mean_intensities = new double[diameters.length];
		for (int i = 0; i < mean_intensities.length; i++) {
			mean_intensities[i] = diff_intensities[i] / 
				( 4/3.0 * Math.PI * 
						(radiuses[i+1]*radiuses[i+1]*radiuses[i+1] - radiuses[i]*radiuses[i]*radiuses[i]) ); 
		}
		
		// Calculate contrasts as minus difference between outer and inner rings mean intensity
		double[] contrasts = new double[diameters.length - 1];
		for (int i = 0; i < contrasts.length; i++) {
			contrasts[i] = - ( mean_intensities[i+1] - mean_intensities[i] );
//			System.out.println(String.format("For diameter %.1f, found constrat of %.1f", diameters[i], contrasts[i])); 
		}
		
		// Find max contrast
		double maxConstrast = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int i = 0; i < contrasts.length; i++) {
			if (contrasts[i] > maxConstrast) {
				maxConstrast = contrasts[i];
				maxIndex = i;
			}
		}
		
		double bestDiameter;
		if ( 0 == maxIndex || contrasts.length-1 == maxIndex) {
			bestDiameter = diameters[maxIndex];
		} else {
			bestDiameter = quadratic1DInterpolation(
					diameters[maxIndex-1], contrasts[maxIndex-1],
					diameters[maxIndex], contrasts[maxIndex],
					diameters[maxIndex+1], contrasts[maxIndex+1]);
		}
		spot.addFeature(FEATURE, bestDiameter);		
	}

	@Override
	protected double getContrast(Spot spot, double diameter) {
		
		return 0;
	}
	
	
	private static final double quadratic1DInterpolation(double x1, double y1, double x2, double y2, double x3, double y3) {
		final double d2 = 2 * ( (y3-y2)/(x3-x2) - (y2-y1)/(x2-x1) ) / (x3-x1);
		if (d2==0)
			return x2;
		else {
			final double d1 = (y3-y2)/(x3-x2) - d2/2 * (x3-x2);
			return x2 -d1/d2;
		}
	}
	
	private static final double[] prepareDiameters(double centralDiameter, int nDiameters) {
		final double[] diameters = new double[nDiameters];
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
		Spot s1 = new Spot(new double[] {100, 100, 100});
		Spot s2 = new Spot(new double[] {100, 100, 200});
		Spot s3 = new Spot(new double[] {100, 100, 300});
		Spot[] spots = new Spot[] {s1, s2, s3};
		double[] radiuses = new double[] {12, 20, 32};
		double[] calibration = new double[] {1, 1, 1};
		
		// Create 3 spots image
		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
					new UnsignedByteType(),
					new ArrayContainerFactory()
				).createImage(new int[] {200, 200, 400});

		Ball3DCursor<UnsignedByteType> cursor;
		int index = 0;
		for (Spot s : spots) {
			cursor = new Ball3DCursor<UnsignedByteType>(
					testImage,
					s.getCoordinates(),
					radiuses[index],
					calibration);
			while (cursor.hasNext())
				cursor.next().set(on);
			cursor.close();
			index++;			
		}
				
//		ij.ImageJ.main(args);
//		ij.ImagePlus imp = mpicbg.imglib.image.display.imagej.ImageJFunctions.copyToImagePlus(testImage);
//		imp.show();
		
		// Apply the estimator
		EstimatedRadius<UnsignedByteType> es = new EstimatedRadius<UnsignedByteType>(
				testImage, 
				40, 
				40, 
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
