package mpicbg.icp;


import java.util.ArrayList;
import java.util.List;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import fiji.util.KDTree;
import fiji.util.NearestNeighborSearch;
import fiji.util.node.Leaf;

public class SimplePointMatchIdentification < P extends Point & Leaf<P> > implements PointMatchIdentification<P>
{
	float distanceThresold;
	
	public SimplePointMatchIdentification( final float distanceThreshold )
	{
		this.distanceThresold = distanceThreshold;
	}
	
	public SimplePointMatchIdentification()
	{
		this.distanceThresold = Float.MAX_VALUE;
	}
	
	public void setDistanceThreshold( final float distanceThreshold ) { this.distanceThresold = distanceThreshold; }
	public float getDistanceThreshold() { return this.distanceThresold; }
	
	@Override
	public ArrayList<PointMatch> assignPointMatches( final List<P> target, final List<P> reference )
	{
		final ArrayList<PointMatch> pointMatches = new ArrayList<PointMatch>();
		
		final KDTree<P> kdTreeTarget = new KDTree<P>( target );		
		final NearestNeighborSearch<P> nnSearchTarget = new NearestNeighborSearch<P>( kdTreeTarget );
		
		for ( final P point : reference )
		{
			final P correspondingPoint = nnSearchTarget.findNearestNeighbor( point );
			
			if ( correspondingPoint.distanceTo( point ) <= distanceThresold )
				pointMatches.add( new PointMatch ( correspondingPoint, point ) );
		}
				
		return pointMatches;
	}
}
