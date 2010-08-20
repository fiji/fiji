package fiji.plugin.nperry.features;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class BlobMorphology <T extends RealType<T>> extends IndependentFeatureAnalyzer {

	/*
	 * Fields
	 */
	
	private static final double SIGNIFICANCE_FACTOR = 3.0;
	private static final Feature FEATURE = Feature.MORPHOLOGY;
	private static final int ELLIPSOID = 1;
	private static final int SPHERICAL = 0;
	private Image<T> img;
	private float diam;
	private float[] calibration;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public BlobMorphology (Image<T> img, float diam, float[] calibration) {
		this.img = img;
		this.diam = diam;
		this.calibration = calibration;
	}
	
	public BlobMorphology (Image<T> img, float diam) {
		this(img, diam, img.getCalibration());
	}
	
	public BlobMorphology (Image<T> img, double diam, double[] calibration) {
		
		// 1 - Convert double[] to float[]
		float[] fCalibration = new float[3];
		for (int i = 0; i < calibration.length; i++) {
			fCalibration[i] = (float) calibration[i];
		}
		
		// 2 - Construct
		this.img = img;
		this.diam = (float) diam;
		this.calibration = fCalibration;
	}
	
	public BlobMorphology (Image<T> img, double diam) {
		this(img, (float) diam, img.getCalibration());
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	private static  ArrayList<Vector3d> prepareUnitVectors(double spacing) {
		double radius;
		double theta; // Angle measured from X axis to current point in the XZ plane
		double phi; // Angle measured from the X axis to the current point in the XY plane
		double x, y, z; // Coordinates of the unit vector
		final ArrayList<Vector3d> unit_vectors = new ArrayList<Vector3d>();
		for (theta = 0; theta <= Math.PI/2; theta += spacing) {
			radius = Math.cos(theta);
			z = Math.sin(theta);
			for (phi = 0; phi < 2*Math.PI; phi += spacing/radius) {				
				x = radius * Math.cos(phi);
				y = radius * Math.sin(phi);
				unit_vectors.add(new Vector3d(x, y, z));				
			}
		}
		return unit_vectors;
		
		
	}
	
	
	
	
	
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
		
		final int[] azCounts = new int[8];
		final int[] incCounts = new int[8];
		
		// 1 - Initialize local variables
		final float[] azimuthOctants = new float[8];
		final float[] inclinationOctants = new float[8];
		final float[] origin = spot.getCoordinates();
		int azOctant, incOctant;
		double phi, theta, val;
		final SphereCursor<T> cursor = new SphereCursor<T>(img, origin, diam / 2, calibration);
		
		// 2 - Iterate over pixels in sphere, assign to an octant by azimuth and then by inclination.
		int counter = 0;
		while (cursor.hasNext()) {
			cursor.next();
			counter++;
			phi = cursor.getPhi();
			theta = cursor.getTheta();
			azOctant = getAzimuthOctantIndex(phi);
			incOctant = getInclinationOctantIndex(theta, phi);
			val = cursor.getType().getRealDouble();
			//System.out.println("Coords: " + MathLib.printCoordinates(cursor.getPosition()));
			//System.out.println("Val: " + val);
			azimuthOctants[azOctant] += val;
			inclinationOctants[incOctant] += val;
			azCounts[azOctant]++;  //debug, for counting how many pixels belong to this octant
			incCounts[incOctant]++;  //debug, for counting how many pixels belong to this octant
		}
		
		// 2.5 Normalize to get mean intensities
		for (int i = 0; i < inclinationOctants.length; i++) 
			inclinationOctants[i] /= incCounts[i];
		for (int i = 0; i < azimuthOctants.length; i++) {
			azimuthOctants[i] /= azCounts[i];
		}
		
		// 3 - Determine the shape of the object.
		
		// 3.1 - Aggregate octant pair intensities (a pair consists of two directly opposing octants).
		float[] azimuthOctantPairs = new float[4];
		float[] inclinationOctantPairs = new float[4];
		aggregateOctantPairs(azimuthOctants, azimuthOctantPairs);
		aggregateOctantPairs(inclinationOctants, inclinationOctantPairs);
		
		
		//<debug>
		System.out.println(spot);

		System.out.println("Number pixels in sphere: " + counter);
		
		System.out.print("Azimuth count: \t\t");
		for (int i = 0; i < azCounts.length; i++) 
			System.out.print("\t\t"+azCounts[i]);
		System.out.println();
		
		System.out.print("Azimuth intensities: \t");
		for (int i = 0; i < azimuthOctants.length; i++) 
			System.out.print(String.format("\t\t%.1e", azimuthOctants[i]));
		System.out.println();
		
		System.out.print("Azimuth pair intensities: ");
		for (int i = 0; i < azimuthOctantPairs.length; i++) 
			System.out.print(String.format("\t\t%.1e", azimuthOctantPairs[i]));
		System.out.println();
		
		System.out.print("Inclination count: \t");
		for (int i = 0; i < incCounts.length; i++) 
			System.out.print("\t\t"+incCounts[i]);
		System.out.println();
		
		System.out.print("Inclination intensities: ");
		for (int i = 0; i < inclinationOctants.length; i++) 
			System.out.print(String.format("\t\t%.1e", inclinationOctants[i]));
		System.out.println();
		
		System.out.print("Inclination pair intensities: ");
		for (int i = 0; i < inclinationOctantPairs.length; i++) 
			System.out.print(String.format("\t\t%.1e", inclinationOctantPairs[i]));
		System.out.println();

		
		int maxAzIndex = 0;
		float maxAz = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < azimuthOctantPairs.length; i++)
			if (azimuthOctantPairs[i] > maxAz) {
				maxAz = azimuthOctantPairs[i];
				maxAzIndex = i;
			}
		
		float interpolatedAngle;
		float[] azAngles = new float[] {-67, -22, 22, 67 };
		if (maxAzIndex == 0) {
			interpolatedAngle = quadratic1DInterpolation(
					azAngles[3]-180, azimuthOctantPairs[3], 
					azAngles[0], azimuthOctantPairs[0], 
					azAngles[1], azimuthOctantPairs[1]); 
		} else if (maxAzIndex == 3) {
			interpolatedAngle = quadratic1DInterpolation(
					azAngles[2], azimuthOctantPairs[2], 
					azAngles[3], azimuthOctantPairs[3], 
					azAngles[0]+180, azimuthOctantPairs[0]); 
		} else {
			interpolatedAngle = quadratic1DInterpolation(
					azAngles[maxAzIndex-1], azimuthOctantPairs[maxAzIndex-1], 
					azAngles[maxAzIndex], azimuthOctantPairs[maxAzIndex], 
					azAngles[maxAzIndex+1], azimuthOctantPairs[maxAzIndex+1]); 
		}
		
		System.out.println(String.format("Interpolated azimuth: %.1f", interpolatedAngle+90));
		
		

		int maxIncIndex = 0;
		float maxInc = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < inclinationOctantPairs.length; i++)
			if (inclinationOctantPairs[i] > maxInc) {
				maxInc = inclinationOctantPairs[i];
				maxIncIndex = i;
			}
		
		float interpolatedIncAngle;
		float[] incAngles = new float[] {22, 67, 113, 157 };
		if (maxIncIndex == 0) {
			interpolatedIncAngle = quadratic1DInterpolation(
					incAngles[3]-180, inclinationOctantPairs[3], 
					incAngles[0], inclinationOctantPairs[0], 
					incAngles[1], inclinationOctantPairs[1]); 
		} else if (maxIncIndex == 3) {
			interpolatedIncAngle = quadratic1DInterpolation(
					incAngles[2], inclinationOctantPairs[2], 
					incAngles[3], inclinationOctantPairs[3], 
					incAngles[0]+180, inclinationOctantPairs[0]); 
		} else {
			interpolatedIncAngle = quadratic1DInterpolation(
					incAngles[maxIncIndex-1], inclinationOctantPairs[maxIncIndex-1], 
					incAngles[maxIncIndex], inclinationOctantPairs[maxIncIndex], 
					incAngles[maxIncIndex+1], inclinationOctantPairs[maxIncIndex+1]); 
		}
		
		System.out.println(String.format("Interpolated inclination: %.1f", 180 - interpolatedIncAngle));
		
		
		int azimuthIndex = brighterOctantPairExists(azimuthOctantPairs);
		int inclinationIndex = brighterOctantPairExists(inclinationOctantPairs);
		if (azimuthIndex >= 0  || inclinationIndex >= 0) {
			System.out.println("ELLIPSE");
			System.out.println("with azimuth index " + azimuthIndex);
			System.out.println("and inclination index " + inclinationIndex);
		} else {
			System.out.println("SPHERE");
		}
		System.out.println();
		//</debug>
		
		// 3.2 - Search for significantly brighter octant pairs as compared to other pairs.
		if (azimuthIndex >= 0  || inclinationIndex >= 0) {
			spot.addFeature(Feature.MORPHOLOGY, ELLIPSOID);  // 1 signifies ellipsoid
		} else {
			spot.addFeature(Feature.MORPHOLOGY, SPHERICAL);  // 0 signifies spherical
		}
		
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
	
	/**
	 * Compares the total intensity of each octant pair to the other octant pairs. If the total intensity
	 * of any pair is greater than <code>SIGNIFICANCE_FACTOR * intensity</code> (SIGNIFICANCE_FACTOR default set
	 * to <code>2.0</code>) of another pair, the object is considered to be an ellipsoid, since a sphere would not exhibit
	 * that behavior.
	 * 
	 * @param octantPairs An array of length 4 which contains the summed intensities of the octant pairs (which are defined
	 * in {@link aggregateOctantPairs}.
	 * @return Returns the octant pair index if any pair is significant brigher than another pair, or 
 	 * -1 otherwise.
	 */
	private final int brighterOctantPairExists(float[] octantPairs) {
		int octantPairFound = -1;
		for (int i = 0; i < octantPairs.length; i++) {
			for (int j = 0; j < octantPairs.length; j++) {
				if (i ==j) continue;
				if (octantPairs[i] >= SIGNIFICANCE_FACTOR * octantPairs[j]) 
					octantPairFound = i;  // if any pair is found to be significantly greater than another pair, we consider it non-spherical.
			}
		}
		return octantPairFound;
	}
	
	/**
	 * Sums the intensities from opposing octants into a single value.
	 * 
	 * @param octants An array of length 8, where each index stores the total intensity of the pixels belonging to that octant.
	 * @param octantPairs An array of length 4 which is used to store the summed intensities of directly opposing octants.
	 */
	/*
	 * For help with above:
	 *       _________     
	 *      / \3 | 4/ \    
	 *     / 2 \ | / 5 \   
	 *    |_____\|/_____|  
	 *     \ 1  /|\  6 /   
	 *      \ /  |  \ /    
	 *	     \_0_|_7_/     
	 *
	 * Opposing octants: (0,4), (1,5), (2,6), (3,7)
	 */
	public final void aggregateOctantPairs(float[] octants, float[] octantPairs) {
		for (int i = 0; i < 4; i++) {
			octantPairs[i] = octants[i] + octants[i + 4];
		}
	}

	/**
	 * Computes which octant the degree phi belongs to. Octants are divided accordingly:
	 * 					<br><br><code>
	 * 					[-180, -135) : 0<br>
	 * 					[-135, -90)  : 1<br>
	 * 					[-90, -45)   : 2<br>
	 * 					[-45, 0)     : 3<br>
	 * 					[0, 45)      : 4<br>
	 * 					[45, 90)     : 5<br>
	 * 					[90, 135)    : 6<br>
	 * 					[135, 180]   : 7
	 * 					</code>
	 * 
	 * @param phi The azimuth angle, which is defined on [-pi, pi].
	 * @return The <code>int</code> value assigned to the octant that phi belongs to, described above.
	 */
	private final int getAzimuthOctantIndex(double phi) {
		if (phi >= 0) {
			if (phi >= Math.PI / 2) {
				if (phi >= (3 * Math.PI / 4) ) {	// [135, 180]
					return 7;
				} else {							// [90, 135)
					return 6;
				}
			} else {
				if (phi >= (Math.PI / 4) ) {		// [45, 90)
					return 5;						
				} else {							// [0, 45)
					return 4;						
				}
			}
		} else {
			if (phi >= - Math.PI / 2) {
				if (phi >= ( - Math.PI / 4) ) {		// [-45, 0)
					return 3;						
				} else {							// [-90, -45)
					return 2;						
				}
			} else {
				if (phi >= (-3 * Math.PI / 4) ) {	// [-135, -90)
					return 1;						
				} else {							// [-180, -135)
					return 0;						
				}
			}
		}
	}
	
	/**
	 * Computes which octant the inclination angle falls into. Since theta (the inclination)
	 * is defined on [-pi/2, pi/2], the sign of the azimuth (phi) is used to fully determine
	 * the octant assignment.
	 * 
	 * Being defined on [-pi/2, pi/2], theta only covers a half-circle. To make assignment to
	 * the other half of the circle (thus allowing for true octants), the sign of the azimuth
	 * is used.
	 * 
	 *  First, theta is used to assign to buckets 0, 1, 2, or 3:
	 *  <code><br><br>
	 *  [-90, -45) : 0 <br>
	 *  [-45, 0)   : 1 <br>
	 *  [0, 45)    : 2 <br>
	 *  [45, 90]   : 3 <br>
	 *  </code><br>
	 *  
	 *  Then, the sign of the azimuth is checked. For points with the same theta, and with
	 *  azimuth identical in magnitude but opposite in sign (pi, and -pi for example), then
	 *  these points are mirror images of each other along the horizontal axis. So, to fit
	 *  the binning scheme above, if the azimuth is <0, then the new index becomes 7 - the
	 *  old index. For example, if the index had been 2 for the point, it's new index is 5 if
	 *  the azimuth for this point was <0. This makes it simple to find opposing octants - 
	 *  for all octants 0, 1, 2, and 3, the opposing octant is simply +4.
	 * 
	 * 
	 * @param theta
	 * @param phi
	 * @return
	 */
	/*
	 * For help with above:
	 *        _______
	 *       /   |   \     
	 *      / \3 | 4/ \    
	 *     / 2 \ | / 5 \   
	 *    |_____\|/_____|  
	 *     \ 1  /|\  6 /   
	 *      \ /0 |7 \ /    
	 *	     \___|___/     
	 *
	 */
	private final int getInclinationOctantIndex(double theta, double phi) {
		int index;
		if (theta >= Math.PI / 2) {
			if (theta >= (3 * Math.PI / 4)) {	// [135, 180] 
				index = 3;
			} else {							// [90, 135)
				index = 2;
			}
		} else {
			if (theta >= (Math.PI / 4)) {		// [45, 90)
				index = 1;
			} else {							// [0, 45)
				index = 0;
			}
		}
		
		// If the azimuth is negative, subtract the index from 7, so that opposing octants are i and i+4, for i = {0,1,2,3}
		if (phi < 0) {
			index = 7 - index;
		}
		
		return index;
	}
	
	public static void main(String[] args) {

		ArrayList<Vector3d> uvs = prepareUnitVectors(Math.PI/4);
		for (Vector3d uv : uvs) {
			System.out.println(uv);
		}
	}
}
