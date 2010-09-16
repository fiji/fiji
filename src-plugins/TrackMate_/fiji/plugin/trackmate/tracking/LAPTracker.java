package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackNode;
import fiji.plugin.trackmate.TrackNodeImp;
import fiji.plugin.trackmate.tracking.costmatrix.LinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.costmatrix.TrackSegmentCostMatrixCreator;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentProblem;
import fiji.plugin.trackmate.tracking.hungarian.HungarianAlgorithm;

/**
 * 
 * <h2>Overview</h2> 
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
 * matrix is created for each step, and the Hungarian Algorithm is used to determine
 * the cost-minimizing assignments. The results of the calculations are the complete
 * tracks of the objects. For more details on the Hungarian Algorithm, see
 * http://en.wikipedia.org/wiki/Hungarian_algorithm.
 * 
 * <h2>Cost Matrices</h2>
 * 
 * Since there are two discrete steps to tracking using this framework, two distinct
 * classes of cost matrices are required to solve the problem. The user can either choose
 * to use the cost matrices / functions from the paper (for Brownian motion),
 * or can supply their own cost matrices.
 * 
 * <p>One matrix corresponds to step (1) above, and is used to assign individual objects 
 * to track segments. A track segment is created by linking the objects between 
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
 * <p>The cost matrix for this step is illustrated in Figure 1b in the paper, and
 * is described in more detail in {@link LinkingCostMatrixCreator}.
 * 
 * <p>The other matrix corresponds to step (2) above, and is used to link together
 * the track segments into final tracks. Track segments can be:
 * 
 * <ul>
 * <li>Linked end-to-tail (gap closing)</li>
 * <li>Split (the start of one track is linked to the middle of another track)</li>
 * <li>Merged (the end of one track is linked to the middle of another track</li>
 * <li>Terminated (track ends)</li>
 * <li>Initiated (track starts)</li>
 * </ul>
 * 
 * <p>The cost matrix for this step is illustrated in Figure 1c in the paper, and
 * is described in more detail in {@link TrackSegmentCostMatrixCreator}.
 * 
 * <p>Solving both LAPs yields complete tracks.
 * 
 * <h2>How to use this class</h2>
 * 
 * <p>If using the default cost matrices/function, use the correct constructor,
 * and simply use {@link #process()}.
 * 
 * <p>If using your own cost matrices:
 * 
 *  <ol>
 *  <li>Use the correct constructor.</li>
 *	<li>Set the linking cost matrix using {@link #setLinkingCosts(ArrayList)}.</li>
 *	<li>Execute {@link #linkObjectsToTrackSegments()}.</li>
 *  <li>Get the track segments created using {@link #getTrackSegments()}.</li>
 * 	<li>Create the segment cost matrix.
 * 	<li>Set the segment cost matrix using {@link #setSegmentCosts(double[][])}.</li>
 * 	<li>Run {@link #linkTrackSegmentsToFinalTracks(ArrayList)} to compute the final tracks.</li>
 * </ol>
 * 
 * @author Nicholas Perry
 */
public class LAPTracker<K extends Spot> implements ObjectTracker {
	
	public static class Settings {
		
		public static final Settings DEFAULT = new Settings();
		
		// Default settings
		private static final int MINIMUM_SEGMENT_LENGTH = 3; 
		private static final double MAX_DIST_OBJECTS = 15.0d;
		private static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;
		private static final double MAX_DIST_SEGMENTS = 15.0d;
		private static final int GAP_CLOSING_TIME_WINDOW = 4;
		private static final double MIN_INTENSITY_RATIO = 0.5d;
		private static final double MAX_INTENSITY_RATIO = 4.0d;
		private static final double CUTOFF_PERCENTILE = 0.9d;
		
