package fiji.plugin.trackmate;

import java.util.EventObject;
import java.util.List;


public class TrackMateModelChangeEvent extends EventObject {
	
	private static final long serialVersionUID = -355025496111237715L;
	/** Indicate a spot added to the model. */
	public static final int 	FLAG_SPOT_ADDED = 0;
	/** Indicate a spot removed from the model. */
	public static final int 	FLAG_SPOT_REMOVED = 1;
	/** Indicate a modification of the features of a spot. */
	public static final int 	FLAG_SPOT_MODIFIED = 2;
	/** Indicate that a spot has changed of frame. */
	public static final int 	FLAG_SPOT_FRAME_CHANGED = 3;
	
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
	 * Event type indicating that the spots of the model were modified,
	 * by adding, removing or changing the feature of some of them.
	 * Content of the modification can be accessed by {@link #getSpots()},
	 * {@link #getSpotFlag()}, {@link #getFromFrame()} and {@link #getToFrame()}.
	 */
	public static final int 	SPOTS_MODIFIED = 7;


	
	private List<Spot> spots;
	private List<Integer> fromFrame;
	private List<Integer> toFrame;
	private List<Integer> spotFlags;
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

	 /** 
	  * @param spotFlags  a list of integers stating what happened to the spot of the
	 * same index in the {@link #getSpots()} list. See {@link #FLAG_SPOT_ADDED}, {@link #FLAG_SPOT_REMOVED},
	 * {@link #FLAG_SPOT_MODIFIED} and {@link #FLAG_SPOT_FRAME_CHANGED}.
	 */
	public void setSpotFlag(List<Integer> spotFlags) {
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
	
	public List<Integer> getSpotFlag() {
		return spotFlags;
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
	
}
