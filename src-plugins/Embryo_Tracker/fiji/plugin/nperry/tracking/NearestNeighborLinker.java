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
	 * can be in order to be linked (centroid to centroid Euclidean distance, in physical units). If the user does not specify
	 * a maximum distance, this variable is set to positive infinity to indicate there is no maximum distance. */
	private float maxDist;
	/** Each index represents a Spot in t0 (same index), and holds a list of the Spots in <code>t+1</code> that are linked 
	 * to it. */
	private ArrayList< ArrayList<Spot> > links;
	/** This String will hold any error message incurred during the use of this class. */
	private String errorMessage = "";
	/** Used to store whether checkInput() was run or not. */
	private boolean inputChecked = false;
	
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
	public ArrayList< ArrayList<Spot> > getResult()
	{
		return this.links;
	}
	
	/**
	 * Returns any error messages that develop during the use of this class.
	 * @return The error message.
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
	/**
	 * Call this method before calling process, in order to guarantee that all of the required input is correct.
	 */
	public boolean checkInput()
	{
		// Check that t0 isn't empty
		if (t0.isEmpty()) {
			errorMessage = "The ArrayList for time point t (t0) is empty.";
			return false;
		}
		
		// Check that t1 isn't empty
		if (t1.isEmpty()) {
			errorMessage = "The ArrayList for time point t+1 (t1) is empty.";
			return false;
		}
		
		// Check that maxDist >= 0
		if (maxDist < 0) {
			errorMessage = "The maximum search distance supplied is negative.";
			return false;
		}
		
		// Check that nLinks >= 0 (0 is internal flag signifying no limit)
		if (nLinks < 0) {
			errorMessage = "The number of links to find is negative.";
			return false;
		}
		
		// If we got here, we are fine.
		inputChecked = true;
		return true;
	}

	public boolean process()
	{
		// Ensure that checkInput() was run before executing
		if (!inputChecked) {
			errorMessage = "checkInput() was never run.";
			return false;
		}
		
		// Initialize local vars
		final float maxDistSq = maxDist * maxDist;								// Prevents us from using the costly Math.sqrt() function for Euclidean distance checks
		final HashMap<Spot, Integer> numLinks = new HashMap<Spot, Integer>();	// A HashMap to keep track of how many times Spots in t+1 have been linked to Spots in t. 
		final ArrayList< HashMap<Spot, Float> > distances = new ArrayList< HashMap<Spot, Float> >();  // For the points we add as links in part (1) below, store the distances we calculate for later pruning
		float[] currCoords = new float[t0.get(1).getCoordinates().length];			// We are guaranteed the existence of at least one Spot in t0, because of checkInput.
		float[] potentialCoords = new float[t0.get(1).getCoordinates().length];
		float dist;
		
		// Add all Spots from t1 into the numLinks hashmap, with an initial count of 0 (they are all unlinked at this point).
		for (int i = 0; i < t1.size(); i++) {
			numLinks.put(t1.get(i), 0);
		}
		
		// For each Spot in t, find *all* potential Spots in t+1 within maxDist to link to (could be > nLinks).
		for (int i = 0; i < t0.size(); i++) {	// For all Spots in t
			currCoords = t0.get(i).getCoordinates();
			HashMap<Spot, Float> distMap = new HashMap<Spot, Float>();	// store the relevant distances we calculate for this Spot to Spots in t+1
			for (int j = 0; j < t1.size(); j++) {	// For all Spots in t+1
				potentialCoords = t1.get(j).getCoordinates();
				dist = getEucDistSq(currCoords, potentialCoords);
				if (dist <= maxDistSq) {
					links.get(i).add(t1.get(j));	// Add this Spot j in t+1 as a link to our Spot i in t
					incrementCount(numLinks, t1.get(j));
					distMap.put(t1.get(j), dist);
				}
			}
			distances.add(distMap);	// Store the distances for each Spot in t+1 linked to the current Spot in t.
		}
		
		// Trim down the number of Spots in t+1 linked to Spot in t to <= nLinks, if nLinks specified
		if (nLinks > 0) {
			for (int i = 0; i < links.size(); i++) {	// For all Spots in t...
				if (links.get(i).size() > nLinks) {		// If there are more than nLinks for this Spot...
					
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
					
					// We might have the right number now.
					if (curr.size() == nLinks) continue;	
					
					// If not, check to see if we are over or under quota.
					if (curr.size() > nLinks) {	// Over, so remove those that are farthest away
						//TODO
					}
					
					else {						// Under, so add back the next closest
						//TODO
					}
				}
			}
		}
		
		// Process() finished
		return true;
	}
	
	/**
	 * Helper method which increments the count of links made to this Spot.
	 * @param numLinks The HashMap storing the number of links made to each Spot.
	 * @param spot	The Spot for which the number of links should be incremented.
	 */
	private static void incrementCount(HashMap<Spot, Integer> numLinks, Spot spot) 
	{
		int count = numLinks.get(spot);
		count++;
		numLinks.put(spot, count);
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