		/** To throw out spurious segments, only include track segments with a length strictly larger
		 * than this value. */
		public int minSegmentLength = MINIMUM_SEGMENT_LENGTH;
		/** The maximum distance away two Spots in consecutive frames can be in order 
		 * to be linked. (Step 1 threshold) */
		public double maxDistObjects = MAX_DIST_OBJECTS;
		/** The maximum distance away two Segments can be in order 
		 * to be linked. (Step 2 threshold) */
		public double maxDistSegments = MAX_DIST_SEGMENTS;
		/** The factor used to create d and b in the paper, the alternative costs to linking
		 * objects. */
		public double altLinkingCostFactor = ALTERNATIVE_OBJECT_LINKING_COST_FACTOR;
		/** The maximum number of frames apart two segments can be 'gap closed.' */
		public int gapClosingTimeWindow = GAP_CLOSING_TIME_WINDOW;
		/** The minimum allowable intensity ratio for merging and splitting. */
		public double minIntensityRatio = MIN_INTENSITY_RATIO;
		/** The maximum allowable intensity ratio for merging and splitting. */
		public double maxIntensityRatio = MAX_INTENSITY_RATIO;
		/** The percentile used to calculate d and b cutoffs in the paper. */
		public double cutoffPercentile = CUTOFF_PERCENTILE;
	}
	
	/** Used as a flag when building track segments to indicate a Spot is unlinked to
	 * the next frame. */
	protected static final int NOT_LINKED = -2;
	/** Used as a flag when building the track segments to indicate a track segment with
	 * only a single Spot. */
	protected static final int SEGMENT_OF_SIZE_ONE = -1;

	/** The cost matrix for linking individual objects (step 1) */
	protected ArrayList<double[][]> linkingCosts = null;
	/** The cost matrix for linking individual track segments (step 2). */
	protected double[][] segmentCosts = null;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected ArrayList< ArrayList<TrackNode<K>> > objects;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** Stores the track segments computed during step (1) of the algorithm */
	protected ArrayList< ArrayList<TrackNode<K>> > trackSegments = null;
	/** Stores whether the default cost matrices from the paper should be used,
	 * or if the user will supply their own. */
	protected boolean defaultCosts = true;
	/** Holds references to the middle spots in the track segments. */
	protected ArrayList<K> middlePoints;
	/** Holds references to the middle spots considered for merging in
	 * the track segments. */
	protected ArrayList<TrackNode<K>> mergingMiddlePoints;	
	/** Holds references to the middle spots considered for splitting in 
	 * the track segments. */
	protected ArrayList<TrackNode<K>> splittingMiddlePoints;
	/** Each index corresponds to a Spot in middleMergingPoints, and holds
	 * the track segment index that the middle point belongs to. */
	protected int[] mergingMiddlePointsSegmentIndices;
	/** Each index corresponds to a Spot in middleSplittingPoints, and holds
	 * the track segment index that the middle point belongs to. */
	protected int[] splittingMiddlePointsSegmentIndices;
	/** The settings to use for this tracker. */
	private Settings settings = Settings.DEFAULT;
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	/** 
	 * Default constructor.
	 * 
	 * @param objects Holds a list of Spots for each frame in the time-lapse image.
	 * @param linkingCosts The cost matrix for step 1, linking objects.
	 * @param settings The settings to use for this tracker.
	 */
	public LAPTracker (TreeMap<Integer, ? extends Collection<TrackNode<K>> > objects, ArrayList<double[][]> linkingCosts, Settings settings) {
		this.objects = convertMapToArrayList(objects);
		this.linkingCosts = linkingCosts;
		this.settings = settings;
	}

	public LAPTracker (TreeMap<Integer, ? extends Collection<TrackNode<K>> > objects, ArrayList<double[][]> linkingCosts) {
		this(objects, linkingCosts, Settings.DEFAULT);
	}
	
	public LAPTracker(TreeMap<Integer, ? extends Collection<TrackNode<K>> > objects, Settings settings) {
		this(objects, null, settings);
	}

	public LAPTracker (TreeMap<Integer, ? extends Collection<TrackNode<K>> > objects) {
		this(objects, null, Settings.DEFAULT);
	}
	

