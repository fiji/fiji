package fiji.plugin.nperry.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.math.MathLib;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public interface ObjectTracker extends Algorithm {

	public void getTracks();
	
	
	
//	/*
//	 * FIELDS
//	 */
//	
//	/** Holds the extrema for each frame. Each index (outer ArrayList) represents a single frame. */
//	private final ArrayList< ArrayList<Spot> > points;
//	/** Biologically defined event - cell death. */
//	private final static int CELL_DEATH = 0;
//	/** Biologically defined event - cell match */
//	private final static int CELL_MATCH = 1;
//	/** Biologically defined event - cell division */
//	private final static int CELL_DIVISION = 2;
//	/** Biologically defined event - cell merge */
//	private final static int MERGE = 4;
//	/** Used to store whether checkInput() was run or not. */
//	private boolean inputChecked = false;
//	/** This String will hold any error message incurred during the use of this class. */
//	private String errorMessage = "";
//	/** The maximum distance away potential Spots can be in t+1 from the Spot in t (centroid to centroid, in physical units). */
//	private final static float maxDist = 10f;
//	/** The maximum number of nearest Spots in t+1 to link to the Spot in t */
//	private final static int nLinks = 3;
//	 
//	/*
//	 * CONSTRUCTORS
//	 */
//	
//	/**
//	 * Instantiate by providing the lists of points in each time point. The outer ArrayList represents each frame
//	 * in the movie, while the inner ArrayList contains the points belonging to that time point.
//	 */
//	public ObjectTracker(ArrayList< ArrayList<Spot> > points)
//	{
//		this.points = points;
//	}
//
//	/*
//	 * PUBLIC METHODS
//	 */
//	
//	/**
//	 * Returns any error messages that develop during the use of this class.
//	 * @return The error message.
//	 */
//	public String getErrorMessage()
//	{
//		return this.errorMessage;
//	}
//	
//	
//	/**
//	 * Call this method before calling process, in order to guarantee that all of the required input is correct.
//	 * @return true if no errors, false otherwise.
//	 */
//	public boolean checkInput()
//	{
//		if (points.isEmpty()) {
//			errorMessage = "There are no points.";
//			return false;
//		}
//		
//		inputChecked = true;
//		return true;
//	}
//	
//	
//	public void getResults()
//	{
//		
//	}
//	
//
//	public boolean process()
//	{
//		//Make sure checkInput() has been called first.
//		if (!inputChecked){
//			errorMessage = "checkInput() was not run before calling process()!";
//			return false;
//		}
//		
//		// For each time frame pair t and t+1, link Spots
//		for (int i = 0; i < points.size() - 1; i++) {
//			 NearestNeighborLinker linker = new NearestNeighborLinker(points.get(i), points.get(i+1), nLinks, maxDist);
//			 if (!linker.checkInput() || !linker.process()) {
//				 System.out.println("NearestNeighborLinker failure: " + linker.getErrorMessage());
//			 }
//			 HashMap< Spot, ArrayList<Spot> > links = linker.getResult();
//			 
//			 // <debug>
//			 System.out.println("Frame " + (i + 1));
//			 System.out.println("------------------");
//			 Set<Spot> keys = links.keySet();
//			 Iterator<Spot> itr = keys.iterator();
//			 while(itr.hasNext()) {
//				 Spot curr = itr.next();
//				 ArrayList<Spot> linked = links.get(curr);
//				 System.out.println("Spot " + MathLib.printCoordinates(curr.getCoordinates()) + " is linked to: ");
//				 for (int k = 0; k < linked.size(); k++) {
//					 System.out.println(MathLib.printCoordinates(linked.get(k).getCoordinates()));
//				 }
//			 }
//			 // </debug>
//			 
//			 // Use scoring criterion to move from potential links to a finalized link configuration
//			 assignFinalLinkConfiguration(links);
//		}
//		
//		// Process() finished
//		return true;
//	}
//	
//	
//	/*
//	 * PRIVATE METHODS
//	 */
//	
//	
//	/**
//	 * Use a scoring system to decide on the finalized link configurations for each Spot in t to Spots in t+1.
//	 * 
//	 * The input is an list of Spots in t+1 for each Spot in t, where each Spot in t+1 has been identified as the potential
//	 * child Spot in t+1 of the Spot in t. To determine the optimal configuration of assignments, a scoring system will
//	 * be used to identify the optimal assignment of the following biologically defined links to each Spot in t:
//	 * 
//	 * <ul>
//	 * <li>One-to-one: Spot in t assigned to a single Spot in t+1</li>
//	 * <li>Division: Spot in t assigned to a two Spots in t+1</li>
//	 * <li>Cell death: Spot in t assigned to no Spot in t+1</li>
//	 * <li>Merge: Two Spots in t assigned to a single Spot in t+1</li>
//	 * </ul>
//	 * 
//	 * A DFS, tree-approach is used to search the entire search space of this problem.
//	 * 
//	 * @param links A list containing the potential child Spots in t+1 for each Spot in t.
//	 */
//	private void assignFinalLinkConfiguration(HashMap< Spot, ArrayList<Spot> > links)
//	{
//		
//	}
//	
//	
}
