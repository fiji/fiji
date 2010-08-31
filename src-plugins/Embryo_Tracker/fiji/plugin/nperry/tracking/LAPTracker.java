package fiji.plugin.nperry.tracking;

import java.util.Collection;

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

	/** The cost matrix for linking individual objects (step 1) */
	protected float[][] objectLinkingCosts;
	/** The cost matrix for linking individual track segments (step 2). */
	protected float[][] segmentLinkingCosts;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected Collection< Collection<Spot> > objects;
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
		this.objects = objects;
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
	public void setObjectCostMatrix(float[][] costs) {
		objectLinkingCosts = costs;
		objectCostsSet = true;
	}
	
	
	/**
	 * Get the cost matrix used for linking individual objects to track segments
	 * in step (1).
	 * @return The cost matrix used for linking objects to track segments in step (1).
	 */
	public float[][] getObjectCostMatrix() {
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
		// TODO Auto-generated method stub
		if (automatic) {
			
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
		}
		
		inputChecked = true;
		return false;
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
		computeTrackSegments();
		
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
	public void createDefaultObjectCosts() {
		// check spots exist
		
		objectCostsSet = true;
	}
	
	
	/**
	 * Uses the scores defined in the paper for creating {@link LAPTracker#segmentLinkingCosts}.
	 */
	public void createDefaultTrackSegmentCosts() {
		// check track segments exist
		
		segmentCostsSet = true;
	}
	
}