	/*
	 * METHODS
	 */
	
	
	/**
	 * Set the cost matrices used for step 1, linking objects into track segments.
	 * @param linkingCosts The cost matrix, with structure matching figure 1b in the paper.
	 */
	public void setLinkingCosts(ArrayList<double[][]> linkingCosts) {
		this.linkingCosts = linkingCosts;
	}
	
	
	/**
	 * Get the cost matrices used for step 1, linking objects into track segments.
	 * @return The cost matrices, with one <code>double[][]</code> in the ArrayList for each frame t, t+1 pair.
	 */
	public ArrayList<double[][]> getLinkingCosts() {
		return linkingCosts;
	}
	
	
	/**
	 * Set the cost matrix used for step 2, linking track segments into final tracks.
	 * @param segmentCosts The cost matrix, with structure matching figure 1c in the paper.
	 */
	public void setSegmentCosts(double[][] segmentCosts) {
		this.segmentCosts = segmentCosts;
	}
	
	
	/**
	 * Get the cost matrix used for step 2, linking track segments into final tracks.
	 * @return The cost matrix.
	 */
	public double[][] getSegmentCosts() {
		return segmentCosts;
	}
	
	
	/**
	 * Returns the final tracks computed.
	 */
	@Override
	public void getTracks() {
		// TODO Auto-generated method stub
	}
	

