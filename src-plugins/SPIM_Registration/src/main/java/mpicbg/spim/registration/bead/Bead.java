package mpicbg.spim.registration.bead;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import mpicbg.imglib.util.Util;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.detection.DetectionView;

public class Bead extends DetectionView< BeadIdentification, Bead >
{
	// so the gauss fit knows if it relocalize it already
	public boolean relocalized = false;
	
	private static final long serialVersionUID = -2875282502611466531L;
	
	public Bead( final int id, final Point3d location, final ViewDataBeads myView )
	{
		super( id, new float[] { (float)location.x, (float)location.y, (float)location.z}, myView );
	}

	public Bead( final int id, final Point3f location, final ViewDataBeads myView )
	{
		super( id, new float[] { location.x, location.y, location.z}, myView );
	}

	public Bead( final int id, final float[] location, final ViewDataBeads myView )
	{
		super( id, location, myView );
	}
			
	@Override
	public String toString()
	{
		String desc = "Bead " + getID() + " l"+ Util.printCoordinates( getL() ) + "; w"+ Util.printCoordinates( getW() );
		
		if ( myView != null)
			return desc + " of view " + myView;
		else
			return desc + " - no view assigned";
	}
	
	public boolean equals( final Bead bead )
	{
		if ( this.getID() == bead.getID() && this.getViewID() == bead.getViewID() )
			return true;
		else
			return false;
	}

	public boolean equals( final BeadIdentification beadID )
	{
		if ( this.getID() == beadID.getDetectionID() && this.getViewID() == beadID.getViewID() )
			return true;
		else
			return false;
	}

	@Override
	public Bead[] createArray( final int n ){ return new Bead[ n ];	}

	@Override
	public BeadIdentification createIdentification() 
	{
		return new BeadIdentification( this );
	}	
}
