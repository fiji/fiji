package fiji.plugin.nperry.features;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

import ij.ImagePlus;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
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
		final double[] diameters = prepareDiameters(diam, nDiameters);
		final double[] contrasts = new double[nDiameters];
		// Calculate contrasts
		for (int i = 0; i < contrasts.length; i++) {
			contrasts[i] = getContrast(spot, diameters[i]);
			System.out.println(String.format("For diameter = %.1f, contrast = %.1f", diameters[i], contrasts[i])); // DEBUG
		}
		// Interpolate max
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
	
	
	private static final ArrayList<int[]> createPositionsInCircle(int[] center, int rmax) {
		final ArrayList<int[]> positions = new ArrayList<int[]>( (int) (4/3.0 *Math.PI * rmax *rmax*rmax ) );
		final int cx = center[0];
		final int cy = center[1];
		final int cz = center[2];
		
		double ox2, oz2;
		int iz, iy, ix, ry, rz ;
		
		/*
		 *  Middle Z
		 */
				
		// Middle Z - Middle Y - All Xs
		for (ix = -rmax; ix <= rmax; ix++) 			
			positions.add(new int[] { cx + ix, cy, cz});
		
		// Middle Z - Other Ys - All Xs
		for (iy = 1; iy <= rmax; iy++) {
			
			// Middle Z - Other Ys - Middle X
			positions.add(new int[] { cx, cy + iy, cz});
			positions.add(new int[] { cx, cy - iy, cz});
			
			ox2 = rmax*rmax - iy*iy;
			ry = (int) Math.sqrt(ox2);
			
			// Middle Z - Other Ys - Other Xs			
			for (ix = 1; ix <= ry; ix++) {
			
				positions.add(new int[] { cx + ix, cy + iy, cz});
				positions.add(new int[] { cx - ix, cy + iy, cz});
				positions.add(new int[] { cx + ix, cy - iy, cz});
				positions.add(new int[] { cx - ix, cy - iy, cz});
				
			}
		}
		
		/*
		 *  Other Zs
		 */
		
		for (iz = 1; iz <= rmax; iz++) {

			oz2 = rmax*rmax - iz*iz;
			rz = (int) Math.sqrt(oz2);

			// Other Zs - Middle Y - All Xs
			for (ix = -rz; ix <= rz; ix++) {
				positions.add(new int[] { cx + ix, cy, cz + iz});
				positions.add(new int[] { cx + ix, cy, cz - iz});
			}
			
			for (iy = 1; iy <= rz; iy++) {
		
				ox2 = rz*rz - iy*iy;
				ry = (int) Math.sqrt(ox2);
				
				// Other Zs - Other Ys - Middle X
				positions.add(new int[] { cx, cy + iy, cz + iz});
				positions.add(new int[] { cx, cy + iy, cz - iz});
				positions.add(new int[] { cx, cy - iy, cz + iz});
				positions.add(new int[] { cx, cy - iy, cz - iz});
				
				// Other Zs - Other Ys - Other Xs
				for (ix = 1; ix <= ry; ix++) {
				
					positions.add(new int[] { cx + ix, cy + iy, cz + iz});
					positions.add(new int[] { cx - ix, cy + iy, cz + iz});
					positions.add(new int[] { cx + ix, cy - iy, cz + iz});
					positions.add(new int[] { cx - ix, cy - iy, cz + iz});
					
					positions.add(new int[] { cx + ix, cy + iy, cz - iz});
					positions.add(new int[] { cx - ix, cy + iy, cz - iz});
					positions.add(new int[] { cx + ix, cy - iy, cz - iz});
					positions.add(new int[] { cx - ix, cy - iy, cz - iz});
					
				}
			}
		}
		
		return positions;
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
	
	private static final double distance(final double[] orig, final int[] coords) {
		double dist = 0;
		for (int i = 0; i < coords.length; i++) 
			dist += ( coords[i] - orig[i] ) * ( coords[i] - orig[i] ); 
		return Math.sqrt(dist);
	}
	
	
	
	/**
	 * For testing purposes
	 */
	public static void main(String[] args) {
		
		ij.ImageJ.main(args);
		
		final byte on = (byte) 255;
		final byte off = (byte) 0;
		Spot s1 = new Spot(new double[] {40, 40, 20});
		Spot s2 = new Spot(new double[] {40, 40, 60});
		Spot s3 = new Spot(new double[] {40, 40, 120});
//		Spot[] spots = new Spot[] {s2};
//		double[] radiuses = new double[] {10 };
		Spot[] spots = new Spot[] {s1, s2, s3};
		double[] radiuses = new double[] {12, 20, 32};
		
		// Create 3 spots image
		Image<UnsignedByteType> testImage = new ImageFactory<UnsignedByteType>(
					new UnsignedByteType(),
					new ArrayContainerFactory()
				).createImage(new int[] {80, 80, 160});

		LocalizableByDimCursor<UnsignedByteType> lbdc = testImage.createLocalizableByDimCursor();
		ArrayList<int[]> positions = createPositionsInCircle(new int[] {40, 40, 40}, 20);
		for(int[] pos : positions) {
			lbdc.setPosition(pos);
			lbdc.getType().inc();
		}
		lbdc.close();
		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.show();
		
		
		/*
		LocalizableCursor<UnsignedByteType> cursor = testImage.createLocalizableCursor();
		int[] position = new int[3];

		Spot s;
		double r;
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.getPosition(position);
			for (int i = 0; i < spots.length; i++) {
				s = spots[i];
				r = radiuses[i];
				if (distance(s.getCoordinates(), position) < r) {
					cursor.getType().set(on);
					break;
				} else {
					cursor.getType().set(off);
				}
				
			}
		}
		cursor.close();
		
		ImagePlus imp = ImageJFunctions.copyToImagePlus(testImage);
		imp.show();
		
		// Apply the estimator
		EstimatedRadius<UnsignedByteType> es = new EstimatedRadius<UnsignedByteType>(
				testImage, 
				40, 
				10, 
				new double[] {1, 1, 1});
		for (int i = 0; i < spots.length; i++) {
			s = spots[i];
			r = radiuses[i];
			es.process(s);
			System.out.println(String.format("For spot %d, found diameter %.1f, real value was %.1f.", i, s.getFeatures().get(FEATURE), 2*r));
		}
		
		*/
		
	}
}
