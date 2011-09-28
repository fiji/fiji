package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeature;
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
public class LAPTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker {

	private final static String BASE_ERROR_MESSAGE = "LAPTracker: ";
	private static final boolean DEBUG = false;

	/** Logger used to echo progress on tracking. */
	protected Logger logger = Logger.DEFAULT_LOGGER;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;

	/** The cost matrix for linking individual track segments (step 2). */
	protected double[][] segmentCosts = null;
	/** Stores the objects to track as a list of Spots per frame.  */

	/** Stores whether the default cost matrices from the paper should be used,
	 * or if the user will supply their own. */
	protected boolean defaultCosts = true;
	/**
	 * Store the track segments computed during step (1) of the algorithm.
	 * <p>
	 * In individual segments, spots are put in a {@link SortedSet} so that
	 * they are retrieved by frame order when iterated over.
	 * <p>
	 * The segments are put in a list, for we need to have them indexed to build
	 * a cost matrix for segments in the step (2) of the algorithm.
	 */
	protected List<SortedSet<Spot>> trackSegments = null;
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
	/** The graph this tracker will use to link spots. */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	/** The Spot collection that will be linked in the {@link #graph.} */
	protected SpotCollection spots;
	/** The settings object that configures this tracker. */
	protected TrackerSettings settings;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;


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
	public LAPTracker(final SpotCollection spots, final TrackerSettings settings) {
		super();
		if (!useMultithreading) {
			setNumThreads(1);
		}
		this.spots = spots;
		this.settings = settings;
		reset();
	}

