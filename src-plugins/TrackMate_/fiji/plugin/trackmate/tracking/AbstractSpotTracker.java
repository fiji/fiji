package fiji.plugin.trackmate.tracking;

import java.util.List;
import java.util.SortedSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

/**
 * Abstract class that offer small facilities for the tracker that aim at
 * implementing {@link SpotTracker}. 
 * <p>
 * This class only advantage is that it deals with fields and setters/getters.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Mar 31, 2011
 */
public abstract class AbstractSpotTracker implements SpotTracker {

	/** The settings to use for this tracker. */
	protected TrackerSettings settings = null;
	/** Logger used to echo progress on tracking. */
	protected Logger logger = Logger.DEFAULT_LOGGER;
	/** Store the whole track connections as a graph. */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
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
	protected SpotCollection spots;

	
	/*
	 * METHODS
	 */
	
	@Override
	public void setSettings(TrackerSettings settings) {
		this.settings = settings;
	}
	
	@Override
	public void setSpots(SpotCollection spots) {
		this.spots = spots;	
	}
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public SimpleWeightedGraph<Spot,DefaultWeightedEdge> getTrackGraph() {
		return trackGraph;
	}


}
