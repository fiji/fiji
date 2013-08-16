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
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

public class EdgeTargetAnalyzer implements EdgeAnalyzer, MultiThreaded {

	public static final String KEY = "Edge target";
	/*
	 * FEATURE NAMES
	 */
	public static final String SPOT_SOURCE_ID = "SPOT_SOURCE_ID";
	public static final String SPOT_TARGET_ID = "SPOT_TARGET_ID";
	public static final String EDGE_COST = "LINK_COST";

	public static final List<String> FEATURES = new ArrayList<String>(4);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(4);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(4);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(4);

	static {
		FEATURES.add(SPOT_SOURCE_ID);
		FEATURES.add(SPOT_TARGET_ID);
		FEATURES.add(EDGE_COST);

		FEATURE_NAMES.put(SPOT_SOURCE_ID, "Source spot ID");
		FEATURE_NAMES.put(SPOT_TARGET_ID, "Target spot ID");
		FEATURE_NAMES.put(EDGE_COST, "Link cost");

		FEATURE_SHORT_NAMES.put(SPOT_SOURCE_ID, "Source ID");
		FEATURE_SHORT_NAMES.put(SPOT_TARGET_ID, "Target ID");
		FEATURE_SHORT_NAMES.put(EDGE_COST, "Cost");

		FEATURE_DIMENSIONS.put(SPOT_SOURCE_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(SPOT_TARGET_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(EDGE_COST, Dimension.NONE);
	}

	private int numThreads;
	private long processingTime;
	private final FeatureModel featureModel;
	private final Model model;

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTargetAnalyzer(final Model model) {
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

		if (edges.isEmpty()) {
			return;
		}

		final ArrayBlockingQueue<DefaultWeightedEdge> queue = new ArrayBlockingQueue<DefaultWeightedEdge>(edges.size(), false, edges);

		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("EdgeTargetAnalyzer thread " + i) {
				@Override
				public void run() {
					DefaultWeightedEdge edge;
					while ((edge = queue.poll()) != null) {
						// Edge weight
						featureModel.putEdgeFeature(edge, EDGE_COST, model.getTrackModel().getEdgeWeight(edge));
						// Source & target name & ID
						final Spot source = model.getTrackModel().getEdgeSource(edge);
						featureModel.putEdgeFeature(edge, SPOT_SOURCE_ID, Double.valueOf(source.ID()));
						final Spot target = model.getTrackModel().getEdgeTarget(edge);
						featureModel.putEdgeFeature(edge, SPOT_TARGET_ID, Double.valueOf(target.ID()));
					}

				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin(threads);
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}


	@Override
	public String getKey() {
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
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;

	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public List<String> getFeatures() {
		return FEATURES;
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	};
}