	/**
	 * Returns the track segments computed from step (1).
	 * @return Returns a reference to the track segments, or null if {@link #computeTrackSegments()}
	 * hasn't been executed.
	 */
	public ArrayList< ArrayList<TrackNode<K>> > getTrackSegments() {
		return this.trackSegments;
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
		for (ArrayList<TrackNode<K>> c : objects) {
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

	
	/**
	 * Use <b>only if the default cost matrices (from the paper) are to be used.</b>
	 */
	@Override
	public boolean process() {
		
		// Make sure checkInput() has been executed.
		if (!inputChecked) {
			errorMessage = "checkInput() must be executed before process().";
			return false;
		}
		
		
		// Step 1 - Link objects into track segments
		System.out.println("--- Step one ---");
		
		// Create cost matrices
		if (!createLinkingCostMatrices()) return false;
		System.out.println("Cost matrix created for frame-to-frame linking successfully.");
		
		// Solve LAP
		if (!linkObjectsToTrackSegments()) return false;
		
		
		// Step 2 - Link track segments into final tracks
		System.out.println("--- Step two ---");

		// Create cost matrix
		if (!createTrackSegmentCostMatrix()) return false;
		System.out.println("Cost matrix for track segments created successfully.");
		
		// Solve LAP
		if (!linkTrackSegmentsToFinalTracks()) return false;
		
		
		
		// Print step 2 cost matrix
		Matrix debug = new Matrix(segmentCosts);
		for (int i = 0; i < debug.getRowDimension(); i++) {
			for (int j = 0; j < debug.getColumnDimension(); j++) {
				if (Double.compare(Double.MAX_VALUE, debug.get(i,j)) == 0) {
					debug.set(i, j, Double.NaN);
				}
			}
		}
		//debug.print(4,2);
		
		return true;
	}
	
	
	/**
	 * Creates the cost matrices used to link objects (Step 1)
	 * @return True if executes successfully, false otherwise.
	 */
	private boolean createLinkingCostMatrices() {
		linkingCosts = new ArrayList<double[][]>();	
		for (int i = 0; i < objects.size() - 1; i++) {
			LinkingCostMatrixCreator<K> objCosts = new LinkingCostMatrixCreator<K>(
					new ArrayList<TrackNode<K>>(objects.get(i)), 
					new ArrayList<TrackNode<K>>(objects.get(i + 1)),
					settings);
			if (!objCosts.checkInput() || !objCosts.process()) {
				System.out.println(objCosts.getErrorMessage());
				return false;
			}
			linkingCosts.add(objCosts.getCostMatrix());
		}
		return true;
	}

	
	/**
	 * Creates the cost matrix used to link track segments (step 2).
	 * @return True if executes successfully, false otherwise.
	 */
	private boolean createTrackSegmentCostMatrix() {
		TrackSegmentCostMatrixCreator<K> segCosts = new TrackSegmentCostMatrixCreator<K>(trackSegments, settings);
		if (!segCosts.checkInput() || !segCosts.process()) {
			System.out.println(segCosts.getErrorMessage());
			return false;
		}
		segmentCosts = segCosts.getCostMatrix();
		splittingMiddlePoints = segCosts.getSplittingMiddlePoints();
		mergingMiddlePoints = segCosts.getMergingMiddlePoints();
		return true;
	}
	
	
	/**
	 * Creates the track segments computed from step 1.
	 * @return True if execution completes successfully.
	 */
	public boolean linkObjectsToTrackSegments() {

		if (null == linkingCosts) {
			errorMessage = "The linking cost matrix is null.";
			return false;
		}
		
		// Solve LAP
		ArrayList< int[] > trackSegmentStructure = solveLAPForTrackSegments();
		System.out.println("LAP for frame-to-frame linking solved.\n");
		
		// Compile LAP solutions into track segments
		compileTrackSegments(trackSegmentStructure);
		
		return true;
	}
	
	
	/**
	 * Creates the final tracks computed from step 2.
	 * @see TrackSegmentCostMatrixCreator#getMiddlePoints()
	 * @param middlePoints A list of the middle points of the track segments. 
	 * @return True if execution completes successfully, false otherwise.
	 */
	public boolean linkTrackSegmentsToFinalTracks() {
		
		// Check that there are track segments.
		if (null == trackSegments || trackSegments.size() < 1) {
			errorMessage = "There are no track segments to link.";
			return false;
		}
		
		// Check that the cost matrix for this step exists.
		if (null == segmentCosts) {
			errorMessage = "The segment cost matrix (step 2) does not exists.";
			return false;
		}

		// Solve LAP
		int[][] finalTrackSolutions = solveLAPForFinalTracks();
		System.out.println("LAP for track segments solved.\n");
		
		// Compile LAP solutions into final tracks
		compileFinalTracks(finalTrackSolutions);
		
//		System.out.println("SOLUTIONS!!");
//		for (int[] solution : finalTrackSolutions) {
//			System.out.println(String.format("[%d, %d]\n", solution[0], solution[1]));
//		}
		
		return true;
	}

	
	/**
	 * Compute the optimal track segments using the cost matrix 
	 * {@link LAPTracker#linkingCosts}.
	 * @return True if executes correctly, false otherwise.
	 */
	public ArrayList< int[] > solveLAPForTrackSegments() {

		/* This local copy of trackSegments will store the relationships between segments.
		 * It will later be converted into an ArrayList< ArrayList<K> > to explicitly
		 * store the segments themselves as a list of Spots.
		 */
		ArrayList< int[] > trackSegmentStructure = initializeTrackSegments();

		for (int i = 0; i < linkingCosts.size(); i++) {
			
			// Solve the LAP using the Hungarian Algorithm
			AssignmentProblem hung = new AssignmentProblem(linkingCosts.get(i));
			int[][] solutions = hung.solve(new HungarianAlgorithm());
			
			// Extend track segments using solutions
			extendTrackSegments(trackSegmentStructure, solutions, i);
		}

		return trackSegmentStructure;
	}
	
	
	/**
	 * Compute the optimal final track using the cost matrix 
	 * {@link LAPTracker#segmentCosts}.
	 * @return True if executes correctly, false otherwise.
	 */
	public int[][] solveLAPForFinalTracks() {
		// Solve the LAP using the Hungarian Algorithm
		AssignmentProblem hung = new AssignmentProblem(segmentCosts);
		int[][] solutions = hung.solve(new HungarianAlgorithm());
		
		return solutions;
	}
	
	
	/*
	 * Uses DFS approach to create ArrayList<K> track segments from the overall 
	 * result of step 1, which recorded the tracks in a series of int[]. This method
	 * converts the int[] track segments (a series of edges) into an explicit ArrayList
	 * of Spots.
	 */
	private void compileTrackSegments(ArrayList< int[] > trackSegments) {
		this.trackSegments = new ArrayList< ArrayList<TrackNode<K>> >();

		for (int i = 0; i < trackSegments.size(); i++) {						// For all frames
			int[] currFrame = trackSegments.get(i);
			for (int j = 0; j < currFrame.length; j++) {						// For all Spots in frame
				if (currFrame[j] != NOT_LINKED) {								// If this Spot in linked to something in the next frame (!= -1)
					ArrayList<TrackNode<K>> trackSegment = new ArrayList<TrackNode<K>>();		// Start a new track segment
					
					// Our spot is just in it's own segment of size one
					if (currFrame[j] == SEGMENT_OF_SIZE_ONE) {
						trackSegment.add(objects.get(i).get(j));
					}
					
					// Spot is beginning of a segment
					else {
						
						// DFS
						int currIndex = j;											// Record the current index
						int frame = i;												// Record the current frame
						int prevIndex = 0;
						while (currIndex != NOT_LINKED) {									
							trackSegment.add(objects.get(frame).get(currIndex));	// Add the current Spot at the current index/frame to the track segment
							prevIndex = currIndex;									// Save the location of the current index, so we can set it to -1 after incrementing.
							currIndex = trackSegments.get(frame)[currIndex];		// Update the current index to be the Spot pointed to by the Spot we just added to the track segment
							trackSegments.get(frame)[prevIndex] = NOT_LINKED;		// Set the Spot's value to -1 so we don't use it again
							frame++;												// Increment the frame number
						}
					}
					
					if (trackSegment.size() >= settings .minSegmentLength) {		
						// TODO probably incorporate this above, but this is faster to implement.
						// Link segment Spots to each other
						TrackNode<K> prev = null;
						TrackNode<K> curr = null;
						prev = trackSegment.get(0);
						for (int h = 1; h < trackSegment.size(); h++) {
							curr = trackSegment.get(h);
							prev.addChild(curr);
							curr.addParent(prev);
							prev = curr;
						}
						
						this.trackSegments.add(trackSegment);						// When we're here, eventually the current index was -1, so the track ended. Add the track to the list of tracks.
					}
				}
			}
		}
		
		// debug
		for (ArrayList<TrackNode<K>> segment : this.trackSegments) {
			System.out.println("-*-*-*-* New Segment *-*-*-*-");
			for (TrackNode<K> trackNode : segment)
				System.out.println(trackNode.getObject());
			System.out.println();
		}
		
	}
	
	
	/*
	 * Takes the solutions from the Hungarian algorithm, which are an int[][], and 
	 * appripriately links the track segments. Before this method is called, the Spots in the
	 * track segments are connected within themselves, but not between track segments.
	 * 
	 * Thus, we only care here if the result was a 'gap closing,' 'merging,' or 'splitting'
	 * event, since the others require no change to the existing structure of the
	 * track segments.
	 * 
	 * Method: for each solution of the LAP, determine if it's a gap closing, merging, or
	 * splitting event. If so, appropriately link the track segment Spots.
	 */
	private void compileFinalTracks(int[][] finalTrackSolutions) {
		final int numTrackSegments = trackSegments.size();
		final int numMergingMiddlePoints = mergingMiddlePoints.size();
		final int numSplittingMiddlePoints = splittingMiddlePoints.size();
		
		for (int[] solution : finalTrackSolutions) {
			int i = solution[0];
			int j = solution[1];
//			//debug
//			System.out.println(String.format("i = %d, j = %d", i, j));
			if (i < numTrackSegments) {
				
				// Case 1: Gap closing
				if (j < numTrackSegments) {
					ArrayList<TrackNode<K>> segmentEnd = trackSegments.get(i);
					ArrayList<TrackNode<K>> segmentStart = trackSegments.get(j);
					TrackNode<K> end = segmentEnd.get(segmentEnd.size() - 1);
					TrackNode<K> start = segmentStart.get(0);
					
//					//debug
					System.out.println("### Gap Closing: ###");
//					System.out.println(end.toString());
//					System.out.println(start.toString());
					
					end.addParent(start);
					start.addChild(end);
					
//					// debug 
//					System.out.println(end.toString());
//					System.out.println(start.toString());
				} 
				
				// Case 2: Merging
				else if (j < (numTrackSegments + numMergingMiddlePoints)) {
					ArrayList<TrackNode<K>> segmentEnd = trackSegments.get(i);
					TrackNode<K> end =  segmentEnd.get(segmentEnd.size() - 1);
					TrackNode<K> middle = mergingMiddlePoints.get(j - numTrackSegments);
					
//					//debug
					System.out.println("### Merging: ###");
//					System.out.println(end.toString());
//					System.out.println(middle.toString());
					
					end.addChild(middle);
					middle.addParent(end);
					
//					//debug
//					System.out.println(end.toString());
//					System.out.println(middle.toString());
				}
			} else if (i < (numTrackSegments + numSplittingMiddlePoints)) {
				
				// Case 3: Splitting
				if (j < numTrackSegments) {
					TrackNode<K> mother = splittingMiddlePoints.get(i - numTrackSegments);
					ArrayList<TrackNode<K>> segmentStart = trackSegments.get(j);
					TrackNode<K> start = segmentStart.get(0);
					
//					//debug
					System.out.println("### Splitting: ###");
//					System.out.println(start.toString());
//					System.out.println(middle.toString());
					
					start.addChild(mother);
					mother.addParent(start);
	
//					// debug
//					System.out.println(start.toString());
//					System.out.println(middle.toString());
				}
			}
		}
	}
	
	
	/*
	 * This method creates an ArrayList< int[] > mimic of the ArrayList< ArrayList<K> > variable 'objects.'
	 * Thus, each internal int[] has the same length as the the corresponding ArrayList<K>.
	 * 
	 * The default value for each int[] is set to NOT_LINKED, unless it's the first frame
	 * (index 0), in which case we set it equal to SEGMENT_OF_SIZE_ONE. Normally, when
	 * a solution is in the top right quadrant, it signifies a segment ends, which is the 
	 * same in our case as leaving the value as -1. However, in the first round,
	 * if the solution is in the top right (object in t not paired with anything in t+1)
	 * we still should consider it to be a segment, thus we set everything to 
	 * SEGMENT_OF_SIZE_ONE in the first frame, and overwrite as we find links to the next
	 * frame.
	 */
	private ArrayList< int[] > initializeTrackSegments() {
		ArrayList< int[] > trackSegments = new ArrayList< int[] >();
		for (int i = 0; i < objects.size(); i++) {						
			int[] arr = new int[objects.get(i).size()];
			for (int j = 0; j < arr.length; j++) {						
				if (i == 0) {
					arr[j] = SEGMENT_OF_SIZE_ONE;
				} else {
					arr[j] = NOT_LINKED;
				}
			}
			trackSegments.add(arr);
		}
		return trackSegments;
	}
	
	
	/*
	 * This method takes a set of solutions from the Hungarian Algorithm (an int[][])
	 * for the track segment step (step 1), and extends the track segments accordingly.
	 * 
	 * The trackSegments object is an ArrayList of integer arrays. Each array is the same
	 * length as the number of Spots in the 'object' ArrayList at the same index. The way
	 * track segments are recorded is as follows:
	 * 
	 * If a Spot in frame t points to another Spot in frame t+1, then the corresponding
	 * integer array index (the same index as the Spot in frame t in the 'objects' ArrayList)
	 * will hold the index of the Spot in frame t+1 that it points to. 
	 * 
	 * Since the default value in all the int[] is -1, we can follow track segments by
	 * searching for values of -1, which signify the current segment ends, or that this point
	 * never points to anything else (never linked, or a segment of size one, which isn't
	 * important).
	 */
	private void extendTrackSegments(ArrayList< int[] > trackSegments, int[][] solutions, int i) {
		int[] t0 = trackSegments.get(i);
		int[] t1 = trackSegments.get(i + 1);
		
		for (int j = 0; j < solutions.length; j++) {
			int[] solution = solutions[j];
			
			// If the solution coordinates belong to the upper left quadrant
			if (solution[0] < t0.length) {
				if (solution[1] < t1.length) {
					t0[solution[0]] = solution[1];
				} 
			} 
			
			// If the solution coordinates belong to the lower left quadrant
			else if (solution[1] < t1.length) {
				t1[solution[1]] = SEGMENT_OF_SIZE_ONE;
			}
		}
	}
	
	
	/**
	 * Takes the TreeMap<Integer, Collection<K> > construction
	 * parameter, and converts it to an ArrayList< ArrayList<TrackNode<K>> >
	 * for the use in this class.
	 */
	private final ArrayList< ArrayList<TrackNode<K>> > convertMapToArrayList(TreeMap< Integer, ? extends Collection<TrackNode<K>> > objects) {
		ArrayList< ArrayList<TrackNode<K>> > newContainer = new ArrayList< ArrayList<TrackNode<K>> >(objects.size());
		Set<Integer> keys = objects.keySet();
		Collection<TrackNode<K>> spotThisFrame;
		ArrayList<TrackNode<K>> trackNodes;
		for (int key : keys) {
			spotThisFrame = objects.get(key);
			trackNodes = new ArrayList<TrackNode<K>>(spotThisFrame.size());
			for (TrackNode<K> node : spotThisFrame)
				trackNodes.add(node);
			newContainer.add(key, trackNodes);
		}
		return newContainer;
	}
	
	
	// For testing!
	public static void main(String args[]) {
		
		final boolean useCustomCostMatrices = false;
		
		// 1 - Set up test spots
		ArrayList<TrackNode<SpotImp>> t0 = new ArrayList<TrackNode<SpotImp>>();
		ArrayList<TrackNode<SpotImp>> t1 = new ArrayList<TrackNode<SpotImp>>();
		ArrayList<TrackNode<SpotImp>> t2 = new ArrayList<TrackNode<SpotImp>>();
		ArrayList<TrackNode<SpotImp>> t3 = new ArrayList<TrackNode<SpotImp>>();
		ArrayList<TrackNode<SpotImp>> t4 = new ArrayList<TrackNode<SpotImp>>();
		ArrayList<TrackNode<SpotImp>> t5 = new ArrayList<TrackNode<SpotImp>>();

		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {0,0,0})));
		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {1,1,1})));
		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2,2,2})));
		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {3,3,3})));
		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {4,4,4})));
		t0.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {5,5,5})));
		
		t0.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 100);
		t0.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 200);
		t0.get(2).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t0.get(3).getObject().putFeature(Feature.MEAN_INTENSITY, 400);
		t0.get(4).getObject().putFeature(Feature.MEAN_INTENSITY, 500);
		t0.get(5).getObject().putFeature(Feature.MEAN_INTENSITY, 600);
		
		t1.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {1.5f,1.5f,1.5f})));
		t1.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.5f,2.5f,2.5f})));
		t1.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {3.5f,3.5f,3.5f})));
		t1.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {4.5f,4.5f,4.5f})));
		
		t1.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 200);
		t1.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t1.get(2).getObject().putFeature(Feature.MEAN_INTENSITY, 400);
		t1.get(3).getObject().putFeature(Feature.MEAN_INTENSITY, 500);
		
		t2.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {1.5f,1.5f,1.5f})));
		t2.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.5f,2.5f,2.5f})));
		t2.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {3.5f,3.5f,3.5f})));
		t2.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {4.5f,4.5f,4.5f})));
		t2.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {10f,10f,10f})));
		
		t2.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 200);
		t2.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t2.get(2).getObject().putFeature(Feature.MEAN_INTENSITY, 400);
		t2.get(3).getObject().putFeature(Feature.MEAN_INTENSITY, 500);
		t2.get(4).getObject().putFeature(Feature.MEAN_INTENSITY, 100);
		
		t3.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.6f,2.6f,2.6f})));
		t3.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.4f,2.4f,2.4f})));
		
		t3.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t3.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		
		t4.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.8f,2.8f,2.8f})));
		t4.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.2f,2.2f,2.2f})));
		
		t4.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t4.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		
		t5.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.8f,2.8f,2.8f})));
		t5.add(new TrackNodeImp<SpotImp>(new SpotImp(new float[] {2.2f,2.2f,2.2f})));
		
		t5.get(0).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
		t5.get(1).getObject().putFeature(Feature.MEAN_INTENSITY, 300);
	
		TreeMap<Integer, ArrayList<TrackNode<SpotImp>>> wrap = new TreeMap<Integer, ArrayList<TrackNode<SpotImp>>>();
		wrap.put(0, t0);
		wrap.put(1, t1);
		wrap.put(2, t2);
		wrap.put(3, t3);
		wrap.put(4, t4);
		wrap.put(5, t5);
		
		int count = 0;
		Set<Integer> keys = wrap.keySet();
		for (int key : keys) {
			ArrayList<TrackNode<SpotImp>> spots = wrap.get(key);
			for (TrackNode<SpotImp> node : spots) {
				node.getObject().setFrame(count);
			}
			count++;
		}
		
		
		// 2 - Track the test spots
		
		if (!useCustomCostMatrices) {
			LAPTracker<SpotImp> lap = new LAPTracker<SpotImp>(wrap);
			if (!lap.checkInput() || !lap.process()) {
				System.out.println(lap.getErrorMessage());
			}
			
			// Print out track segments
//			ArrayList<ArrayList<Spot>> trackSegments = lap.getTrackSegments();
//			for (ArrayList<Spot> trackSegment : trackSegments) {
//				System.out.println("-*-*-*-*-* New Segment *-*-*-*-*-");
//				for (Spot spot : trackSegment) {
//					//System.out.println(spot.toString());
//					System.out.println(MathLib.printCoordinates(spot.getCoordinates()) + ", Frame [" + spot.getFrame() + "]");
//				}
//			}
		} else {
			
			LAPTracker<SpotImp> lap = new LAPTracker<SpotImp>(wrap);
			
			// Get linking costs
			ArrayList<double[][]> linkingCosts = new ArrayList<double[][]>();
			Settings settings = new Settings();
			for (int i = 0; i < wrap.size() - 1; i++) {
				ArrayList<TrackNode<SpotImp>> x = wrap.get(i);
				ArrayList<TrackNode<SpotImp>> y = wrap.get(i+1);
				LinkingCostMatrixCreator<SpotImp> l = new LinkingCostMatrixCreator<SpotImp>(x, y, settings);
				l.checkInput();
				l.process();
				linkingCosts.add(l.getCostMatrix());
			}
			
			// Link objects to track segments
			lap.setLinkingCosts(linkingCosts);
			lap.checkInput();
			lap.linkObjectsToTrackSegments();
			ArrayList< ArrayList<TrackNode<SpotImp>> > tSegs = lap.getTrackSegments();
			
			// Print out track segments
//			for (ArrayList<Spot> trackSegment : tSegs) {
//				System.out.println("-*-*-*-*-* New Segment *-*-*-*-*-");
//				for (Spot spot : trackSegment) {
//					//System.out.println(spot.toString());
//					System.out.println(MathLib.printCoordinates(spot.getCoordinates()));
//				}
//			}
			
			// Get segment costs
			TrackSegmentCostMatrixCreator<SpotImp> segCosts = new TrackSegmentCostMatrixCreator<SpotImp>(tSegs, settings);
			segCosts.checkInput();
			segCosts.process();
			double[][] segmentCosts = segCosts.getCostMatrix();
			
			// Link track segments to final tracks
			lap.setSegmentCosts(segmentCosts);
			lap.linkTrackSegmentsToFinalTracks();
			
		}

		
		// 3 - Print out results for testing
		
		// Print out final tracks.
		int counter = 1;
		System.out.println();
		for (int key : keys) {
			ArrayList<TrackNode<SpotImp>> spots = wrap.get(key);
			System.out.println("--- Frame " + counter + " ---");
			System.out.println("Number of Spots in this frame: " + spots.size());
			for (TrackNode<SpotImp> spot : spots) {
				System.out.println(spot.getObject().toString());
			}
			System.out.println();
			counter++;
		}
	}
}

