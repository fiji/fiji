package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.math.MathLib;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class ObjectTracker {

	/*
	 * FIELDS
	 */
	
	/** Holds the extrema for each frame. Each index (outer ArrayList) represents a single frame. */
	private final ArrayList< ArrayList<Spot> > points;
	/** Biologically defined event - cell death. */
	private final static int CELL_DEATH = 0;
	/** Biologically defined event - cell match */
	private final static int CELL_MATCH = 1;
	/** Biologically defined event - cell division */
	private final static int CELL_DIVISION = 2;
	/** Biologically defined event - cell merge */
	private final static int MERGE = 4;
	/** Used to store whether checkInput() was run or not. */
	private boolean inputChecked = false;
	/** This String will hold any error message incurred during the use of this class. */
	private String errorMessage = "";
	/** The maximum distance away potential Spots can be in t+1 from the Spot in t (centroid to centroid, in physical units). */
	private final static float maxDist = 10f;
	/** The maximum number of nearest Spots in t+1 to link to the Spot in t */
	private final static int nLinks = 3;
	 
	/*
	 * CONSTRUCTORS
	 */
	
	public ObjectTracker(ArrayList< ArrayList<Spot> > points)
	{
		this.points = points;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Returns any error messages that develop during the use of this class.
	 * @return The error message.
	 */
	public String getErrorMessage()
	{
		return this.errorMessage;
	}
	
	/**
	 * Call this method before calling process, in order to guarantee that all of the required input is correct.
	 * @return true if no errors, false otherwise.
	 */
	public boolean checkInput()
	{
		if (points.isEmpty()) {
			errorMessage = "There are no points.";
			return false;
		}
		
		inputChecked = true;
		return true;
	}
	

	public boolean process()
	{
		/*
		 * Make sure checkInput() has been called first.
		 */
		if (!inputChecked){
			errorMessage = "checkInput() was not run before calling process()!";
			return false;
		}
		
		for (int i = 0; i < points.size() - 1; i++) {
			 NearestNeighborLinker linker = new NearestNeighborLinker(points.get(i), points.get(i+1), nLinks, maxDist);
			 if (!linker.checkInput() || !linker.process()) {
				 System.out.println("NearestNeighborLinker failure: " + linker.getErrorMessage());
			 }
			 ArrayList< ArrayList<Spot> > links = linker.getResult();
			 
			 // <debug>
			 System.out.println("Frame " + (i + 1));
			 System.out.println("------------------");
			 for (int j = 0; j < links.size(); j++) {
				 ArrayList<Spot> linked = links.get(j);
				 System.out.println("Spot " + MathLib.printCoordinates(points.get(i).get(j).getCoordinates()) + " is linked to: ");
				 for (int k = 0; k < linked.size(); k++) {
					 System.out.println(MathLib.printCoordinates(linked.get(k).getCoordinates()));
				 }
			 }
			 // </debug>
		}
		
		
		
		return true;
	}
	
	
	public void getResults()
	{
		
	}
	
}
