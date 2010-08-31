package fiji.plugin.nperry.tracking;

import java.util.ArrayList;
import java.util.Collection;

import Jama.Matrix;

import mpicbg.imglib.algorithm.math.MathLib;

import fiji.plugin.nperry.Spot;

/**
 * 
 * <b>Overview</b> 
 * 
 * <p>
 * This class tracks objects by formulating the problem as a Linear Assignment Problem.
 * 
 * <p>
 * For reference, see:
 * Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>
 * In this tracking framework, tracking is divided into two steps:
 * 
 * <ol>
 * <li>Identify individual track segments</li>
 * <li>Gap closing, merging and splitting</li>
 * </ol>
 * 
 * <p>
 * Both steps are treated as a linear assignment problem. To solve the problems, a cost
 * matrix is designed for each step, and the Hungarian Algorithm is used to determine
 * the cost-minimizing assignments. The result of the calculation is the complete
 * tracks of the objects. For more details on the Hungarian Algorithm, see
 * http://en.wikipedia.org/wiki/Hungarian_algorithm.
 * 
 * <p><b>Cost Matrices</b>
 * 
 * <p>
 * Since there are two discrete steps to tracking using this framework, two distinct
 * cost matrices are required to solve the problem. The user can optionally provide
 * the cost matrices at construction, or use the default cost matrices defined
 * in the class. 
 * 
 * <p>One matrix corresponds to step (1) above, and is used to assign objects to 
 * individual track segments. A track segment is created by linking the objects between 
 * consecutive frames, with the condition that at an object in one frame can link to at 
 * most one other object in another frame. The options for a object assignment at this
 * step are:
 * 
 * <ul>
 * <li>Object linking (an object in frame t is linked one-to-one to a object in frame
 * t+1)</li>
 * <li>Object in frame t not linked to an object in frame t+1 (track end)</li>
 * <li>Object in frame t+1 not linked to an object in frame t (track start)</li>
 * </ul>
 * 
 * TODO draw/describe the cost matrix 
 * 
 * <p>The other matrix corresponds to step (2) above, and is used to link together
 * the various track segments previously calculated. Track segments can be:
 * 
 * <ul>
 * <li>Linked end-to-tail (gap closing)</li>
 * <li>Split (the start of one track is linked to the middle of another track)</li>
 * <li>Merged (the end of one track is linked to the middle of another track</li>
 * <li>Terminated (track ends)<li>
 * <li>Initiated (track starts)</li>
 * </ul>
 * 
 * TODO draw/describe the cost matrix
 * 
 * @version 0.1
 * @author nperry
 */
public class LAPTracker implements ObjectTracker {
	
	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double maxDist = 5.0f;
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.5f;
	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected static final double BLOCKED = Double.POSITIVE_INFINITY;
	
