package fiji.plugin.trackmate.features.spot;

import java.util.Arrays;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.SpotImp;

/**
 * This {@link SpotFeatureAnalyzer} computes morphology features for the given spots. 
 * <p>
 * It estimates shape parameters by computing the most resembling ellipsoid from the pixels
 * contained within the spot radius. From this ellipsoid, it determines what are its semi-axes lengths,
 * and their orientation.
 * <p>
 * In the 3D case, the features ELLIPSOIDFIT_SEMIAXISLENGTH_* contains the semi-axes lengths, ordered from 
 * the largest (A) to the smallest (C). ELLIPSOIDFIT_AXISPHI_* and ELLIPSOIDFIT_AXISTHETA_* give the 
 * orientation angles of the corresponding ellipsoid axis, in spherical coordinates. Angles are expressed
 * in radians. 
 * <ul>
 * 	<li>φ is the azimuth int the XY plane and its range is ]-π/2 ; π/2]  
 * 	<li>ϑ is the elevation with respect to the Z axis and ranges from 0 to π
 * </ul>
 * <p>
 * In the 2D case, ELLIPSOIDFIT_SEMIAXISLENGTH_A and ELLIPSOIDFIT_AXISPHI_A are always 0, the THETA angles are 0, and  
 * ELLIPSOIDFIT_AXISPHI_B and ELLIPSOIDFIT_AXISPHI_C differ by π/2.
 * <p>
 * From the semi-axis length, a morphology index is computed, echoed in the {@link SpotFeature#MORPHOLOGY} feature.
 * Spots are classified according to the shape of their most-resembling ellipsoid. We look for equality between
 * semi-axes, with a certain tolerance, which value is {@link #SIGNIFICANCE_FACTOR}. 
 * <p>
 * In the 2D case, if b > c are the semi-axes length
 * <ul>
 * 	<li> if b ≅ c, then this index has the value {@link #SPHERE}
 * 	<li> otherwise, it has the value {@link #PROLATE}
 * </ul>
 * <p>
 * In the 2D case, if a > b > c are the semi-axes length
 * <ul>
 * 	<li> if a ≅ b ≅ c, then this index has the value {@link #SPHERE}
 * 	<li> if a ≅ b > c, then this index has the value {@link #OBLATE}: the spot resembles a flat disk
 * 	<li> if a > b ≅ c, then this index has the value {@link #PROLATE}: the spot resembles a rugby ball
 * 	<li> otherwise it has the value {@link #SCALENE}; the spot's shape has nothing particular
 * </ul>
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 1, 2011
 *
 * @param <T>  the type of the input {@link Image}
 */
public class BlobMorphology <T extends RealType<T>> extends IndependentSpotFeatureAnalyzer {

	/*
	 * Fields
	 */
	
	private final static SpotFeature[] featurelist_sa 	= {SpotFeature.ELLIPSOIDFIT_SEMIAXISLENGTH_C, 	SpotFeature.ELLIPSOIDFIT_SEMIAXISLENGTH_B, 	SpotFeature.ELLIPSOIDFIT_SEMIAXISLENGTH_A};
	private final static SpotFeature[] featurelist_phi 	= {SpotFeature.ELLIPSOIDFIT_AXISPHI_C, 			SpotFeature.ELLIPSOIDFIT_AXISPHI_B, 		SpotFeature.ELLIPSOIDFIT_AXISPHI_A };
	private final static SpotFeature[] featurelist_theta = {SpotFeature.ELLIPSOIDFIT_AXISTHETA_C, 		SpotFeature.ELLIPSOIDFIT_AXISTHETA_B, 		SpotFeature.ELLIPSOIDFIT_AXISTHETA_A}; 
	/** The Feature characteristics this class computes. */
	private static final SpotFeature FEATURE = SpotFeature.MORPHOLOGY;
	
