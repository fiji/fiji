package fiji.plugin.trackmate.tracking.kdtree;

import fiji.plugin.trackmate.Spot;
import mpicbg.imglib.algorithm.kdtree.node.Leaf;

public class SpotNode implements Leaf<SpotNode> {

	
	private Spot spot;
	private float[] pos;
	/**
	 * A public flag that states whether this node is already part of a link. If 
	 * true, then another node must be sought for linking in a nearest neighbor search.
	 */
	public boolean isAssigned = false;

	public SpotNode(Spot spot) {
		this.spot = spot;
		this.pos = spot.getPosition(null);
	}
	
	/*
	 * METHODS
	 */
	
	public Spot getSpot() {
		return spot;
	}
	
	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public float get(int k) {
		return pos[k];
	}

	@Override
	public float distanceTo(SpotNode other) {
		return spot.squareDistanceTo(other.getSpot());
	}

	@Override
	public int getNumDimensions() {
		return pos.length;
	}

	@Override
	public SpotNode[] createArray(int n) {
		return new SpotNode[n];
	}

	@Override
	public SpotNode getEntry() {
		return this;
	}

}
