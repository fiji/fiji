package fiji.plugin.trackmate.tracking;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;

public abstract class AbstractSpotTracker implements SpotTracker {

	/** The settings to use for this tracker. */
	protected TrackerSettings settings = null;
	/** Logger used to echo progress on tracking. */
	protected Logger logger = Logger.DEFAULT_LOGGER;
	/** Store the whole track connections as a graph. */
	protected SimpleWeightedGraph<Spot, DefaultEdge> trackGraph = new SimpleWeightedGraph<Spot, DefaultEdge>(DefaultEdge.class);
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
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected TreeMap<Integer, List<Spot>> spots;

	

	/*
	 * CONSTRUCTOR
	 */
	
	protected AbstractSpotTracker(TrackerSettings settings) {
		this.settings = settings;
	}
	
	
	/*
	 * METHODS
	 */
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}



	@Override
	public SimpleWeightedGraph<Spot,DefaultEdge> getTrackGraph() {
		return trackGraph;
	}


}
