package fiji.plugin.trackmate.tracking.kdtree;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.collection.KDTree;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;

public class NearestNeighborTracker extends MultiThreadedBenchmarkAlgorithm	implements SpotTracker {

	/*
	 * FIELDS
	 */

	public static final String TRACKER_KEY = "NEAREST_NEIGHBOR_TRACKER";
	public static final String NAME = "Nearest neighbor search";
	public static final String INFO_TEXT = "<html>" +
				"This tracker is the most simple one, and is based on nearest neighbor <br>" +
				"search. The spots in the target frame are searched for the nearest neighbor <br> " +
				"of each spot in the source frame. If the spots found are closer than the <br>" +
				"maximal allowed distance, a link between the two is created. <br>" +
				"<p>" +
				"The nearest neighbor search relies upon the KD-tree technique implemented <br>" +
				"in imglib by Johannes Schindelin and friends. This ensure a very efficient " +
				"tracking and makes this tracker suitable for situation where a huge number <br>" +
				"of particles are to be tracked over a very large number of frames. However, <br>" +
				"because of the naiveness of its principles, it can result in pathological <br>" +
				"tracks. It can only do frame-to-frame linking; there cannot be any track <br>" +
				"merging or splitting, and gaps will not be closed. Also, the end results are non-" +
				"deterministic." +
				" </html>";
	
	protected final SpotCollection spots;
	protected final Logger logger;
	protected SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph;
	protected Map<String, Object> settings;

	/*
	 * CONSTRUCTOR
	 */

	public NearestNeighborTracker(final SpotCollection spots, final Logger logger) {
		this.spots = spots;
		this.logger = logger;
	}

	public NearestNeighborTracker(final SpotCollection spots) {
		this(spots, Logger.VOID_LOGGER);
	}


	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void setSettings(Map<String, Object> settings) {
		this.settings = settings;
	}

	@Override
	public boolean checkInput() {
		StringBuilder errrorHolder = new StringBuilder();;
		boolean ok = checkInput(settings, errrorHolder);
		if (!ok) {
			errorMessage = errrorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		reset();
		
		final double maxLinkingDistance = (Double) settings.get(KEY_LINKING_MAX_DISTANCE);
		final double maxDistSquare = maxLinkingDistance  * maxLinkingDistance;

		final TreeSet<Integer> frames = new TreeSet<Integer>(spots.keySet());
		Thread[] threads = new Thread[numThreads];

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(frames.first());
		final AtomicInteger progress = new AtomicInteger(0);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot feature calculating thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < frames.last(); i = ai.getAndIncrement()) {

						// Build frame pair
						int sourceFrame = i;
						int targetFrame = frames.higher(i);
						List<Spot> sourceSpots = spots.get(sourceFrame);
						List<Spot> targetSpots = spots.get(targetFrame);
						
						List<RealPoint> targetCoords = new ArrayList<RealPoint>(targetSpots.size());
						List<FlagNode<Spot>> targetNodes = new ArrayList<FlagNode<Spot>>(targetSpots.size());
						for(Spot spot : targetSpots) {
							double[] coords = new double[3];
							TMUtils.localize(spot, coords);
							targetCoords.add(new RealPoint(coords));
							targetNodes.add(new FlagNode<Spot>(spot));
						}
						
						
						KDTree<FlagNode<Spot>> tree = new KDTree<FlagNode<Spot>>(targetNodes, targetCoords);
						NearestNeighborFlagSearchOnKDTree<Spot> search = new NearestNeighborFlagSearchOnKDTree<Spot>(tree);
						
						// For each spot in the source frame, find its nearest neighbor in the target frame
						for (Spot source : sourceSpots) {

							double[] coords = new double[3];
							TMUtils.localize(source, coords);
							RealPoint sourceCoords = new RealPoint(coords);
							search.search(sourceCoords);
							
							double squareDist = search.getSquareDistance();
							FlagNode<Spot> targetNode = search.getSampler().get();
							
							if (squareDist > maxDistSquare) {
								// The closest we could find is too far. We skip this source spot and do not create a link
								continue;								
							}

							// Everything is ok. This mode is free and below max dist. We create a link
							// and mark this node as assigned.

							targetNode.setVisited(true);
							synchronized (graph) {
								DefaultWeightedEdge edge = graph.addEdge(source, targetNode.getValue());
								graph.setEdgeWeight(edge, squareDist);
							}

						}
						logger.setProgress(progress.incrementAndGet() / (float)frames.size() );

					}
				}
			};

			
		}
		
		logger.setStatus("Tracking...");
		logger.setProgress(0);
		
		SimpleMultiThreading.startAndJoin(threads);
		
		logger.setProgress(1);
		logger.setStatus("");
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
		return graph;
	}

	public void reset() {
		graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for(Spot spot : spots) 
			graph.addVertex(spot);
	}

	@Override
	public String getKey() {
		return TRACKER_KEY;
	}
	
	public static boolean checkInput(final Map<String, Object> settings, StringBuilder errrorHolder) {
		boolean ok = checkParameter(settings, KEY_LINKING_MAX_DISTANCE, Double.class, errrorHolder);
		List<String> mandatoryKeys = new ArrayList<String>();
		mandatoryKeys.add(KEY_LINKING_MAX_DISTANCE);
		ok = ok & checkMapKeys(settings, mandatoryKeys, null, errrorHolder);
		return ok;
	}
}
