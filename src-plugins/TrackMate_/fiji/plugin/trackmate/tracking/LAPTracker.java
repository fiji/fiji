package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.costmatrix.LinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.costmatrix.TrackSegmentCostMatrixCreator;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
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
 * <p>To use the default cost matrices/function, use the default constructor,
 * and simply call {@link #process()}.
 * 
 * <p>If you wish to using your specify your own cost matrices:
 * 
 *  <ol>
 *  <li>Instantiate this class normally.
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
public class LAPTracker extends AbstractSpotTracker {

	/** The cost matrix for linking individual objects (step 1), indexed by the first frame index. */
	protected TreeMap<Integer, double[][]> linkingCosts = null;
	/** The cost matrix for linking individual track segments (step 2). */
	protected double[][] segmentCosts = null;
	/** Stores the objects to track as a list of Spots per frame.  */

	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** Stores whether the default cost matrices from the paper should be used,
	 * or if the user will supply their own. */
	protected boolean defaultCosts = true;
	/** Holds references to the middle spots in the track segments. */
	protected List<Spot> middlePoints;
	/** Holds references to the middle spots considered for merging in
	 * the track segments. */
	protected List<Spot> mergingMiddlePoints;	
	/** Holds references to the middle spots considered for splitting in 
	 * the track segments. */
	protected List<Spot> splittingMiddlePoints;
	/** Each index corresponds to a Spot in middleMergingPoints, and holds
	 * the track segment index that the middle point belongs to. */
	protected int[] mergingMiddlePointsSegmentIndices;
	/** Each index corresponds to a Spot in middleSplittingPoints, and holds
	 * the track segment index that the middle point belongs to. */
	protected int[] splittingMiddlePointsSegmentIndices;
	
	
	private final static String BASE_ERROR_MESSAGE = "LAPTracker: ";
	private static final boolean DEBUG = false;

	

	/*
	 * CONSTRUCTORS
	 */

	/** 
	 * Default constructor.
	 * 
	 * @param objects Holds a list of Spots for each frame in the time-lapse image.
	 * @param linkingCosts The cost matrix for step 1, linking objects, specified for every frame.
	 * @param settings The settings to use for this tracker.
	 */
	public LAPTracker (SpotCollection spots, TrackerSettings settings) {
		super(settings);
		this.spots = spots;
		// Add all spots to the graph
		for(int frame : spots.keySet())
			for(Spot spot : spots.get(frame))
				trackGraph.addVertex(spot);
	}


	public LAPTracker (SpotCollection spots) {
		this(spots, new TrackerSettings());
	}


	/*
	 * METHODS
	 */


	/**
	 * Set the cost matrices used for step 1, linking objects into track segments.
	 * <p>
	 * The matrices are indexed by frame: if a matrix links spots at frame t0 with spots at frame t1,
	 * then it will be put at index t0 in this {@link TreeMap}. The individual matrices must have 
	 * the proper dimension, set by the number of spot in t0 and t1. 
	 * @param linkingCosts The cost matrix, with structure matching figure 1b in the paper.
	 */
	public void setLinkingCosts(TreeMap<Integer, double[][]> linkingCosts) {
		this.linkingCosts = linkingCosts;
	}


	/**
	 * Get the cost matrices used for step 1, linking objects into track segments.
	 * @return The cost matrices, with one <code>double[][]</code> in the ArrayList for each frame t, t+1 pair.
	 */
	public TreeMap<Integer, double[][]> getLinkingCosts() {
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
	 * Returns the track segments computed from step (1).
	 * @return Returns a reference to the track segments, or null if {@link #computeTrackSegments()}
	 * hasn't been executed.
	 */
	public List<SortedSet<Spot>> getTrackSegments() {
		return trackSegments;
	}


	@Override
	public boolean checkInput() {
		// Check that the objects list itself isn't null
		if (null == spots) {
			errorMessage = BASE_ERROR_MESSAGE + "The spot list is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if (spots.isEmpty()) {
			errorMessage = BASE_ERROR_MESSAGE + "The spot list is empty.";
			return false;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for (int frame : spots.keySet()) {
			if (!spots.get(frame).isEmpty()) {
				empty = false;
				break;
			}
		}
		if (empty) {
			errorMessage = BASE_ERROR_MESSAGE + "The spot list is empty.";
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
		long tend, tstart;

		// Make sure checkInput() has been executed.
		if (!inputChecked) {
			errorMessage = BASE_ERROR_MESSAGE + "checkInput() must be executed before process().";
			return false;
		}

		// Step 1 - Link objects into track segments

		// Create cost matrices
		tstart = System.currentTimeMillis();
		if (!createLinkingCostMatrices()) return false;
		tend = System.currentTimeMillis();
		logger.log(String.format("  Cost matrix for frame-to-frame linking created in %.1f s.\n", (tend-tstart)/1e3f));

		// Solve LAP
		tstart = System.currentTimeMillis();
		if (!linkObjectsToTrackSegments()) return false;
		tend = System.currentTimeMillis();
		logger.log(String.format("  Frame to frame LAP solved in %.1f s.\n", (tend-tstart)/1e3f));


		// Step 2 - Link track segments into final tracks

		// Create cost matrix
		tstart = System.currentTimeMillis();
		if (!createTrackSegmentCostMatrix()) return false;
		tend = System.currentTimeMillis();
		logger.log(String.format("  Cost matrix for track segments created in %.1f s.\n", (tend-tstart)/1e3f));

		// Solve LAP
		tstart = System.currentTimeMillis();
		if (!linkTrackSegmentsToFinalTracks()) return false;
		tend = System.currentTimeMillis();
		logger.log(String.format("  Track segment LAP solved in %.1f s.\n", (tend-tstart)/1e3f));

		return true;
	}


	/**
	 * Creates the cost matrices used to link objects (Step 1) for each frame pair.
	 * Calling this method resets the {@link #linkingCosts} field, which stores these 
	 * matrices.
	 * @return True if executes successfully, false otherwise.
	 */
	private boolean createLinkingCostMatrices() {
		linkingCosts = new TreeMap<Integer, double[][]>();
		List<Spot> t0, t1;
		LinkingCostMatrixCreator objCosts;
		double[][] costMatrix = null;

		// Iterate properly over frame pair in order, not necessarily separated by 1.
		Iterator<Integer> frameIterator = spots.keySet().iterator(); 		
		int frame0 = frameIterator.next();
		int frame1;
		while(frameIterator.hasNext()) { // ascending order

			frame1 = frameIterator.next();
			t0 = spots.get(frame0);
			t1 = spots.get(frame1);

			objCosts = new LinkingCostMatrixCreator(t0, t1, settings);
			if (!objCosts.checkInput() || !objCosts.process()) {
				errorMessage = objCosts.getErrorMessage();
				return false;
			}
			costMatrix = objCosts.getCostMatrix();
			linkingCosts.put(frame0, costMatrix);
			frame0 = frame1;
		}
		return true;
	}




	/**
	 * Creates the cost matrix used to link track segments (step 2).
	 * @return True if executes successfully, false otherwise.
	 */
	private boolean createTrackSegmentCostMatrix() {
		TrackSegmentCostMatrixCreator segCosts = new TrackSegmentCostMatrixCreator(trackSegments, settings);
		if (!segCosts.checkInput() || !segCosts.process()) {
			errorMessage = BASE_ERROR_MESSAGE + segCosts.getErrorMessage();
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
		solveLAPForTrackSegments();

		// Compile LAP solutions into track segments
		compileTrackSegments();

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
		
		if (DEBUG) {
			LAPUtils.displayCostMatrix(segmentCosts, trackSegments.size(), splittingMiddlePoints.size(), settings.blockingValue, finalTrackSolutions);
		}

		// Compile LAP solutions into final tracks
		compileFinalTracks(finalTrackSolutions);

		return true;
	}


	/**
	 * Compute the optimal track segments using the cost matrix {@link LAPTracker#linkingCosts}.
	 * Update the {@link #trackGraph} field.
	 * 
	 * @see LAPTracker#createLinkingCostMatrices()
	 */
	public void solveLAPForTrackSegments() {
		final AssignmentAlgorithm solver = new HungarianAlgorithm();
		// Iterate properly over frame pair in order, not necessarily separated by 1.
		Iterator<Integer> frameIterator = spots.keySet().iterator(); 
		DefaultWeightedEdge edge;
		int frame0 = frameIterator.next();
		int frame1;
		double weight;
		while(frameIterator.hasNext()) { // ascending order

			double[][] costMatrix = linkingCosts.get(frame0);
			AssignmentProblem problem = new AssignmentProblem(costMatrix);
			int[][] solutions = problem.solve(solver);			
			frame1 = frameIterator.next();			

			// Extend track segments using solutions: we update the graph edges
			List<Spot> t0 = spots.get(frame0);
			List<Spot> t1 = spots.get(frame1);
			System.out.println("LAPTracker: spots are: "+frame0+": "+t0.size()+" spots - "+frame1+": "+t1.size()+" spots.");// DEBUG
			for (int i = 0; i < solutions.length; i++) {
				if (solutions[i].length == 0)
					continue;
				int i0 = solutions[i][0];
				int i1 = solutions[i][1];
				
				if (i0 < t0.size() && i1 < t1.size() ) {
					// Solution belong to the upper-left quadrant: we can connect the spots
					Spot s0 = t0.get(i0);
					Spot s1 = t1.get(i1);
					edge = trackGraph.addEdge(s0, s1);
					if (null == edge) {
						System.out.println("Could not add edge between spot "+s0+" and spot "+s1);// DEBUG
						continue;						
					}
					// We set the edge weight to be the linking cost, for future reference. 
					// This is NOT used in further tracking steps
					weight = costMatrix[i0][i1]; 
					trackGraph.setEdgeWeight(edge, weight);
				} // otherwise we do not create any connection
			}

			// Next frame pair
			frame0 = frame1;
		}
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


	/**
	 * Uses DFS approach to create a List of track segments from the overall 
	 * result of step 1
	 * <p> 
	 * We have recorded the tracks as edges in the track graph, we now turn them
	 * into multiple explicit sets of Spots, sorted by their {@link Feature#POSITION_T}.
	 */
	private void compileTrackSegments() {

		trackSegments = new ArrayList<SortedSet<Spot>>();

		Collection<Spot> spotPool = new ArrayList<Spot>();
		for(int frame : spots.keySet())
			spotPool.addAll(spots.get(frame)); // frame info lost

		Spot source, current;
		DepthFirstIterator<Spot, DefaultWeightedEdge> graphIterator;
		SortedSet<Spot> trackSegment = null;

		while (!spotPool.isEmpty()) {
			source = spotPool.iterator().next();
			graphIterator = new DepthFirstIterator<Spot, DefaultWeightedEdge>(trackGraph, source); // restricted to connected components
			trackSegment = new TreeSet<Spot>(Spot.frameComparator);

			while(graphIterator.hasNext()) {
				current = graphIterator.next();
				trackSegment.add(current);
				spotPool.remove(current);
			}

			trackSegments.add(trackSegment);
		}
	}

	/**
	 * Takes the solutions from the Hungarian algorithm, which are an int[][], and 
	 * appropriately links the track segments. Before this method is called, the Spots in the
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
		DefaultWeightedEdge edge;
		double weight;

		if(DEBUG)  {
			System.out.println("-- DEBUG information from LAPTracker --");
			System.out.println("Compiling final tracks with "+numTrackSegments+" segments, "
					+numMergingMiddlePoints+" merging spot candidates, "+numSplittingMiddlePoints+" splitting spot condidates.");
		}
		for (int[] solution : finalTrackSolutions) {
			int i = solution[0];
			int j = solution[1];
			
			if (i < numTrackSegments) {

				// Case 1: Gap closing
				if (j < numTrackSegments) {
					SortedSet<Spot> segmentEnd = trackSegments.get(i);
					SortedSet<Spot> segmentStart = trackSegments.get(j);
					Spot end = segmentEnd.last();
					Spot start = segmentStart.first();
					edge = trackGraph.addEdge(end, start);
					// Set weight to be the linking cost
					weight = segmentCosts[i][j];
					trackGraph.setEdgeWeight(edge, weight);

					if(DEBUG) 
						System.out.println("Gap closing from segment "+i+" to segment "+j+".");

				} else if (j < (numTrackSegments + numMergingMiddlePoints)) {
					
					// Case 2: Merging
					SortedSet<Spot> segmentEnd = trackSegments.get(i);
					Spot end =  segmentEnd.last();
					Spot middle = mergingMiddlePoints.get(j - numTrackSegments);
					edge = trackGraph.addEdge(end, middle);
					// Set weight to be the linking cost
					weight = segmentCosts[i][j];
					trackGraph.setEdgeWeight(edge, weight);


					if(DEBUG) {
						SortedSet<Spot> track = null;
						int indexTrack = 0;
						int indexSpot = 0;
						for(SortedSet<Spot> t : trackSegments)
							if (t.contains(middle)) {
								track = t;
								for(Spot spot : track) {
									if (spot == middle)
										break;
									else
										indexSpot++;
								}
								break;
							} else
								indexTrack++;
						System.out.println("Merging from segment "+i+" end to spot "+indexSpot+" in segment "+indexTrack+".");
					}

				}
			} else if (i < (numTrackSegments + numSplittingMiddlePoints)) {

				// Case 3: Splitting
				if (j < numTrackSegments) {
					SortedSet<Spot> segmentStart = trackSegments.get(j);
					Spot start = segmentStart.first();
					Spot mother = splittingMiddlePoints.get(i - numTrackSegments);
					Spot target = mother;
					edge = trackGraph.addEdge(start, target);
					// Set weight to be the linking cost
					weight = segmentCosts[i][j];
					trackGraph.setEdgeWeight(edge, weight);


					if(DEBUG) {
						SortedSet<Spot> track = null;
						int indexTrack = 0;
						int indexSpot = 0;
						for(SortedSet<Spot> t : trackSegments)
							if (t.contains(mother)) {
								track = t;
								for(Spot spot : track) {
									if (spot == mother)
										break;
									else
										indexSpot++;
								}
								break;
							} else
								indexTrack++;
						System.out.println("Splitting from spot "+indexSpot+" in segment "+indexTrack+" to segment"+j+".");
					}
				}
			}
		}
	}
}

