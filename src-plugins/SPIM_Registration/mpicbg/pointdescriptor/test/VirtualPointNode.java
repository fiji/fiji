package mpicbg.pointdescriptor.test;

import fiji.util.node.Leaf;
import mpicbg.models.Point;

public class VirtualPointNode<P extends Point> implements Leaf<VirtualPointNode<P>>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final P p;
	final int numDimensions;
	
	public VirtualPointNode( final P p )
	{
		this.p = p;
		this.numDimensions = p.getL().length;
	}
	
	public P getPoint() { return p; }

	@Override
	public float distanceTo( final VirtualPointNode<P> other ) { return Point.distance( p, other.getPoint() ); }

	@Override
	public float get( final int k ) { return p.getW()[ k ]; }

	@Override
	public int getNumDimensions() { return numDimensions; }

	@Override
	public boolean isLeaf() { return true; }

	@Override
	public VirtualPointNode<P>[] createArray( final int n ) { return new VirtualPointNode[ n ]; }
}
