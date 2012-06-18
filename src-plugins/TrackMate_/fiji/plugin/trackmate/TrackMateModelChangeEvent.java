package fiji.plugin.trackmate;

import java.util.EventObject;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;


public class TrackMateModelChangeEvent extends EventObject {

	private static final long serialVersionUID = -355025496111237715L;
	/** Indicate that a spot was added to the model. */
	public static final int 	FLAG_SPOT_ADDED = 0;
	/** Indicate that a spot was removed from the model. */
	public static final int 	FLAG_SPOT_REMOVED = 1;
	/** Indicate a modification of the features of a spot. */
	public static final int 	FLAG_SPOT_MODIFIED = 2;
	/** Indicate that a spot has changed of frame. */
	public static final int 	FLAG_SPOT_FRAME_CHANGED = 3;
	/** Indicate that an edge was added to the model. */
	public static final int 	FLAG_EDGE_ADDED = 4;
	/** Indicate that an edge was removed from the model. */
	public static final int 	FLAG_EDGE_REMOVED = 5;

	/** 
	 * Event type indicating that the spots of the model were computed, and 
	 * are now accessible through {@link TrackMateModel#getSpots()}. 
	 */
	public static final int 	SPOTS_COMPUTED = 4;
	/** 
	 * Event type indicating that the spots of the model were filtered.
	 * Result of filtering is now accessible through {@link TrackMateModel#getFilteredSpots()}. 
	 */
	public static final int 	SPOTS_FILTERED = 5;
	/** 
	 * Event type indicating that the tracks of the model were computed, and 
	 * are now accessible through {@link TrackMateModel#getTrackGraph()}. 
	 */
	public static final int 	TRACKS_COMPUTED = 6;
	/** 
	 * Event type indicating that the tracks of the model had their 
	 * visibility changed.
	 */
	public static final int 	TRACKS_VISIBILITY_CHANGED = 7;
	/** 
	 * Event type indicating that model was modified,
	 * by adding, removing or changing the feature of some spots, and/or
	 * adding or removing edges in the tracks.
	 * Content of the modification can be accessed by {@link #getSpots()},
	 * {@link #getSpotFlag()}, {@link #getFromFrame()} and {@link #getToFrame()}, 
	 * and for the tracks: {@link #getEdges()} and {@link #getEdgeFlags()}.
	 */
	public static final int 	MODEL_MODIFIED = 8;

	private List<Spot> spots;
	private List<DefaultWeightedEdge> edges;
	private List<Integer> fromFrame;
	private List<Integer> toFrame;
	private List<Integer> spotFlags;
	private List<Integer> edgeFlags;
	private int eventID;

	/**
	 * Create a new event, reflecting a change in a {@link TrackMateModel}.
	 * 
	 * @param source  the object source of this event
	 */
	public TrackMateModelChangeEvent(Object source, int eventID) {
		super(source);
		this.eventID = eventID;
	}

	public int getEventID() {
		return this.eventID;
	}

	public void setSpots(List<Spot> spots) { 
		this.spots = spots;
	}

	public void setEdges(List<DefaultWeightedEdge> edges) { 
		this.edges = edges;
	}

	/** 
	 * @param edgeFlags  a list of integers stating what happened to the edge of the
	 * same index in the {@link #getEdges()} list. See {@link #FLAG_EDGE_ADDED}, {@link #FLAG_EDGE_REMOVED}.
	 */
	public void setEdgeFlags(List<Integer> edgeFlags) {
		this.edgeFlags = edgeFlags;
	}

	/** 
	 * @param spotFlags  a list of integers stating what happened to the spot of the
	 * same index in the {@link #getSpots()} list. See {@link #FLAG_SPOT_ADDED}, {@link #FLAG_SPOT_REMOVED},
	 * {@link #FLAG_SPOT_MODIFIED} and {@link #FLAG_SPOT_FRAME_CHANGED}.
	 */
	public void setSpotFlags(List<Integer> spotFlags) {
		this.spotFlags = spotFlags;
	}

	/**
	 * @param fromFrame  a list of integers specifying the frame the corresponding spot 
	 * belonged to before this event. If the corresponding flag is {@link #FLAG_SPOT_ADDED} or
	 * {@link #FLAG_SPOT_MODIFIED}, then this integer is <code>null</code>.
	 */
	public void setFromFrame(List<Integer> fromFrame) {
		this.fromFrame = fromFrame;
	}

	/**
	 * @param toFrame  a list of integers specifying the destination frame of the 
	 * corresponding spot. If the corresponding flag is {@link #FLAG_SPOT_REMOVED} or
	 * {@link #FLAG_SPOT_MODIFIED}, then this integer is <code>null</code>.
	 */
	public void setToFrame(List<Integer> toFrame) {
		this.toFrame = toFrame;
	}	

	public List<Spot> getSpots() {
		return spots;
	}

	public List<DefaultWeightedEdge> getEdges() {
		return edges;
	}

	public List<Integer> getSpotFlag() {
		return spotFlags;
	}

	public List<Integer> getEdgeFlags() {
		return edgeFlags;
	}

	public Integer getSpotFlag(Spot spot) {
		int index =  spots.indexOf(spot);
		if (-1 == index)
			return null;
		return spotFlags.get(index);
	}

	public List<Integer> getToFrame() {
		return toFrame;
	}

	public Integer getToFrame(Spot spot) {
		int index =  spots.indexOf(spot);
		if (-1 == index)
			return null;
		return toFrame.get(index);
	}

	public List<Integer> getFromFrame() {
		return fromFrame;
	}

	public Integer getFromFrame(Spot spot) {
		int index =  spots.indexOf(spot);
		if (-1 == index)
			return null;
		return fromFrame.get(index);
	}

	public void setSource(Object source) {
		this.source = source;
	}
	
	@Override
	public String toString() {
		String str = "[TrackModelChangeEvent]:\n";
		str += " - source: "+source+"\n";
		str += " - event type: ";
		switch (eventID) {
		case SPOTS_COMPUTED:
			str += "Spots computed\n";
			break;
		case SPOTS_FILTERED:
			str += "Spots filtered\n";
			break;
		case TRACKS_COMPUTED:
			str += "Tracks computed\n";
			break;
		case TRACKS_VISIBILITY_CHANGED:
			str += "Track visibility changed\n";
			break;
		case MODEL_MODIFIED:
			str += "Model modified, with:\n";
			str += "\t- spots modified: "+ (spots != null ? spots.size() : 0) +"\n"; 
			str += "\t- edges modified: "+ (edges != null ? edges.size() : 0) +"\n"; 
		}
		return str;
	}

}
