package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

public class ObjectTracker {

	/*
	 * FIELDS
	 */
	
	/** Holds the extrema for each frame. Each index (outer ArrayList) represents a single frame. */
	private final ArrayList< ArrayList<Spot> > extrema;
	/** Biologically defined event - cell death. */
	private final static int CELL_DEATH = 0;
	/** Biologically defined event - cell match */
	private final static int CELL_MATCH = 1;
	/** Biologically defined event - cell division */
	private final static int CELL_DIVISION = 2;
	/** Biologically defined event - cell merge */
	private final static int MERGE = 4;
	 
	/*
	 * CONSTRUCTORS
	 */
	
	public ObjectTracker(ArrayList< ArrayList<Spot> > extrema) {
		this.extrema = extrema;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	public void process() {
//		int counter = 1;
//		for (ArrayList<Spot> spots : extrema) {
//			System.out.println("--- Frame " + counter + " ---");
//			for (Spot spot : spots) {
//				double[] coords = spot.getCoordinates();
//				System.out.println("[" + coords[0] + ", " + coords[1] + ", " + coords[2] + "] (" + coords[0] * .2 + ", " + coords[1] * .2 + ", " + coords[2] + ")");
//			}
//			counter++;
//			System.out.println();
//		}
//		ArrayList<Spot> frame = null;
//		ArrayList<Spot> nextFrame = null;
//		for (int i = 0; i < extrema.size() - 1; i++) {  // -1 here, because the last frame can't be linked to any subsequent frames.
//			// 1 - store frame i, and frame i+1
//			frame = extrema.get(i);
//			nextFrame = extrema.get(i + 1);
//			
//			TreeNode root = new TreeNode();
//		}
		
		
	}
	
	public void getResults() {
		
	}
	
}