	/** The cost matrix for linking individual objects (step 1) */
	protected ArrayList<double[][]> objectLinkingCosts;
	/** The cost matrix for linking individual track segments (step 2). */
	protected float[][] segmentLinkingCosts;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected ArrayList< ArrayList<Spot> > objects;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user will provide cost matrices or not. If automatic,
	 * the cost matrices will be those used in the paper. */
	protected boolean automatic = true;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** Stores whether the object cost matrix (for step 1) has been initialized or not. */
	protected boolean objectCostsSet = false;
	/** Stores whether the track segment cost matrix (for step 2) has been initialized 
	 * or not. */
	protected boolean segmentCostsSet = false;
	
	
	/**
	 * This constructor allows the user to use the default cost matrix cost functions, 
	 * as defined in Jaqaman, K. et al (see {@link LAPTracker} for complete reference)
	 * for Brownian motion tracking.
	 * @param objects Holds the list of Spots belonging to each frame in the movie.
	 */
	public LAPTracker (Collection< Collection<Spot> > objects) {
		ArrayList< Collection<Spot> > temp = new ArrayList< Collection<Spot> >(objects);
		this.objects = new ArrayList< ArrayList<Spot> >();
		
		for (int i = 0; i < temp.size(); i++) {
			this.objects.add(new ArrayList<Spot>(temp.get(i)));
		}
	}
	
	
	/**
	 * This constructor should be used if the user would like to externally create cost
	 * matrices. In this case, the algorithm is processed in steps, allowing the user
	 * to extract the track segments following step (1) in order to create the cost matrix
	 * for step (2).
	 */
	public LAPTracker () {
		this.automatic = false;
	}
	
	
	/**
	 * Returns the final tracks computed by the class.
	 */
	@Override
	public void getTracks() {
		// TODO Auto-generated method stub
	}
	
	
	/**
	 * Sets the cost matrix to use for linking individual objects to track segments
	 * in step (1).
	 * @param costs The properly formatted cost matrix for step (1). See
	 * {@link LAPTracker} for details on proper formatting.
	 */
	public void setObjectCostMatrix(ArrayList<double[][]> costs) {
		objectLinkingCosts = costs;
		objectCostsSet = true;
	}
	
	
	/**
	 * Get the cost matrix used for linking individual objects to track segments
	 * in step (1).
	 * @return The cost matrix used for linking objects to track segments in step (1).
	 */
	public ArrayList<double[][]> getObjectCostMatrix() {
		return objectLinkingCosts;
	}
	
	
	/**
	 * Sets the cost matrix to use for linking track segments to final track
	 * in step (2).
	 * @param costs The properly formatted cost matrix for step (2). See
	 * {@link LAPTracker} for details on proper formatting.
	 */
	public void setTrackSegmentsCostMatrix(float[][] costs) {
		segmentLinkingCosts = costs;
		segmentCostsSet = true;
	}
	
	
	/**
	 * Get the cost matrix used for linking track segments to final tracks
	 * in step (2).
	 * @return The cost matrix used for linking track segments to final tracks in step (2).
	 */
	public float[][] getTrackSegmentsCostMatrix() {
		return segmentLinkingCosts;
	}
	
	
	/**
	 * Returns the track segments computed from step (1).
	 */
	public void getTrackSegments() {
		// TODO provide way to extract track segments, so they can make cost matrix 2 manually.
	}

	
	@Override
	public boolean checkInput() {
		// Check that the objects list itself isn't null
		if (null == objects) {
			errorMessage = "The objects list is null.";
			return false;
		}
		
		// Check that the objects list contains inner collections.
		if (objects.isEmpty()) {
			errorMessage = "The objects list is empty.";
			return false;
		}
		
		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for (Collection<Spot> c : objects) {
			if (!c.isEmpty()) {
				empty = false;
				break;
			}
		}
		if (empty) {
			errorMessage = "The objects list is empty.";
			return false;
		}
		
		inputChecked = true;
		return true;
	}

	
	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	
	@Override
	public boolean process() {
		// Make sure checkInput() has been executed.
		if (!inputChecked) {
			errorMessage = "checkInput() must be executed before process().";
			return false;
		}
		
		// Step 1
		createDefaultObjectCosts();
		for (int i = 0; i < objects.size(); i++) {
			computeTrackSegments();
		}
		
		// Step 2
		createDefaultTrackSegmentCosts();
		computeFinalTracks();
		
		return true;
	}

	
	/**
	 * Compute the optimal track segments using the cost matrix 
	 * {@link LAPTracker#objectLinkingCosts}.
	 * @return True if executes correctly, false otherwise.
	 */
	public boolean computeTrackSegments() {
		if (!objectCostsSet) {
			errorMessage = "The object cost matrix for step (1) has not been created.";
			return false;
		}
		
		else {
			
		}
		
		return true;
	}
	
	
	/**
	 * Compute the optimal track segments using the cost matrix 
	 * {@link LAPTracker#segmentLinkingCosts}.
	 * @return True if executes correctly, false otherwise.
	 */
	public boolean computeFinalTracks() {
		if (!segmentCostsSet) {
			errorMessage = "The segment cost matrix for step (2) has not been created.";
			return false;
		}
		
		else {
			return true;
		}
	}
	
	
	/**
	 * Uses the scores defined in the paper for creating {@link LAPTracker#objectLinkingCosts}.
	 */
	public boolean createDefaultObjectCosts() {
		// check inputs
		if (!inputChecked) {
			errorMessage = "checkInput() must be executed before createDefaultObjectCosts().";
			return false;
		}
		
		// initialize cost matrices container
		objectLinkingCosts = new ArrayList<double[][]>();
		
		// create cost matrices
		for (int i = 0; i < objects.size() - 1; i++) {
			ArrayList<Spot> t0 = new ArrayList<Spot>(objects.get(i));
			ArrayList<Spot> t1 = new ArrayList<Spot>(objects.get(i+1));
			Matrix costs = fillInObjectCostMatrix(t0, t1, t0.size() + t1.size());
			objectLinkingCosts.add(costs.getArray());
		}
		
		objectCostsSet = true;
		return true;
	}
	
	
	/**
	 * Uses the scores defined in the paper for creating {@link LAPTracker#segmentLinkingCosts}.
	 */
	public boolean createDefaultTrackSegmentCosts() {
		// check track segments exist
		
		segmentCostsSet = true;
		return true;
	}
	
