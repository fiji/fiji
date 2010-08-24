package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import fiji.plugin.nperry.Spot;

/**
 * <p>This class takes a list of {@link Spots} from time point <code>t</code>, and "links" each Spot to the nearest
 * <code>nLinks</code> Spots in time point <code>t+1</code>. The measure of distance used is Euclidean distance.</p>
 * 
 * <p>The user has the option of specifying a maximum search distance, such that a Spot in time <code>t</code> can only be
 * linked to Spots in <code>t+1</code> that are within this distance.</p>
 * 
 * <p>In the event that the user specifies a maximum search distance, and there are more than <code>nLinks</code> Spots
 * in <code>t+1</code> within this distance for a Spot in <code>t</code>, two rules are used to select which <code>nLink</code>
 * Spots in <code>t+1</code> are used:
 * 
 * <ol>
 * <li>Spots in <code>t+1</code> that have are not linked to an object in <code>t</code> are selected.</li>
 * <li>If there is still contention, the closest Spots are chosen.</li>
 * </ol>
 * 
 * @author nperry
 *
 */
public class NearestNeighborLinker {
	/*
	 * FIELDS
	 */
	
	/** The Spots belonging to time point <code>t</code>. */
	private ArrayList<Spot> t0;
	/** The Spots belonging to time point <code>t+1</code>. */
	private ArrayList<Spot> t1;
	/** The maximum number of Spots in time <code>t+1</code> Spots in time <code>t</code> can be linked to */
	private int nLinks;
	/** The maximum distance away from the center of a Spot in time <code>t</code> that a Spot in time <code>t+1</code>
	 * can be in order to be linked (centroid to centroid Euclidean distance, in pixels). If the user does not specify
	 * a maximum distance, this variable is set to NaN to indicate there is no maximum distance.*/
	private float maxDist;
	
	private ArrayList< ArrayList<Spot> > links;
	
	/*
	 * CONSTRUCTORS
	 */

	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, int nLinks, float maxDist)
	{
		this.t0 = t0;
		this.t1 = t1;
		this.nLinks = nLinks;
		this.maxDist = maxDist;
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, int nLinks)
	{
		this(t0, t1, nLinks, Float.NaN);
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public int getNLinks() {
		return this.nLinks;
	}
	
	public float getMaxDist() {
		return this.maxDist;
	}
}

