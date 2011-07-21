package mpicbg.spim.mpicbg;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.Model;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.detection.AbstractDetection;

public class TileSPIM< M extends AbstractAffineModel3D<M> > extends Tile<M> 
{
	/**
	 * Constructor
	 * 
	 * @param model the transformation {@link Model} of the {@link Tile}.
	 */
	public TileSPIM( final M model, final ViewDataBeads parent )
	{
		super( model );
		this.parent = parent;
	}

	final protected ViewDataBeads parent;
	
	public ViewDataBeads getParent(){ return parent; }

	/**
	 * Apply the current {@link Model} to all local point coordinates.
	 * Update {@link #cost} and {@link #distance}.
	 *
	 */
	final public void updateWithDections()
	{
		// call the original method
		update();
		
		if ( matches.size() > 0 )
		{
			for ( final PointMatch match : matches )
			{
				final double dl = match.getDistance();
				((AbstractDetection<?>)match.getP1()).setDistance( (float)dl );
				((AbstractDetection<?>)match.getP2()).setDistance( (float)dl );				
			}
		}

	}
}
