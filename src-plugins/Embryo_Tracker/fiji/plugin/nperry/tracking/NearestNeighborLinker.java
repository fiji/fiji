package fiji.plugin.nperry.tracking;

import java.util.ArrayList;
import java.util.HashMap;

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
	/** The maximum number of Spots in time <code>t+1</code> Spots in time <code>t</code> can be linked to. If not specified
	 * 0 is used as a flag. */
	private int nLinks;
	/** The maximum distance away from the center of a Spot in time <code>t</code> that a Spot in time <code>t+1</code>
	 * can be in order to be linked (centroid to centroid Euclidean distance, in pixels). If the user does not specify
	 * a maximum distance, this variable is set to positive infinity to indicate there is no maximum distance. */
	private float maxDist;
	/** Each index represents a Spot in t0 (same index), and holds a list of the Spots in <code>t+1</code> that are linked 
	 * to it. */
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
		this(t0, t1, nLinks, Float.POSITIVE_INFINITY);
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	public NearestNeighborLinker(ArrayList<Spot> t0, ArrayList<Spot> t1, float maxDist)
	{
		this(t0, t1, 0, maxDist);
		this.links = new ArrayList< ArrayList<Spot> >();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * 
	 * @return The max number of links.
	 */
	public int getNLinks()
	{
		return this.nLinks;
	}
	
	/**
	 * 
	 * @return The maximum distance between potentially link-able Spots.
	 */
	public float getMaxDist()
	{
		return this.maxDist;
	}
	
	/**
	 * 
	 * @return The computed list of links for each object in time t.
	 */
	public ArrayList< ArrayList<Spot> > getLinks()
	{
		return this.links;
	}

	public void process()
	{
		final float maxDistSq = maxDist * maxDist;								// Prevents us from using the costly Math.sqrt() function for Euclidean distance checks
		final HashMap<Spot, Integer> numLinks = new HashMap<Spot, Integer>();	// A HashMap to keep track of how many times Spots in t+1 have been linked to Spots in t. 
		for (int i = 0; i < t1.size(); i++) {
			numLinks.put(t1.get(i), 0);
		}
		final ArrayList< HashMap<Spot, Float> > distances = new ArrayList< HashMap<Spot, Float> >();  // For the points we add as links in part (1) below, store the distances we calculate for later pruning
		
		// 1 - For each Spot in t, find *all* potential Spots in t+1 within maxDist to link to (could be > nLinks).
		for (int i = 0; i < t0.size(); i++) {
			float[] curr = t0.get(i).getCoordinates();
			HashMap<Spot, Float> distance = new HashMap<Spot, Float>();	// store the relevant distances we calculate for this Spot to Spots in t+1
			for (int j = 0; j < t1.size(); j++) {
				float[] potential = t1.get(j).getCoordinates();
				float d = getEucDistSq(curr, potential);
				if (d <= maxDistSq) {
					Spot s = t1.get(j);
					links.get(i).add(s);	  		// Add this Spot j in t+1 as a link to our Spot i in t
					int count = numLinks.get(s);	// Increment the link count for this Spot in t+1
					count++;
					numLinks.put(s, count);
					distance.put(s, d);
				}
			}
		}
		
		// 2 - Trim down the number of Spots linked to in t+1 from each Spot in t to <= nLinks, if nLinks specified
		if (nLinks > 0) {
			for (int i = 0; i < links.size(); i++) {
				if (links.get(i).size() > nLinks) {	// If there are more than nLinks for this Spot...
					
					// Create a duplicate list, and clear the real one
					ArrayList<Spot> curr = links.get(i);
					ArrayList<Spot> dup = new ArrayList<Spot>(curr);
					curr.clear();
					
					// Add back only the Spots that are not linked to anything else
					for (int j = 0; i < dup.size(); j++) {
						Spot s = dup.get(j);
						if (numLinks.get(s) == 1) {	// If == 1, then this Spot in t+1 is only linked to the current Spot in t.
							curr.add(s);
							dup.remove(s); // Keep track of the Spots that aren't linked anymore
						}
					}
					
					if (curr.size() == nLinks) continue;	// We might have the right number now.
					
					HashMap<Spot, Float> distanceList = distances.get(i);
//					
//					//TODO make a separate method
//					// If we still have too many, remove the farthest away until we have the right number.
//					if (curr.size() > nLinks) {
//						while (curr.size() > nLinks) {
//							Spot min = null;
//							float minD = 0;
//							for (int j = 0; j < curr.size(); j++) {
//								Spot next = curr.get(j);
//								if (min == null) {
//									min = next;
//									minD = distanceList.get(min);
//								} else {
//									float nextD = distanceList.get(next);
//									if (nextD > minD) {
//										min = next;
//										minD = nextD;
//									}
//								}
//							}
//						}
//					}
//					
//					//TODO make a separate method
//					// We don't have enough, so add back the closest.
//					else {
//						
//						while (curr.size() < nLinks) {
//							Spot max = null;
//							float maxD = 0;
//							for (int j = 0; j < dup.size(); j++) {
//								Spot next = dup.get(j);
//								if (max == null) {
//									max = next;
//									maxD = distanceList.get(max);
//								} else {
//									float nextD = distanceList.get(next);
//									if (nextD > maxD) {
//										max = next;
//										maxD = nextD;
//									}
//								}
//							}
//						}
//					}
				}
			}
		}
	}
	
	/**
	 * This method returns the (Euclidean distance)^2 between two points.
	 * 
	 * @param a The coordinates of the first point.
	 * @param b The coordinates of the second point.
	 * @return The Euclidean distance squared.
	 */
	private static float getEucDistSq(float[] a, float[] b) {
		float total = 0;
		for (int i = 0; i < a.length; i++) {
			total += ((a[i] - b[i]) * (a[i] - b[i]));
		}
		return total;
	}
}

