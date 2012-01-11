package selection;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class ExtendedPointMatch extends PointMatch
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final float radius1, radius2;
	
	public ExtendedPointMatch( final Point p1, final Point p2, final float radius1, final float radius2 ) 
	{
		super( p1, p2 );
		
		this.radius1 = radius1;
		this.radius2 = radius2;
	}

}
