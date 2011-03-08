package mpicbg.spim.registration.segmentation;

import mpicbg.imglib.util.Util;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.detection.DetectionView;

public class Nucleus extends DetectionView<NucleusIdentification, Nucleus>
{
	private static final long serialVersionUID = 1L;

	protected double weight = 1;
	float diameter = 1;

	public Nucleus( final int id, final float[] location, final ViewDataBeads myView ) 
	{
		super( id, location, myView );
	}

	public void setDiameter( final float diameter ) { this.diameter = diameter; }
	public float getDiameter() { return diameter; }

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
		String desc = "Nucleus " + getID() + " l"+ Util.printCoordinates( getL() ) + "; w"+ Util.printCoordinates( getW() );
		
		if ( myView != null)
			return desc + " of view " + myView;
		else
			return desc + " - no view assigned";
	}

	@Override
	public Nucleus[] createArray( final int n ){ return new Nucleus[ n ];	}

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
	
	public boolean isTrueCorrespondence = false;
	public boolean isFalseCorrespondence = false;
	public boolean isAmbigous = false;
	public boolean isUnique = false;
	public int numCorr = 0;

	@Override
	public NucleusIdentification createIdentification() 
	{
		return new NucleusIdentification( this );
	}
}
