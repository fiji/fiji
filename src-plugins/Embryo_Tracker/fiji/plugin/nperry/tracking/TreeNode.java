package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import fiji.plugin.nperry.Spot;

public class TreeNode {
	/*
	 * FIELDS
	 */
	private TreeNode child;
	private ArrayList< ArrayList<Spot> > potentialMatches;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public TreeNode() { 
		this.child = null;
	}
	
	public TreeNode(TreeNode child) {
		this.child = child;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Returns a reference to the CellNode that this CellNode "points" to. "Points" can be thought of
	 * as a <b>directed edge</b> from this CellNode to this.child.
	 */
	public TreeNode getChild() {
		return this.child;
	}
	
	public void setChild() {
		
	}
	
	/**
	 * Returns an ArrayList of Spots, which represents a possible match configuration during the linking step.
	 * @return Returns <code>null</code> if there are no more potential matches, otherwise returns an <code>ArrayList</code>
	 * of <code>Spot</code>.
	 */
	public ArrayList<Spot> getNextMatch() {
		if (potentialMatches.size() > 0) {
			return potentialMatches.remove(0);
		} else{
			return null;
		}
	}
}
