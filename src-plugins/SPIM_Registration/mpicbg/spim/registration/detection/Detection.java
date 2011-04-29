package mpicbg.spim.registration.detection;

import mpicbg.models.Point;
import fiji.util.node.Leaf;

public abstract class Detection< T extends Detection< T > > extends Point implements Leaf< T >
{
	private static final long serialVersionUID = 1L;
	
	final protected int id;
	protected double weight;
	protected boolean useW = false;

	// used for display
	protected float distance = -1;

	// used for recursive parsing
	protected boolean isUsed = false;		
	
	public Detection( final int id, final float[] location )
	{
		super( location );
		this.id = id;
	}

	public void setWeight( final double weight ){ this.weight = weight; }
	public double getWeight(){ return weight; }
	public int getID() { return id; }
	public void setDistance( float distance )  { this.distance = distance; }
	public float getDistance() { return distance; }
	public boolean isUsed() { return isUsed; }
	public void setUsed( final boolean isUsed ) { this.isUsed = isUsed; }

	public boolean equals( final DetectionView<?,?> otherDetection )
	{
		if ( useW )
		{
			for ( int d = 0; d < 3; ++d )
				if ( w[ d ] != otherDetection.w[ d ] )
					return false;			
		}
		else
		{
			for ( int d = 0; d < 3; ++d )
				if ( l[ d ] != otherDetection.l[ d ] )
					return false;						
		}
				
		return true;
	}
	
	public void setW( final float[] wn )
	{
		for ( int i = 0; i < w.length; ++i )
			w[ i ] = wn[ i ];
	}
	
	public void resetW()
	{
		for ( int i = 0; i < w.length; ++i )
			w[i] = l[i];
	}
	
	public double getDistance( final Point point2 )
	{
		double distance = 0;
		final float[] a = getL();
		final float[] b = point2.getW();
		
		for ( int i = 0; i < getL().length; ++i )
		{
			final double tmp = a[ i ] - b[ i ];
			distance += tmp * tmp;
		}
		
		return Math.sqrt( distance );
	}
	
	@Override
	public boolean isLeaf() { return true; }

	@Override
	public float distanceTo( final T o ) 
	{
		final float x = o.get( 0 ) - get( 0 );
		final float y = o.get( 1 ) - get( 1 );
		final float z = o.get( 2 ) - get( 2 );
		
		return (float)Math.sqrt(x*x + y*y + z*z);
	}
	
	public void setUseW( final boolean useW ) { this.useW = useW; } 
	public boolean getUseW() { return useW; } 

	@Override
	public float get( final int k ) 
	{
		if ( useW )
			return w[ k ];
		else
			return l[ k ];
	}
	
	@Override
	public int getNumDimensions() { return 3; }	
}
