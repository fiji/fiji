package mpicbg.stitching;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class PointMatchStitching extends PointMatch 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ComparePair pair;
	
	/**
	 * Constructor
	 * 
	 * Create a {@link PointMatch} with one weight.
	 * 
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 */
	public PointMatchStitching(
			Point p1,
			Point p2,
			float weight,
			ComparePair pair )
	{
		super ( p1, p2, weight );
		
		this.pair = pair;
	}
	
	public ComparePair getPair() { return pair; }

}
