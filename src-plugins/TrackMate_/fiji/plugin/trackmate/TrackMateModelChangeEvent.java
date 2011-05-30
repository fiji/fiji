package fiji.plugin.trackmate;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;


public class TrackMateModelChangeEvent extends EventObject {
	
	private static final long serialVersionUID = -355025496111237715L;
	/** Indicate a spot added to the model. */
	public static final int 	SPOT_ADDED = 0;
	/** Indicate a spot removed from the model. */
	public static final int 	SPOT_REMOVED = 1;
	/** Indicate a modification of the features of a spot. */
	public static final int 	SPOT_MODIFIED = 2;
	/** Indicate that a spot has changed of frame. */
	public static final int 	SPOT_FRAME_CHANGED = 3;

	protected List<Spot> spots;
	protected List<Integer> fromFrame;
	protected List<Integer> toFrame;
	protected Map<DefaultWeightedEdge, Boolean> edgesAdded;
	private List<Integer> spotFlags;

	/**
	 * Create a new event, reflecting a change in a {@link TrackMateModel}.
	 * 
	 * @param source  the object source of this event
	 * @param spots  the list of spots concerned by this event. Can be <code>null</code> to
	 * specify that no spots are affected by this event. In this case, <code>spotFlags</code>,
	 * <code>fromFrame</code> and <code>toFrame</code>should be <code>null</code> as well.
	 * @param spotFlags  a list of integers stating what happened to the spot of the
	 * same index in the previous list. See {@link #SPOT_ADDED}, {@link #SPOT_REMOVED},
	 * {@link #SPOT_MODIFIED} and {@link #SPOT_FRAME_CHANGED}.
	 * @param fromFrame  a list of integers specifying the frame the corresponding spot 
	 * belonged to before this event. If the corresponding flag is {@link #SPOT_ADDED} or
	 * {@link #SPOT_MODIFIED}, then this integer is <code>null</code>.
	 * @param toFrame  a list of integers specifying the destination frame of the 
	 * corresponding spot. If the corresponding flag is {@link #SPOT_REMOVED} or
	 * {@link #SPOT_MODIFIED}, then this integer is <code>null</code>.
	 */
	public TrackMateModelChangeEvent(Object source, 
			List<Spot> spots, 
			List<Integer> spotFlags,
			List<Integer> fromFrame,
			List<Integer> toFrame) {
		super(source);
		this.spots = spots;
		this.spotFlags = spotFlags;
		this.fromFrame = fromFrame;
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