	/**
	 * This function can be used for equations (3) and (4) in the paper.
	 * 
	 * @param i Spot i
	 * @param j Spot j
	 * @return The Euclidean distance between Spots i and j.
	 */
	private double euclideanDistance(Spot i, Spot j) {
		final float[] coordsI = i.getCoordinates();
		final float[] coordsJ = j.getCoordinates();
		double eucD = 0;

		for (int k = 0; k < coordsI.length; k++) {
			eucD += (coordsI[k] - coordsJ[k]) * (coordsI[k] - coordsJ[k]);
		}
		eucD = Math.sqrt(eucD);

		return eucD;
	}
	
	
	/**
	 * <p>Returns a filled-in Matrix to be used to link objects between frame t and frame t+1.
	 * 
	 * <p>For a description of the scores used in this matrix, please see Supplementary Note
	 * 3 of the paper.
	 * @param t0 The Spots belonging to frame t.
	 * @param t1 The Spots belonging to frame t+1.
	 * @param length The length of one of the Matrix's sides (square matrix).
	 * @return A reference to the filled in cost matrix.
	 */
	public Matrix fillInObjectCostMatrix(ArrayList<Spot> t0, ArrayList<Spot> t1, int length) {
		double maxScore = Double.NEGATIVE_INFINITY;							// Will hold the maximum score of all links
		
		// Initialize parent matrix and sub matrices
		Matrix costs = new Matrix(length, length);
		Matrix topLeftQuadrant = new Matrix(t0.size(), t1.size());			// Linking
		Matrix topRightQuadrant = new Matrix(t0.size(), t0.size());			// No linking (objects in t)
		Matrix bottomLeftQuadrant =  new Matrix(t1.size(), t1.size());		// No linking (objects in t+1)
		Matrix bottomRightQuadrant = new Matrix(t1.size(), t0.size());		// Nothing, but mathematically required for LAP
		
		// Fill in top left quadrant
		Spot s0 = null;
		Spot s1 = null;
		for (int i = 0; i < t0.size(); i++) {
			s0 = t0.get(i);
			for (int j = 0; j < t1.size(); j++) {
				s1 = t1.get(j);
				double d = euclideanDistance(s0, s1);
				if (d < maxDist) {
					topLeftQuadrant.set(i, j, d);
					if (d > maxScore) {
						maxScore = d;
					}
				} else {
					topLeftQuadrant.set(i, j, BLOCKED);
				}
			}
		}
		
		// Fill in top right quadrant
		for (int i = 0; i < t0.size(); i++) {
			for (int j = 0; j < t0.size(); j++) {
				if (i == j) {
					topRightQuadrant.set(i, j, ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * maxScore);
				} else {
					topRightQuadrant.set(i, j, BLOCKED);
				}
				
			}
		}
		
		// Fill in bottom left quadrant
		for (int i = 0; i < t1.size(); i++) {
			for (int j = 0; j < t1.size(); j++) {
				if (i == j) {
					bottomLeftQuadrant.set(i, j, ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * maxScore);
				} else {
					bottomLeftQuadrant.set(i, j, BLOCKED);
				}
				
			}
		}

		// Fill in bottom right quadrant
		for (int i = 0; i < t1.size(); i++) {
			for (int j = 0; j < t0.size(); j++) {
				if (topLeftQuadrant.get(j, i) < maxScore) {
					bottomRightQuadrant.set(i, j, Double.MIN_VALUE);
				} else {
					bottomRightQuadrant.set(i, j, BLOCKED);
				}
			}
		}
		
		// Set submatrices of parent matrix with the submatrices we calculated separately
		costs.setMatrix(0, t0.size() - 1, 0, t1.size() - 1, topLeftQuadrant);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, t1.size(), costs.getColumnDimension() - 1, bottomRightQuadrant);
		costs.setMatrix(0, t0.size() - 1, t1.size(), costs.getColumnDimension() - 1, topRightQuadrant);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, 0, t1.size() - 1, bottomLeftQuadrant);
		
		return costs;
	}
	
	// For testing!
	public static void main(String args[]) {
		ArrayList<Spot> t0 = new ArrayList<Spot>();
		ArrayList<Spot> t1 = new ArrayList<Spot>();
		
		t0.add(new Spot(new float[] {0,0,0}));
		t0.add(new Spot(new float[] {1,1,1}));
		t0.add(new Spot(new float[] {2,2,2}));
		t0.add(new Spot(new float[] {3,3,3}));
		t0.add(new Spot(new float[] {4,4,4}));
		t0.add(new Spot(new float[] {5,5,5}));
		
		t1.add(new Spot(new float[] {1.5f,1.5f,1.5f}));
		t1.add(new Spot(new float[] {2.5f,2.5f,2.5f}));
		t1.add(new Spot(new float[] {3.5f,3.5f,3.5f}));
		t1.add(new Spot(new float[] {4.5f,4.5f,4.5f}));
	
		LAPTracker lap = new LAPTracker();
		Matrix test = lap.fillInObjectCostMatrix(t0, t1, t0.size() + t1.size());
		test.print(4, 2);
	}
}

