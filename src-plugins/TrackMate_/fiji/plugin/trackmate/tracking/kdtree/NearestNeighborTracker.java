package fiji.plugin.trackmate.tracking.kdtree;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.kdtree.KDTree;
import mpicbg.imglib.algorithm.kdtree.NNearestNeighborSearch;
import mpicbg.imglib.algorithm.kdtree.NearestNeighborSearch;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class NearestNeighborTracker extends MultiThreadedBenchmarkAlgorithm	implements SpotTracker {

	/*
	 * FIELDS
	 */

	private SpotCollection spots;
	private Logger logger = Logger.DEFAULT_LOGGER;
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private NearestNeighborTrackerSettings settings;

	/*
	 * CONSTRUCTOR
	 */

	public NearestNeighborTracker() {
		super();
	}


	/*
	 * PUBLIC METHODS
	 */

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		final double maxDistSquare = settings.maxLinkingDistance * settings.maxLinkingDistance;

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

						// Build KD-tree for target frame
						ArrayList<SpotNode> targetNodes = new ArrayList<SpotNode>(targetSpots.size());
						for (Spot spot : targetSpots) {
							targetNodes.add(new SpotNode(spot));
						}
						KDTree<SpotNode> kdTree = new KDTree<SpotNode>(targetNodes);
						NearestNeighborSearch<SpotNode> search = new NearestNeighborSearch<SpotNode>(kdTree);
						NNearestNeighborSearch<SpotNode> multiSearch = new NNearestNeighborSearch<SpotNode>(kdTree);

						// For each spot in the source frame, find its nearest neighbor in the target frame
						for (Spot source : sourceSpots) {

							SpotNode sourceNode = new SpotNode(source);
							SpotNode targetNode = search.findNearestNeighbor(sourceNode);
							double squareDist = targetNode.distanceTo(sourceNode);
							if (squareDist > maxDistSquare) {
								// The closest we could find is too far. We skip this source spot and do not create a link
								continue;								
							}

							boolean doNotCreateALink = false;
							if (targetNode.isAssigned) {
								// Bad luck: the closes node we have found is already taken. So we need to find the
								// closest one, not yet taken, and not farther than the max dist.
								// This is inefficient because we always recalculate the first neighbors, which we
								// had at the previous iteration. We do it like that for the moment, keeping in mind
								// that for this application, such situations are marginal.
								for (int currentNeighbor = 2; currentNeighbor < targetNodes.size(); currentNeighbor++) {

									SpotNode[] found = multiSearch.findNNearestNeighbors(sourceNode, currentNeighbor);
									targetNode = found[currentNeighbor-1];

									squareDist = targetNode.distanceTo(sourceNode);
									if (squareDist > maxDistSquare) {
										// We have found that the next closest node is already too far. Give up and stop searching.
										doNotCreateALink = true;
										break;				
									}

									if (!targetNode.isAssigned) {
										// Success! We finally found one that is both within max dist and free. We stop
										// searching and create a link
										break;
									}
									
									// Damn. This one is taken as well. So we look further.
								}
								
								// If we reached this point, that means that we exhausted all target nodes and still
								// did not find one that is both free and within max dist. So we give up, and set 
								// a flag that states not to create a link.
								doNotCreateALink = true;
							}

							if (doNotCreateALink) {
								// The closest we could find is too far. We skip this source spot and do not create a link
								continue;
							}

							// Everything is ok. This mode is free and below max dist. We create a link
							// and mark this node as assigned.
							DefaultWeightedEdge edge = graph.addEdge(source, targetNode.getSpot());
							graph.setEdgeWeight(edge, squareDist);
							targetNode.isAssigned = true;

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
		return "Nearest neighbor search";
	}
	
	@Override
	public String getInfoText() {
		return "<html>" +
				"This tracker is the most simple one, and is based on nearest neighbor <br>" +
				"search. The spots in the target frame are searched for the nearest neighbor <br> " +
				"of each spot in the source frame. If the spots found are closer than the <br>" +
				"maximal allowed distance, a link between the two is created. <br>" +
				"<p>" +
				"The nearest neighbor search relies upon the KD-tree technique implemented <br>" +
				"in imglib by ??. This ensure a very efficient tracking and <br>" +
				"makes this tracker suitable for situation where a huge number of particles <br>" +
				"are to be tracked over a very large number of frames. However, because of <br>" +
				"the naiveness of its principles, it can result in pathological tracks. <br>" +
				"It can only do frame-to-frame linking; there cannot be any track merging or <br>" +
				"splitting, and gaps will not be closed." +
				" </html>";	
	}

	@Override
	public void setModel(TrackMateModel model) {
		this.spots = model.getFilteredSpots();
		this.settings = (NearestNeighborTrackerSettings) model.getSettings().trackerSettings;
		reset();
	}

	@Override
	public void setLogger(Logger logger) {
		this.logger  = logger;

	}

	@Override
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
		return graph;
	}

	@Override
	public TrackerSettings createDefaultSettings() {
		return new NearestNeighborTrackerSettings();
	}

	public void reset() {
		graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for(Spot spot : spots) 
			graph.addVertex(spot);
	}


}