	/*	
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Generate the assignment algorithm that will be used to solve 
	 * the {@link AssignmentProblem} held by this tracker.
	 * <p>
	 * Here, by default, it returns the Hungarian algorithm implementation by Gary Baker and Nick
	 * Perry that solves an assignment problem in O(n^4).  
	 */
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new HungarianAlgorithm();
	}



	/*
	 * METHODS
	 */

	/**
	 * Reset any link created in the graph result in this tracker, effectively creating a new graph, 
	 * containing the spots but no edge.
	 */
	public void reset() {
		graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for(Spot spot : spots) 
			graph.addVertex(spot);
	}

	public SimpleWeightedGraph<Spot,DefaultWeightedEdge> getResult() {
		return graph;
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
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if (spots.isEmpty()) {
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
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
			errorMessage = BASE_ERROR_MESSAGE + "The filtered spot collection is empty.";
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

		reset();

		// Step 1 - Link objects into track segments
		tstart = System.currentTimeMillis();
		if (!linkObjectsToTrackSegments()) return false;
		tend = System.currentTimeMillis();
		logger.log(String.format("  Frame to frame LAP solved in %.1f s.\n", (tend-tstart)/1e3f));

		// Skip 2nd step if there is no rules to link track segments
		if (!settings.allowGapClosing && !settings.allowSplitting && !settings.allowMerging) {
			return true;
		}
		
		// Step 2 - Link track segments into final tracks

		// Create cost matrix
		tstart = System.currentTimeMillis();
		if (!createTrackSegmentCostMatrix()) return false;
		tend = System.currentTimeMillis();
		logger.setProgress(0.75f);
		logger.log(String.format("  Cost matrix for track segments created in %.1f s.\n", (tend-tstart)/1e3f));

		// Solve LAP
		tstart = System.currentTimeMillis();
		if (!linkTrackSegmentsToFinalTracks()) return false;
		tend = System.currentTimeMillis();
		logger.setProgress(1);
		logger.setStatus("");
		logger.log(String.format("  Track segment LAP solved in %.1f s.\n", (tend-tstart)/1e3f));

		return true;
	}


	/**
	 * Creates the cost matrices used to link objects (Step 1) for each frame pair.
	 * Calling this method resets the {@link #linkingCosts} field, which stores these 
	 * matrices.
	 * @return True if executes successfully, false otherwise.
	 */
	/*
	public boolean createLinkingCostMatrices() {
		linkingCosts = Collections.synchronizedSortedMap(new TreeMap<Integer, double[][]>());

		// Prepare frame pairs in order, not necessarily separated by 1.
		final ArrayList<int[]> framePairs = new ArrayList<int[]>(spots.keySet().size()-1);
		final Iterator<Integer> frameIterator = spots.keySet().iterator(); 		
		int frame0 = frameIterator.next();
		int frame1;
		while(frameIterator.hasNext()) { // ascending order
			frame1 = frameIterator.next();
			framePairs.add( new int[] {frame0, frame1} );
			frame0 = frame1;
		}

		// Prepare threads
		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("LAPTracker linking cost thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < framePairs.size(); i = ai.getAndIncrement()) {

						int frame0 = framePairs.get(i)[0];
						int frame1 = framePairs.get(i)[1];
						List<Spot> t0 = spots.get(frame0);
						List<Spot> t1 = spots.get(frame1);

						LinkingCostMatrixCreator objCosts = new LinkingCostMatrixCreator(t0, t1, settings);
						if (!objCosts.checkInput() || !objCosts.process()) {
							errorMessage = objCosts.getErrorMessage();
							return;
						}
						double[][] costMatrix = objCosts.getCostMatrix();
						linkingCosts.put(frame0, costMatrix);

						logger.setProgress(0 + 0.25f * progress.incrementAndGet() / (float) framePairs.size());
					}
				}
			};
		}

		logger.setStatus("Creating linking cost matrices...");
		logger.setProgress(0);
		SimpleMultiThreading.startAndJoin(threads);
		logger.setProgress(0.25f);
		logger.setStatus("");
		return true;
	}
	 */

	/**
	 * Creates the cost matrix used to link track segments (step 2).
	 * @return True if executes successfully, false otherwise.
	 */
	public boolean createTrackSegmentCostMatrix() {
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

		// Solve LAP
		if (!solveLAPForTrackSegments()) {
			return false;
		}

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
	 * Perform the frame to frame linking. 
	 * <p>
	 * For each frame, compute the cost matrix to link each spot to another spot in the next frame.
	 * Then compute the optimal track segments using this cost matrix.
	 * Finally, update the {@link #trackGraph} field with found links.
	 * 
	 * @see LAPTracker#createFrameToFrameLinkingCostMatrix(List, List, TrackerSettings)
	 */
	public boolean solveLAPForTrackSegments() {

		// Prepare frame pairs in order, not necessarily separated by 1.
		final ArrayList<int[]> framePairs = new ArrayList<int[]>(spots.keySet().size()-1);
		final Iterator<Integer> frameIterator = spots.keySet().iterator(); 		
		int frame0 = frameIterator.next();
		int frame1;
		while(frameIterator.hasNext()) { // ascending order
			frame1 = frameIterator.next();
			framePairs.add( new int[] {frame0, frame1} );
			frame0 = frame1;
		}

		// Prepare threads
		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("LAPTracker track segment linking thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < framePairs.size(); i = ai.getAndIncrement()) {

						// Get frame pairs
						final int frame0 = framePairs.get(i)[0];
						final int frame1 = framePairs.get(i)[1];

						// Get spots
						final List<Spot> t0 = spots.get(frame0);
						final List<Spot> t1 = spots.get(frame1);
						
						// Create cost matrix
						double[][] costMatrix = createFrameToFrameLinkingCostMatrix(t0, t1, settings);
						
						// Special case: top-left corner of the cost matrix is all blocked: we do nothing for this pair
						// We handle this special case here, because some solvers might hang with this.
						boolean allBlocked = true;
						for (int j = 0; j < t0.size(); j++) {
							for (int k = 0; k < t1.size(); k++) {
								if (costMatrix[j][k] != settings.blockingValue) {
									allBlocked = false;
									break;
								}
								if (!allBlocked)
									break;
							}
						}

						if (!allBlocked) {
							// Find solution
							AssignmentProblem problem = new AssignmentProblem(costMatrix);
							AssignmentAlgorithm solver = createAssignmentProblemSolver();
							int[][] solutions = problem.solve(solver);

							// Extend track segments using solutions: we update the graph edges
							for (int j = 0; j < solutions.length; j++) {
								if (solutions[j].length == 0)
									continue;
								int i0 = solutions[j][0];
								int i1 = solutions[j][1];

								if (i0 < t0.size() && i1 < t1.size() ) {
									// Solution belong to the upper-left quadrant: we can connect the spots
									Spot s0 = t0.get(i0);
									Spot s1 = t1.get(i1);
									// We set the edge weight to be the linking cost, for future reference. 
									// This is NOT used in further tracking steps
									double weight = costMatrix[i0][i1];
									synchronized (graph) { // To avoid concurrent access, sad but true
										DefaultWeightedEdge edge = graph.addEdge(s0, s1);
										graph.setEdgeWeight(edge, weight);
									}
								} // otherwise we do not create any connection
							}
						}
						logger.setProgress(0.5f * progress.incrementAndGet() / (float) framePairs.size());

					}
				}
			};

		}

		logger.setStatus("Solving for track segments...");
		SimpleMultiThreading.startAndJoin(threads);
		logger.setProgress(0.5f);
		logger.setStatus("");
		return true;
	}


	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the cost matrix required in the frame to frame linking.
	 *  
	 * @param t0  the list of spots in the first frame 
	 * @param t1  the list of spots in the second frame 
	 * @param settings  the tracker settings that specifies how this cost should be created
	 * @return  the cost matrix as an array of array of double
	 */
	protected double[][] createFrameToFrameLinkingCostMatrix(final List<Spot> t0, List<Spot> t1, final TrackerSettings settings) {
		// Create cost matrix
		LinkingCostMatrixCreator objCosts = new LinkingCostMatrixCreator(t0, t1, settings);
		if (!objCosts.checkInput() || !objCosts.process()) {
			errorMessage = BASE_ERROR_MESSAGE + objCosts.getErrorMessage();
			return null;
		}
		return objCosts.getCostMatrix();
	}

	


	/**
	 * Compute the optimal final track using the cost matrix 
	 * {@link LAPTracker#segmentCosts}.
	 * @return True if executes correctly, false otherwise.
	 */
	public int[][] solveLAPForFinalTracks() {
		// Solve the LAP using the Hungarian Algorithm
		logger.setStatus("Solving for final tracks...");
		AssignmentProblem problem = new AssignmentProblem(segmentCosts);
		AssignmentAlgorithm solver = createAssignmentProblemSolver();
		int[][] solutions = problem.solve(solver);
		return solutions;
	}


	/**
	 * Uses DFS approach to create a List of track segments from the overall 
	 * result of step 1
	 * <p> 
	 * We have recorded the tracks as edges in the track graph, we now turn them
	 * into multiple explicit sets of Spots, sorted by their {@link SpotFeature#POSITION_T}.
	 */
	private void compileTrackSegments() {

		trackSegments = new ArrayList<SortedSet<Spot>>();
		Collection<Spot> spotPool = new ArrayList<Spot>();
		for(int frame : spots.keySet()) {
			spotPool.addAll(spots.get(frame)); // frame info lost
		}
		Spot source, current;
		DepthFirstIterator<Spot, DefaultWeightedEdge> graphIterator;
		SortedSet<Spot> trackSegment = null;

		while (!spotPool.isEmpty()) {
			source = spotPool.iterator().next();
			graphIterator = new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, source);
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
					weight = segmentCosts[i][j];
					DefaultWeightedEdge edge = graph.addEdge(end, start);
					graph.setEdgeWeight(edge, weight);

					if(DEBUG) 
						System.out.println("Gap closing from segment "+i+" to segment "+j+".");

				} else if (j < (numTrackSegments + numMergingMiddlePoints)) {

					// Case 2: Merging
					SortedSet<Spot> segmentEnd = trackSegments.get(i);
					Spot end =  segmentEnd.last();
					Spot middle = mergingMiddlePoints.get(j - numTrackSegments);
					weight = segmentCosts[i][j];
					DefaultWeightedEdge edge = graph.addEdge(end, middle);
					graph.setEdgeWeight(edge, weight);

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
					weight = segmentCosts[i][j];
					DefaultWeightedEdge edge = graph.addEdge(start, target);
					graph.setEdgeWeight(edge, weight);

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




	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}

