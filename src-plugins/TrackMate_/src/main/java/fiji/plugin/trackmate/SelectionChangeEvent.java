package fiji.plugin.trackmate;

import java.util.EventObject;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;


/**
 * An event that characterizes a change in the current selection. 
 * {@link Spot} selection and {@link DefaultWeightedEdge} selection are dealt with separately, 
 * to keep the use of this class general.
 */
public class SelectionChangeEvent extends EventObject {

	private static final long serialVersionUID = -8920831578922412606L;

	/** Changes in {@link DefaultWeightedEdge} selection this event represents. */
	private Map<DefaultWeightedEdge, Boolean> edges;

	/** Changes in {@link Spot} selection this event represents. */
	protected Map<Spot, Boolean> spots;

	/*
	 * CONSTRUCTORS 
	 */
	
	/**
	 * Represents a change in the selection of a displayed TM model.
	 * <p>
	 * Two maps are given. The first one represent changes in the spot selection. The {@link Boolean} 
	 * mapped to a {@link Spot} key specifies if the spot was added to the selection (<code>true</code>)
	 * or removed from it (<code>false</code>). The same goes for the {@link DefaultWeightedEdge} map.
	 * <code>null</code>s are accepted for the two maps, to specify that no changes happened for the 
	 * corresponding type.
	 */
	public SelectionChangeEvent(Object source, Map<Spot, Boolean> spots, Map<DefaultWeightedEdge, Boolean> edges) {
		super(source);
		this.spots = spots;
		this.edges = edges;
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Returns the spots that have been added or removed from the selection.
	 * The {@link Boolean} 
	 * mapped to a {@link Spot} key specifies if the spot was added to the selection (<code>true</code>)
	 * or removed from it (<code>false</code>).
	 * @return added or removed spots, can be <code>null</code> if no changes on spot selection happened.
	 */
	public Map<Spot, Boolean> getSpots() {
		return spots;
	}
	
	/**
	 * Returns the edges that have been added or removed from the selection.
	 * The {@link Boolean} mapped to a {@link DefaultWeightedEdge} key specifies 
	 * if the edge was added to the selection (<code>true</code>)
	 * or removed from it (<code>false</code>).
	 * @return added or removed edges, can be <code>null</code> if no changes on edge selection happened.
	 */
	public Map<DefaultWeightedEdge, Boolean> getEdges() {
		return edges;
	}

	


}
