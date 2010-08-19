package fiji.plugin.nperry.features;

import ij.IJ;
import ij.ImagePlus;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
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
		final double[] azimuthOctants = new double[8];
		final double[] inclinationOctants = new double[8];
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
		
		// 3 - Determine the shape of the object.
		
		// 3.1 - Aggregate octant pair intensities (a pair consists of two directly opposing octants).
		double[] azimuthOctantPairs = new double[4];
		double[] inclinationOctantPairs = new double[4];
		aggregateOctantPairs(azimuthOctants, azimuthOctantPairs);
		aggregateOctantPairs(inclinationOctants, inclinationOctantPairs);
		
		// 3.2 - Search for significantly brighter octant pairs as compared to other pairs.
		if (brighterOctantPairExists(azimuthOctantPairs) || brighterOctantPairExists(inclinationOctantPairs)) {
			spot.addFeature(Feature.MORPHOLOGY, ELLIPSOID);  // 1 signifies ellipsoid
		} else {
			spot.addFeature(Feature.MORPHOLOGY, SPHERICAL);  // 0 signifies spherical
		}
		
		//<debug>
		System.out.println("--- New Spot ---");
		System.out.println("Coordinates: " + MathLib.printCoordinates(origin));
		System.out.println("Number pixels in sphere: " + counter);
		System.out.println("Azimuth count: " + azCounts[0] + ", " + azCounts[1] + ", " + azCounts[2] + ", " + azCounts[3] + ", " + azCounts[4] + ", " + azCounts[5] + ", " + azCounts[6] + ", " + azCounts[7]);
		System.out.println("Azimuth intensities: " + azimuthOctants[0] + ", " + azimuthOctants[1] + ", " + azimuthOctants[2] + ", " + azimuthOctants[3] + ", " + azimuthOctants[4] + ", " + azimuthOctants[5] + ", " + azimuthOctants[6] + ", " + azimuthOctants[7]);
		System.out.println("Azimuth pair intensities: " + azimuthOctantPairs[0] + ", " + azimuthOctantPairs[1] + ", " + azimuthOctantPairs[2] + ", " + azimuthOctantPairs[3]);
		System.out.println("Inclination count: " + incCounts[0] + ", " + incCounts[1] + ", " + incCounts[2] + ", " + incCounts[3] + ", " + incCounts[4] + ", " + incCounts[5] + ", " + incCounts[6] + ", " + incCounts[7]);
		System.out.println("Inclination intensities: " + inclinationOctants[0] + ", " + inclinationOctants[1] + ", " + inclinationOctants[2] + ", " + inclinationOctants[3] + ", " + inclinationOctants[4] + ", " + inclinationOctants[5] + ", " + inclinationOctants[6] + ", " + inclinationOctants[7]);
		System.out.println("Inclination pair intensities: " + inclinationOctantPairs[0] + ", " + inclinationOctantPairs[1] + ", " + inclinationOctantPairs[2] + ", " + inclinationOctantPairs[3]);
		if (brighterOctantPairExists(azimuthOctantPairs) || brighterOctantPairExists(inclinationOctantPairs)) {
			System.out.println("ELLIPSE");
		} else {
			System.out.println("SPHERE");
		}
		System.out.println();
		//</debug>
		
		
	}
	
	/**
	 * Compares the total intensity of each octant pair to the other octant pairs. If the total intensity
	 * of any pair is greater than <code>SIGNIFICANCE_FACTOR * intensity</code> (SIGNIFICANCE_FACTOR default set
	 * to <code>2.0</code>) of another pair, the object is considered to be an ellipsoid, since a sphere would not exhibit
	 * that behavior.
	 * 
	 * @param octantPairs An array of length 4 which contains the summed intensities of the octant pairs (which are defined
	 * in {@link aggregateOctantPairs}.
	 * @return Returns <code>true</code> if any pair is significant brigher than another pair, or <code>
	 * false</code> otherwise.
	 */
	private final boolean brighterOctantPairExists(double[] octantPairs) {
		boolean brighterPairExists = false;
		for (int i = 0; i < octantPairs.length; i++) {
			for (int j = 0; j < octantPairs.length; j++) {
				if (i ==j) continue;
				if (octantPairs[i] >= SIGNIFICANCE_FACTOR * octantPairs[j]) brighterPairExists = true;  // if any pair is found to be significantly greater than another pair, we consider it non-spherical.
			}
		}
		return brighterPairExists;
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
	public final void aggregateOctantPairs(double[] octants, double[] octantPairs) {
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
	 *       _________     
	 *      / \3 | 4/ \    
	 *     / 2 \ | / 5 \   
	 *    |_____\|/_____|  
	 *     \ 1  /|\  6 /   
	 *      \ /  |  \ /    
	 *	     \_0_|_7_/     
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
		

	}
}
