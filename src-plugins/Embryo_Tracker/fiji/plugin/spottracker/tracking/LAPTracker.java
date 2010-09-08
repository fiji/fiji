package fiji.plugin.spottracker.tracking;

import java.util.ArrayList;
import java.util.Collection;

import mpicbg.imglib.algorithm.math.MathLib;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.tracking.costmatrix.LinkingCostMatrixCreator;
import fiji.plugin.spottracker.tracking.costmatrix.TrackSegmentCostMatrixCreator;
import fiji.plugin.spottracker.tracking.hungarian.AssignmentProblem;
import fiji.plugin.spottracker.tracking.hungarian.HungarianAlgorithm;

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
 * the cost-minimizing assignments. The result of the calculation are the complete
 * tracks of the objects. For more details on the Hungarian Algorithm, see
 * http://en.wikipedia.org/wiki/Hungarian_algorithm.
 * 
 * <p><b>Cost Matrices</b>
 * 
 * <p>
 * Since there are two discrete steps to tracking using this framework, two distinct
 * cost matrices are required to solve the problem. The user can either choose
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
 * the various track segments previously calculated. Track segments can be:
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
 * <p><b>How to use this class</b>
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
public class LAPTracker implements ObjectTracker {
	
	/** Used as a flag when building track segments to indicate a Spot is unlinked to
	 * the next frame. */
	protected static final int NOT_LINKED = -2;
	/** Used as a flag when building the track segments to indicate a track segment with
	 * only a single Spot. */
	protected static final int SEGMENT_OF_SIZE_ONE = -1;
	/** To throw out spurious segments, only include those with a length strictly larger
	 * than this value. */
	protected static final int MINIMUM_SEGMENT_LENGTH = 3; // TODO use this, make user input. Also, needs to be something to have a sensible merging/splitting section (need a middle...)

