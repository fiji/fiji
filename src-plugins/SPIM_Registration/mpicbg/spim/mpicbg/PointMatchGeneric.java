package mpicbg.spim.mpicbg;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class PointMatchGeneric <P extends Point> extends PointMatch
{
	private static final long serialVersionUID = 1L;

	public PointMatchGeneric( P p1, P p2, float[] weights, float strength )
	{
		super( p1, p2, weights, strength );
	}
	
	public PointMatchGeneric( P p1, P p2, float[] weights )
	{
		super( p1, p2, weights );
	}
	
	public PointMatchGeneric( P p1, P p2, float weight )
	{
		super( p1, p2, weight );
	}

	public PointMatchGeneric( P p1, P p2, float weight, float strength )
	{
		super( p1, p2, weight, strength );
	}
	
	public PointMatchGeneric( P p1, P p2 )
	{
		super( p1, p2 );
	}
	
	final public P getPoint1() { return (P)getP1(); }
	
	final public P getPoint2() { return (P)getP2(); }
	
}
