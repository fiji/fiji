package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackCollection;

public abstract class AbstractSpotTracker implements SpotTracker {

	/** The settings to use for this tracker. */
	protected TrackerSettings settings = null;
	/** Logger used to echo progress on tracking. */
	protected Logger logger = Logger.DEFAULT_LOGGER;
	/** The tracks resulting from tracking process. */
	protected TrackCollection tracks;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores the objects to track as a list of Spots per frame.  */
	protected SpotCollection spots;
	 

	/*
	 * CONSTRUCTOR
	 */
	
	protected AbstractSpotTracker(TrackerSettings settings) {
		this.settings = settings;
		this.tracks = new TrackCollection();
	}
	
	
	/*
	 * METHODS
	 */
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public TrackCollection getTracks() {
		return tracks;
	}


}