	/** Spherical shape, that is roughly a = b = c. */
	public static final int SPHERE = 0;
	/** Oblate shape, disk shaped, that is roughly a = b > c. */
	public static final int OBLATE = 1;
	/** Prolate shape, rugby ball shape, that is roughly a = b < c. */
	public static final int PROLATE = 2;
	/** Scalene shape, nothing particular, a > b > c. */
	public static final int SCALENE = 3;
	
	/** Significance factor to determine when a semiaxis length should be
	 *  considered significantly larger than the others. */
	private static final double SIGNIFICANCE_FACTOR = 1.2;
	
	/** The image to extract the Feature characteristics from. */
	private Image<T> img;
	/** The Image calibration information. */
	private float[] calibration;
	/** Utility holder. */
	private float[] coords;
	
	/*
	 * CONSTRUCTORS
	 */
	
	
	public BlobMorphology (Image<T> img, float[] calibration) {
		this.img = img;
		this.calibration = calibration;
		this.coords = new float[img.getNumDimensions()];
	}
	
	
	public BlobMorphology (Image<T> img) {
		this(img, img.getCalibration());
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	
	@Override
	public SpotFeature getFeature() {
		return FEATURE;
	}
	
	
	@Override
	public void process(final Spot spot) {
		final float radius = spot.getFeature(SpotFeature.RADIUS);
		for (int i = 0; i < coords.length; i++) 
			coords[i] = spot.getFeature(Spot.POSITION_FEATURES[i]);

		if (img.getNumDimensions() == 3) {

			// 3D case
			
			final SphereCursor<T> cursor = new SphereCursor<T>(img, coords, radius, calibration);
			double x, y, z;
			double x2, y2, z2;
			double mass, totalmass = 0;
			double Ixx = 0, Iyy = 0, Izz = 0, Ixy = 0, Ixz = 0, Iyz = 0;
			int[] position = cursor.createPositionArray();

			while (cursor.hasNext()) {
				cursor.fwd();
				mass = cursor.getType().getRealDouble();
				cursor.getRelativePosition(position);
				x = position[0] * calibration[0];
				y = position[1] * calibration[1];
				z = position[2] * calibration[2];
				totalmass += mass;
				x2 = x*x;
				y2 = y*y;
				z2 = z*z;
				Ixx += mass*(y2+z2);
				Iyy += mass*(x2+z2);
				Izz += mass*(x2+y2);
				Ixy -= mass*x*y;
				Ixz -= mass*x*z;
				Iyz -= mass*y*z;
			}
			cursor.close();

			Matrix mat = new Matrix( new double[][] { 
					{ Ixx, Ixy, Ixz },
					{ Ixy, Iyy, Iyz },
					{ Ixz, Iyz, Izz } } );
			mat.timesEquals(1/totalmass);
			EigenvalueDecomposition eigdec = mat.eig();
			double[] eigenvalues = eigdec.getRealEigenvalues();
			Matrix eigenvectors = eigdec.getV();

			double I1 = eigenvalues[0];
			double I2 = eigenvalues[1];
			double I3 = eigenvalues[2];
			double a = Math.sqrt( 2.5 *(I2+I3-I1) );
			double b = Math.sqrt( 2.5 *(I3+I1-I2) );
			double c = Math.sqrt( 2.5 *(I1+I2-I3) );
			double[] semiaxes = new double[] {a, b, c};

			// Sort semi-axes by ascendent order and get the sorting index
			double[] semiaxes_ordered = semiaxes.clone();
			Arrays.sort(semiaxes_ordered);
			int[] order = new int[3];
			for (int i = 0; i < semiaxes_ordered.length; i++) 
				for (int j = 0; j < semiaxes.length; j++) 
					if (semiaxes_ordered[i] == semiaxes[j])
						order[i] = j;

			// Get the sorted eigenvalues
			double[][] uvectors = new double[3][3];
			for (int i = 0; i < eigenvalues.length; i++) {
				uvectors[i][0] = eigenvectors.get(0, order[i]);
				uvectors[i][1] = eigenvectors.get(1, order[i]);
				uvectors[i][2] = eigenvectors.get(2, order[i]);
			}

			// Store in the Spot object
			double theta, phi;
			for (int i = 0; i < uvectors.length; i++) {
				theta = Math.acos( uvectors[i][2] / Math.sqrt(
						uvectors[i][0]*uvectors[i][0] +
						uvectors[i][1]*uvectors[i][1] +
						uvectors[i][2]*uvectors[i][2]) );
				phi = Math.atan2(uvectors[i][1], uvectors[i][0]);
				if (phi < - Math.PI/2 ) 
					phi += Math.PI; // For an ellipsoid we care only for the angles in [-pi/2 , pi/2]
				if (phi > Math.PI/2 ) 
					phi -= Math.PI; 

				// Store in descending order
				spot.putFeature(featurelist_sa[i], (float) semiaxes_ordered[i]);
				spot.putFeature(featurelist_phi[i], (float) phi);
				spot.putFeature(featurelist_theta[i], (float) theta);
			}

			// Store the Spot morphology (needs to be outside the above loop)
			spot.putFeature(SpotFeature.MORPHOLOGY, estimateMorphology(semiaxes_ordered));
			
		} else if (img.getNumDimensions() == 2) {
			
			// 2D case
			
			final DiscCursor<T> cursor = new DiscCursor<T>(img, coords, radius, calibration);
			double x, y;
			double x2, y2;
			double mass, totalmass = 0;
			double Ixx = 0, Iyy = 0, Ixy = 0;
			int[] position = cursor.createPositionArray();

			while (cursor.hasNext()) {
				cursor.fwd();
				mass = cursor.getType().getRealDouble();
				cursor.getRelativePosition(position);
				x = position[0] * calibration[0];
				y = position[1] * calibration[1];
				totalmass += mass;
				x2 = x*x;
				y2 = y*y;
				Ixx += mass*(y2);
				Iyy += mass*(x2);
				Ixy -= mass*x*y;
			}
			cursor.close();

			Matrix mat = new Matrix( new double[][] { 
					{ Ixx, Ixy},
					{ Ixy, Iyy} } );
			mat.timesEquals(1/totalmass);
			EigenvalueDecomposition eigdec = mat.eig();
			double[] eigenvalues = eigdec.getRealEigenvalues();
			Matrix eigenvectors = eigdec.getV();

			double I1 = eigenvalues[0];
			double I2 = eigenvalues[1];
			double a = Math.sqrt( 4 * I1 );
			double b = Math.sqrt( 4 * I2 );
			double[] semiaxes = new double[] {a, b};

			// Sort semi-axes by ascendent order and get the sorting index
			double[] semiaxes_ordered = semiaxes.clone();
			Arrays.sort(semiaxes_ordered);
			int[] order = new int[2];
			for (int i = 0; i < semiaxes_ordered.length; i++) 
				for (int j = 0; j < semiaxes.length; j++) 
					if (semiaxes_ordered[i] == semiaxes[j])
						order[i] = j;

			// Get the sorted eigenvalues
			double[][] uvectors = new double[2][2];
			for (int i = 0; i < eigenvalues.length; i++) {
				uvectors[i][0] = eigenvectors.get(0, order[i]);
				uvectors[i][1] = eigenvectors.get(1, order[i]);
			}

			// Store in the Spot object
			double theta, phi;
			for (int i = 0; i < uvectors.length; i++) {
				theta = 0;
				phi = Math.atan2(uvectors[i][1], uvectors[i][0]);
				if (phi < - Math.PI/2 ) 
					phi += Math.PI; // For an ellipsoid we care only for the angles in [-pi/2 , pi/2]
				if (phi > Math.PI/2 ) 
					phi -= Math.PI; 

				// Store in descending order
				spot.putFeature(featurelist_sa[i], (float) semiaxes_ordered[i]);
				spot.putFeature(featurelist_phi[i], (float) phi);
				spot.putFeature(featurelist_theta[i], (float) theta);
			}
			spot.putFeature(featurelist_sa[2], 0);
			spot.putFeature(featurelist_phi[2], 0);
			spot.putFeature(featurelist_theta[2], 0);

			// Store the Spot morphology (needs to be outside the above loop)
			spot.putFeature(SpotFeature.MORPHOLOGY, estimateMorphology(semiaxes_ordered));
			
		}
	}
	
	
	/**
	 * Estimates whether a Spot morphology from the semi-axes lengths of its
	 * most resembling ellipsoid. 
	 * @param semiaxes The semi-axis lengths <b>in ascending order</b>.
	 * @return 1 [Ellipsoid] if any semi-axis length(s) are significantly larger than the other(s). 0 [Spherical] otherwise. 
	 */
	private static final int estimateMorphology(final double[] semiaxes) {
		
		if (semiaxes.length == 2) {
			// 2D case
			double a = semiaxes[0];
			double b = semiaxes[1];
			if (b >= SIGNIFICANCE_FACTOR * a)
				return PROLATE;
			else 
				return SPHERE;
		
		} else {
			// 3D case 
			
			double a = semiaxes[0]; // Smallest
			double b = semiaxes[1];
			double c = semiaxes[2]; // Largest
			
			// Sphere: all equals with respect to significance, that is: the largest semi-axes must not
			// be larger that factor * the smallest
			if (c < SIGNIFICANCE_FACTOR * a)
				return SPHERE;
			
			// Oblate: the 2 largest are equals with respect to significance
			if (c < SIGNIFICANCE_FACTOR * b) 
				return OBLATE;
			
			// Prolate: the 2 smallest are equals with respect to significance
			if (b < SIGNIFICANCE_FACTOR * a)
				return PROLATE;
			
			return SCALENE;
			
		}
		
	}
	
	public static void main(String[] args) {

		// TEST 2D case

		// Parameters
		int size_x = 200;
		int size_y = 200;
		
		float a = 10;
		float b = 5;
		float phi_r = (float) Math.toRadians(30);

		float max_radius = Math.max(a, b);
		float[] calibration = new float[] {1, 1};
		
		// Create blank image
		Image<UnsignedByteType> img = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
			).createImage(new int[] {200, 200});
		final byte on = (byte) 255;
		
		// Create an ellipse 
		long start = System.currentTimeMillis();
		System.out.println(String.format("Creating an ellipse with a = %.1f, b = %.1f", a, b));
		System.out.println(String.format("phi = %.1f", Math.toDegrees(phi_r)));
		float[] center = new float[] { size_x/2, size_y/2, 0 };
		DiscCursor<UnsignedByteType> sc = new DiscCursor<UnsignedByteType>(img, center, max_radius, calibration);
		double r2, phi, term;
		double cosphi, sinphi;
		while (sc.hasNext()) {
			sc.fwd();
			r2 = sc.getDistanceSquared();
			phi = sc.getPhi();
			cosphi = Math.cos(phi-phi_r);
			sinphi = Math.sin(phi-phi_r);
			term = r2*cosphi*cosphi/a/a +
				r2*sinphi*sinphi/b/b; 
			if (term <= 1) 
				sc.getType().set(on);
		}
		sc.close();
		long end = System.currentTimeMillis();
		System.out.println("Ellipse creation done in " + (end-start) + " ms.");
		System.out.println();
		
		ij.ImageJ.main(args);
		img.getDisplay().setMinMax();
		ImageJFunctions.copyToImagePlus(img).show();
		
		start = System.currentTimeMillis();
		BlobMorphology<UnsignedByteType> bm = new BlobMorphology<UnsignedByteType>(img, calibration);
		SpotImp spot = new SpotImp(center);
		spot.putFeature(SpotFeature.RADIUS, max_radius);
		bm.process(spot);
		end = System.currentTimeMillis();
		System.out.println("Blob morphology analyzed in " + (end-start) + " ms.");
		float phiv, thetav, lv;
		for (int j = 0; j < 2; j++) {
			lv = spot.getFeature(featurelist_sa[j]);
			phiv = spot.getFeature(featurelist_phi[j]);
			thetav = spot.getFeature(featurelist_theta[j]);
			System.out.println(String.format("For axis of semi-length %.1f, orientation is phi = %.1f°, theta = %.1f°",
					lv, Math.toDegrees(phiv), Math.toDegrees(thetav)));
		}
		System.out.println(spot.echo());
	
				
		// TEST 3D case
		/*
		
		// Parameters
		int size_x = 200;
		int size_y = 200;
		int size_z = 200;
		
		float a = 5.5f;
		float b = 4.9f;
		float c = 5;
		float theta_r = (float) Math.toRadians(0); // I am unable to have it working for theta_r != 0
		float phi_r = (float) Math.toRadians(45);

		float max_radius = Math.max(a, Math.max(b, c));
		float[] calibration = new float[] {1, 1, 1};
		
		// Create blank image
		Image<UnsignedByteType> img = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
			).createImage(new int[] {200, 200, 200});
		final byte on = (byte) 255;
		
		// Create an ellipse 
		long start = System.currentTimeMillis();
		System.out.println(String.format("Creating an ellipse with a = %.1f, b = %.1f, c = %.1f", a, b, c));
		System.out.println(String.format("phi = %.1f and theta = %.1f", Math.toDegrees(phi_r), Math.toDegrees(theta_r)));
		float[] center = new float[] { size_x/2, size_y/2, size_z/2 };
		SphereCursor<UnsignedByteType> sc = new SphereCursor<UnsignedByteType>(img, center, max_radius, calibration);
		double r2, theta, phi, term;
		double cosphi, sinphi, costheta, sintheta;
		while (sc.hasNext()) {
			sc.fwd();
			r2 = sc.getDistanceSquared();
			phi = sc.getPhi();
			theta = sc.getTheta();
			cosphi = Math.cos(phi-phi_r);
			sinphi = Math.sin(phi-phi_r);
			costheta = Math.cos(theta-theta_r);
			sintheta = Math.sin(theta-theta_r);
			term = r2*cosphi*cosphi*sintheta*sintheta/a/a +
				r2*sinphi*sinphi*sintheta*sintheta/b/b   +
				r2*costheta*costheta/c/c;
			if (term <= 1) 
				sc.getType().set(on);
		}
		sc.close();
		long end = System.currentTimeMillis();
		System.out.println("Ellipse creation done in " + (end-start) + " ms.");
		System.out.println();
		
		ij.ImageJ.main(args);
		img.getDisplay().setMinMax();
		ImageJFunctions.copyToImagePlus(img).show();
		
		start = System.currentTimeMillis();
		BlobMorphology<UnsignedByteType> bm = new BlobMorphology<UnsignedByteType>(img, calibration);
		SpotImp spot = new SpotImp(center);
		spot.putFeature(Feature.RADIUS, max_radius);
		bm.process(spot);
		end = System.currentTimeMillis();
		System.out.println("Blob morphology analyzed in " + (end-start) + " ms.");
		float phiv, thetav, lv;
		for (int j = 0; j < 3; j++) {
			lv = spot.getFeature(featurelist_sa[j]);
			phiv = spot.getFeature(featurelist_phi[j]);
			thetav = spot.getFeature(featurelist_theta[j]);
			System.out.println(String.format("For axis of semi-length %.1f, orientation is phi = %.1f°, theta = %.1f°",
					lv, Math.toDegrees(phiv), Math.toDegrees(thetav)));
		}
		System.out.println(spot.echo());
		
		*/
	}

}
