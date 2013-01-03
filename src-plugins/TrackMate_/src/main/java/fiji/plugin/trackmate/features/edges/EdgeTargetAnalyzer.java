package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class EdgeTargetAnalyzer implements EdgeAnalyzer, MultiThreaded {

	public static final String KEY = "Edge target";
	/*
	 * FEATURE NAMES 
	 */
//	private static final String SPOT1_NAME = "SPOT1_NAME";
//	private static final String SPOT2_NAME = "SPOT1_NAME";
	public static final String SPOT1_ID = "SPOT1_ID";
	public static final String SPOT2_ID = "SPOT2_ID";
	public static final String EDGE_COST = "COST";
//	private static final String TRACK_ID = "TRACK_ID";


	public static final List<String> FEATURES = new ArrayList<String>(4);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(4);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(4);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(4);

	static {
//		FEATURES.add(SPOT1_NAME);
//		FEATURES.add(SPOT2_NAME);
		FEATURES.add(SPOT1_ID);
		FEATURES.add(SPOT2_ID);
		FEATURES.add(EDGE_COST);
//		FEATURES.add(TRACK_ID);

//		FEATURE_NAMES.put(SPOT1_NAME, "Source spot name");
//		FEATURE_NAMES.put(SPOT2_NAME, "Target spot name");
		FEATURE_NAMES.put(SPOT1_ID, "Source spot ID");
		FEATURE_NAMES.put(SPOT2_ID, "Target spot ID");
		FEATURE_NAMES.put(EDGE_COST, "Link cost");
//		FEATURE_NAMES.put(TRACK_ID, "Track ID");

//		FEATURE_SHORT_NAMES.put(SPOT1_NAME, "Source");
//		FEATURE_SHORT_NAMES.put(SPOT2_NAME, "Target");
		FEATURE_SHORT_NAMES.put(SPOT1_ID, "Source ID");
		FEATURE_SHORT_NAMES.put(SPOT2_ID, "Target ID");
		FEATURE_SHORT_NAMES.put(EDGE_COST, "Cost");
//		FEATURE_SHORT_NAMES.put(TRACK_ID, "Track");

//		FEATURE_DIMENSIONS.put(SPOT1_NAME, Dimension.STRING);
//		FEATURE_DIMENSIONS.put(SPOT2_NAME, Dimension.STRING);
		FEATURE_DIMENSIONS.put(SPOT1_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(SPOT2_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(EDGE_COST, Dimension.NONE);
//		FEATURE_DIMENSIONS.put(TRACK_ID, Dimension.NONE);
	}

	private int numThreads;
	private long processingTime;
	private final FeatureModel featureModel;
	private final TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTargetAnalyzer(final TrackMateModel model) {
		this.model = model;
		this.featureModel = model.getFeatureModel();
		setNumThreads();
	}
	
	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public void process(final Collection<DefaultWeightedEdge> edges) {

		final ArrayBlockingQueue<DefaultWeightedEdge> queue = new ArrayBlockingQueue<DefaultWeightedEdge>(edges.size(), false, edges);

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("EdgeTargetAnalyzer thread " + i) {
				@Override
				public void run() {
					DefaultWeightedEdge edge;
					while ((edge = queue.poll()) != null) {

						// Edge weight
						featureModel.putEdgeFeature(edge, EDGE_COST, model.getTrackModel().getEdgeWeight(edge));
						// Source & target name & ID
						Spot source = model.getTrackModel().getEdgeSource(edge);
//						featureModel.putEdgeFeature(edge, SPOT1_NAME, source.getName());
						featureModel.putEdgeFeature(edge, SPOT1_ID, Double.valueOf(source.ID()));
						Spot target = model.getTrackModel().getEdgeTarget(edge);
//						featureModel.putEdgeFeature(edge, SPOT2_NAME, target.getName());
						featureModel.putEdgeFeature(edge, SPOT2_ID, Double.valueOf(target.ID()));
						// Track it belong to TOO COSTY IN TIME
//						final Map<Integer,Set<DefaultWeightedEdge>> tracks = model.getTrackEdges();
//						int trackID = -1;
//						for (int id : tracks.keySet()) {
//							if (tracks.get(id).contains(edge)) {
//								trackID = id;
//								break;
//							}
//						}
//						featureModel.putEdgeFeature(edge, TRACK_ID, Double.valueOf(trackID));

					}

				}
			};
		}

		long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin(threads);
		long end = System.currentTimeMillis();
		processingTime = end - start;
	}


	@Override
	public String toString() {
		return KEY;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();  
	}

	@Override
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;

	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	};
}
