package mpicbg.spim.registration.segmentation;

import fiji.util.node.Leaf;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.models.Point;
import mpicbg.spim.registration.ViewDataBeads;

public class Nucleus extends Point implements Leaf<Nucleus>
{
	private static final long serialVersionUID = 1L;

	final protected int id;
	final protected ViewDataBeads myView;
	
	protected double weight = 1;

	public Nucleus( final int id, final float[] location, final ViewDataBeads myView ) 
	{
		super( location );
		this.id = id;
		this.myView = myView;
	}

	public ViewDataBeads getView() { return myView; }
	public int getViewID() { return myView.getID(); }
	public int getID() { return id; }	
	public void setWeight( final double weight ){ this.weight = weight; }
	public double getWeight(){ return weight; }
	
	final public void setW( final float[] wn )
	{
		for ( int i = 0; i < Math.min( w.length, wn.length ); ++i )
			w[i] = wn[i];
	}
	
	final public void resetW()
	{
		for ( int i = 0; i < w.length; ++i )
			w[i] = l[i];
	}

	protected boolean useW = true;
	
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
	
	public void set( final float v, final int k ) 
	{
		if ( useW )
			w[ k ] = v;
		else
			l[ k ] = v;
	}	

	@Override
	public String toString()
	{
		String desc = "Nucleus " + getID() + " l"+ MathLib.printCoordinates( getL() ) + "; w"+ MathLib.printCoordinates( getW() );
		
		if ( myView != null)
			return desc + " of view " + myView;
		else
			return desc + " - no view assigned";
	}

	public boolean isLeaf() { return true; }

	@Override
	public float distanceTo( final Nucleus o )
	{
		final float x = o.get( 0 ) - get( 0 );
		final float y = o.get( 1 ) - get( 1 );
		final float z = o.get( 2 ) - get( 2 );
		
		return (float)Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Nucleus[] createArray( final int n ){ return new Nucleus[ n ];	}

	@Override
	public int getNumDimensions(){ return 3; }
	
	public boolean equals( final Nucleus o )
	{
		if ( useW )
		{
			for ( int d = 0; d < 3; ++d )
				if ( w[ d ] != o.w[ d ] )
					return false;			
		}
		else
		{
			for ( int d = 0; d < 3; ++d )
				if ( l[ d ] != o.l[ d ] )
					return false;						
		}
				
		return true;
	}
	
	public static boolean equals( final Nucleus nucleus1, final Nucleus nucleus2 )
	{
		if ( nucleus1.getID() == nucleus2.getID() && nucleus1.getViewID() == nucleus2.getViewID() )
			return true;
		else
			return false;
	}
	
	public boolean isCorrespondence = false;
	public boolean isReference = false;
	public boolean isCoordinate = false;
}