	/** The cost matrix for linking individual objects (step 1) */
	protected ArrayList<double[][]> linkingCosts = null;
	/** The cost matrix for linking individual track segments (step 2). */
	protected double[][] segmentCosts = null;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected ArrayList< ArrayList<Spot> > objects;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** Stores the track segments computed during step (1) of the algorithm */
	protected ArrayList< ArrayList<Spot> > trackSegments = null;
	/** Stores whether the default cost matrices from the paper should be used,
	 * or if the user will supply their own. */
	protected boolean defaultCosts = true;
	
	
	/**
	 * Use this constructor to use the cost-matrices described in the paper
	 * for Brownian motion.
	 * @param objects Holds a list of Spots for each frame in the time-lapse image.
	 */
	public LAPTracker (ArrayList< ArrayList<Spot> > objects) {
		this.objects = objects;
	}
	
	
	/** 
	 * Use this constructor if you want to supply your own cost matrices, and want to supply
	 * the linking cost matrix (step 1) at construction.
	 * @param objects Holds a list of Spots for each frame in the time-lapse image.
	 * @param linkingCosts The cost matrix for step 1, linking objects
	 */
	public LAPTracker (ArrayList< ArrayList<Spot> > objects, ArrayList<double[][]> linkingCosts) {
		this.objects = objects;
		this.linkingCosts = linkingCosts;
		this.defaultCosts = false;
	}
	
	
	/**
	 * This constructor should be used when not providing the object cost matrix (step 1) at 
	 * construction, but when planning to use non-default cost matrices.
	 * @param objects Holds a list of Spots for each frame in the time-lapse image.
	 * @param defaultCosts If true, the default matrices will be used. If false, the user must supply them.
	 */
	public LAPTracker (ArrayList< ArrayList<Spot> > objects, boolean defaultCosts) {
		this.objects = objects;
		this.defaultCosts = defaultCosts;
	}

	
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
	 * TODO describe the cost matrix, since this doens't match the paper's anymore.
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
	public ArrayList< ArrayList<Spot> > getTrackSegments() {
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
		
		// If not using the default costs, make sure the linking cost matrix exists
		if (!defaultCosts) {
			if (null == linkingCosts) {
				errorMessage = "No linking costs have been provided!";
				return false;
			}
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
		
		// 1.1 - Create cost matrices
		linkingCosts = new ArrayList<double[][]>();	
		for (int i = 0; i < objects.size() - 1; i++) {
			LinkingCostMatrixCreator objCosts = new LinkingCostMatrixCreator(new ArrayList<Spot>(objects.get(i)), new ArrayList<Spot>(objects.get(i + 1)));
			if (!objCosts.checkInput() || !objCosts.process()) {
				System.out.println(objCosts.getErrorMessage());
				return false;
			}
			linkingCosts.add(objCosts.getCostMatrix());
		}
		System.out.println("Cost matrix created for frame-to-frame linking successfully.");
		
		// 1.2 - Solve LAP
		if (!linkObjectsToTrackSegments()) return false;
		
		// Step 2 - Link track segments into final tracks
		
		System.out.println("--- Step two ---");
		
		// 2.1 - Create cost matrix
		TrackSegmentCostMatrixCreator segCosts = new TrackSegmentCostMatrixCreator(trackSegments);
		if (!segCosts.checkInput() || !segCosts.process()) {
			System.out.println(segCosts.getErrorMessage());
			return false;
		}
		segmentCosts = segCosts.getCostMatrix();
		System.out.println("Cost matrix for track segments created successfully.");
		
		// 2.2 - Solve LAP
		if (!linkTrackSegmentsToFinalTracks(segCosts.getMiddlePoints())) return false;
		
		return true;
		
//		Matrix debug = new Matrix(segmentCosts);
//		for (int i = 0; i < debug.getRowDimension(); i++) {
//			for (int j = 0; j < debug.getColumnDimension(); j++) {
//				if (Double.compare(Double.MAX_VALUE, debug.get(i,j)) == 0) {
//					debug.set(i, j, Double.NaN);
//				}
//			}
//		}
//		debug.print(4,2);
	}
	
	
	/**
	 * Creates the track segments computed from step 1.
	 * @return True if execution completes successfully.
	 */
	public boolean linkObjectsToTrackSegments() {

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
	public boolean linkTrackSegmentsToFinalTracks(ArrayList<Spot> middlePoints) {
		
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
		compileFinalTracks(finalTrackSolutions, middlePoints);
		
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
		 * It will later be converted into an ArrayList< ArrayList<Spot> > to explicitly
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
	 * Uses DFS approach to create ArrayList<Spot> track segments from the overall 
	 * result of step 1, which recorded the tracks in a series of int[]. This method
	 * converts the int[] track segments (a series of edges) into an explicit ArrayList
	 * of Spots.
	 */
	private void compileTrackSegments(ArrayList< int[] > trackSegments) {
		this.trackSegments = new ArrayList< ArrayList<Spot> >();

		for (int i = 0; i < trackSegments.size(); i++) {						// For all frames
			int[] currFrame = trackSegments.get(i);
			for (int j = 0; j < currFrame.length; j++) {						// For all Spots in frame
				if (currFrame[j] != NOT_LINKED) {								// If this Spot in linked to something in the next frame (!= -1)
					ArrayList<Spot> trackSegment = new ArrayList<Spot>();		// Start a new track segment
					
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
					
					if (trackSegment.size() >= MINIMUM_SEGMENT_LENGTH) {		
						// TODO probably incorporate this above, but this is faster to implement.
						// Link segment Spots to each other
						Spot prev = null;
						Spot curr = null;
						prev = trackSegment.get(0);
						for (int h = 1; h < trackSegment.size(); h++) {
							curr = trackSegment.get(h);
							prev.addNext(curr);
							curr.addPrev(prev);
							prev = curr;
						}
						
						this.trackSegments.add(trackSegment);						// When we're here, eventually the current index was -1, so the track ended. Add the track to the list of tracks.
					}
				}
			}
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
	private void compileFinalTracks(int[][] finalTrackSolutions, ArrayList<Spot> middlePoints) {
		final int numTrackSegments = trackSegments.size();
		final int numMiddlePoints = middlePoints.size();
		
		for (int[] solution : finalTrackSolutions) {
			int i = solution[0];
			int j = solution[1];
//			//debug
//			System.out.println(String.format("i = %d, j = %d", i, j));
			if (i < numTrackSegments) {
				
				// Case 1: Gap closing
				if (j < numTrackSegments) {
					ArrayList<Spot> segmentEnd = trackSegments.get(i);
					ArrayList<Spot> segmentStart = trackSegments.get(j);
					Spot end = segmentEnd.get(segmentEnd.size() - 1);
					Spot start = segmentStart.get(0);
					
//					//debug
					System.out.println("### Gap Closing: ###");
//					System.out.println(end.toString());
//					System.out.println(start.toString());
					
					end.addPrev(start);
					start.addNext(end);
					
//					// debug 
//					System.out.println(end.toString());
//					System.out.println(start.toString());
				} 
				
				// Case 2: Merging
				else if (j < (numTrackSegments + numMiddlePoints)) {
					ArrayList<Spot> segmentEnd = trackSegments.get(i);
					Spot end =  segmentEnd.get(segmentEnd.size() - 1);
					Spot middle = middlePoints.get(j - numTrackSegments);
					
//					//debug
					System.out.println("### Merging: ###");
//					System.out.println(end.toString());
//					System.out.println(middle.toString());
					
					end.addNext(middle);
					middle.addPrev(end);
					
//					//debug
//					System.out.println(end.toString());
//					System.out.println(middle.toString());
				}
			} else if (i < (numTrackSegments + numMiddlePoints)) {
				
				// Case 3: Splitting
				if (j < numTrackSegments) {
					Spot middle = middlePoints.get(i - numTrackSegments);
					ArrayList<Spot> segmentStart = trackSegments.get(j);
					Spot start = segmentStart.get(0);
					
//					//debug
					System.out.println("### Splitting: ###");
//					System.out.println(start.toString());
//					System.out.println(middle.toString());
					
					start.addPrev(middle);
					middle.addNext(start);
	
//					// debug
//					System.out.println(start.toString());
//					System.out.println(middle.toString());
				}
			}
		}
	}
	
	
	/*
	 * This method creates an ArrayList< int[] > mimic of the ArrayList< ArrayList<Spot> > variable 'objects.'
	 * Thus, each internal int[] has the same length as the the corresponding ArrayList<Spot>.
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
	
	
	// For testing!
	public static void main(String args[]) {
		
		final boolean useCustomCostMatrices = false;
		
		// 1 - Set up test spots
		ArrayList<Spot> t0 = new ArrayList<Spot>();
		ArrayList<Spot> t1 = new ArrayList<Spot>();
		ArrayList<Spot> t2 = new ArrayList<Spot>();
		ArrayList<Spot> t3 = new ArrayList<Spot>();
		ArrayList<Spot> t4 = new ArrayList<Spot>();
		ArrayList<Spot> t5 = new ArrayList<Spot>();

		t0.add(new Spot(new float[] {0,0,0}));
		t0.add(new Spot(new float[] {1,1,1}));
		t0.add(new Spot(new float[] {2,2,2}));
		t0.add(new Spot(new float[] {3,3,3}));
		t0.add(new Spot(new float[] {4,4,4}));
		t0.add(new Spot(new float[] {5,5,5}));
		
		t0.get(0).putFeature(Feature.MEAN_INTENSITY, 100);
		t0.get(1).putFeature(Feature.MEAN_INTENSITY, 200);
		t0.get(2).putFeature(Feature.MEAN_INTENSITY, 300);
		t0.get(3).putFeature(Feature.MEAN_INTENSITY, 400);
		t0.get(4).putFeature(Feature.MEAN_INTENSITY, 500);
		t0.get(5).putFeature(Feature.MEAN_INTENSITY, 600);
		
		t1.add(new Spot(new float[] {1.5f,1.5f,1.5f}));
		t1.add(new Spot(new float[] {2.5f,2.5f,2.5f}));
		t1.add(new Spot(new float[] {3.5f,3.5f,3.5f}));
		t1.add(new Spot(new float[] {4.5f,4.5f,4.5f}));
		
		t1.get(0).putFeature(Feature.MEAN_INTENSITY, 200);
		t1.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		t1.get(2).putFeature(Feature.MEAN_INTENSITY, 400);
		t1.get(3).putFeature(Feature.MEAN_INTENSITY, 500);
		
		t2.add(new Spot(new float[] {1.5f,1.5f,1.5f}));
		t2.add(new Spot(new float[] {2.5f,2.5f,2.5f}));
		t2.add(new Spot(new float[] {3.5f,3.5f,3.5f}));
		t2.add(new Spot(new float[] {4.5f,4.5f,4.5f}));
		t2.add(new Spot(new float[] {10f,10f,10f}));
		
		t2.get(0).putFeature(Feature.MEAN_INTENSITY, 200);
		t2.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		t2.get(2).putFeature(Feature.MEAN_INTENSITY, 400);
		t2.get(3).putFeature(Feature.MEAN_INTENSITY, 500);
		t2.get(4).putFeature(Feature.MEAN_INTENSITY, 100);
		
		t3.add(new Spot(new float[] {2.6f,2.6f,2.6f}));
		t3.add(new Spot(new float[] {2.4f,2.4f,2.4f}));
		
		t3.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t3.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		
		t4.add(new Spot(new float[] {2.8f,2.8f,2.8f}));
		t4.add(new Spot(new float[] {2.2f,2.2f,2.2f}));
		
		t4.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t4.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		
		t5.add(new Spot(new float[] {2.8f,2.8f,2.8f}));
		t5.add(new Spot(new float[] {2.2f,2.2f,2.2f}));
		
		t5.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t5.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
	
		ArrayList<ArrayList<Spot>> wrap = new ArrayList<ArrayList<Spot>>();
		wrap.add(t0);
		wrap.add(t1);
		wrap.add(t2);
		wrap.add(t3);
		wrap.add(t4);
		wrap.add(t5);
		
		int count = 0;
		for (ArrayList<Spot> spots : wrap) {
			for (Spot spot : spots) {
				spot.setFrame(count);
			}
			count++;
		}
		
		
		// 2 - Track the test spots
		
		if (!useCustomCostMatrices) {
			LAPTracker lap = new LAPTracker(wrap);
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
			
			LAPTracker lap = new LAPTracker(wrap, false);
			
			// Get linking costs
			ArrayList<double[][]> linkingCosts = new ArrayList<double[][]>();
			for (int i = 0; i < wrap.size() - 1; i++) {
				ArrayList<Spot> x = wrap.get(i);
				ArrayList<Spot> y = wrap.get(i+1);
				LinkingCostMatrixCreator l = new LinkingCostMatrixCreator(x, y);
				l.checkInput();
				l.process();
				linkingCosts.add(l.getCostMatrix());
			}
			
			// Link objects to track segments
			lap.setLinkingCosts(linkingCosts);
			lap.checkInput();
			lap.linkObjectsToTrackSegments();
			ArrayList< ArrayList<Spot> > tSegs = lap.getTrackSegments();
			
			// Print out track segments
//			for (ArrayList<Spot> trackSegment : tSegs) {
//				System.out.println("-*-*-*-*-* New Segment *-*-*-*-*-");
//				for (Spot spot : trackSegment) {
//					//System.out.println(spot.toString());
//					System.out.println(MathLib.printCoordinates(spot.getCoordinates()));
//				}
//			}
			
			// Get segment costs
			TrackSegmentCostMatrixCreator segCosts = new TrackSegmentCostMatrixCreator(tSegs);
			segCosts.checkInput();
			segCosts.process();
			double[][] segmentCosts = segCosts.getCostMatrix();
			
			// Link track segments to final tracks
			lap.setSegmentCosts(segmentCosts);
			lap.linkTrackSegmentsToFinalTracks(segCosts.getMiddlePoints());
			
		}

		
		// 3 - Print out results for testing
		
		// Print out final tracks.
//		int counter = 1;
//		System.out.println();
//		for (ArrayList<Spot> spots : wrap) {
//			System.out.println("--- Frame " + counter + " ---");
//			System.out.println("Number of Spots in this frame: " + spots.size());
//			for (Spot spot : spots) {
//				System.out.println(spot.toString());
//			}
//			System.out.println();
//			counter++;
//		}
	}
}

