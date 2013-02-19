package mpicbg.pointdescriptor.model;

import java.util.Collection;

import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * 2d-rigid transformation models to be applied to points in 2d-space.
 * This model includes the closed form weighted least squares solution as
 * described by \citet{SchaeferAl06} and implemented by Johannes Schindelin.  
 * 
 * BibTeX:
 * <pre>
 * &#64;article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   year      = {2006},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 *   url       = {http://faculty.cs.tamu.edu/schaefer/research/mls.pdf},
 * }
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 * 
 */
public class TranslationInvariantRigidModel2D extends TranslationInvariantModel<TranslationInvariantRigidModel2D> 
{
	static final protected int MIN_NUM_MATCHES = 2;
	
	protected float cos = 1.0f, sin = 0.0f, tx = 0.0f, ty = 0.0f;
	protected float itx = 0.0f, ity = 0.0f;
	
	@Override
	public boolean canDoNumDimension( final int numDimensions ) { return numDimensions == 2; }
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
		
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = cos * l0 - sin * l[ 1 ] + tx;
		l[ 1 ] = sin * l0 + cos * l[ 1 ] + ty;
	}

	@Override
	public TranslationInvariantRigidModel2D copy()
	{
		final TranslationInvariantRigidModel2D m = new TranslationInvariantRigidModel2D();
		m.cos = cos;
		m.sin = sin;
		m.tx = tx;
		m.ty = ty;
		m.itx = itx;
		m.ity = ity;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationInvariantRigidModel2D m )
	{
		cos = m.cos;
		sin = m.sin;
		tx = m.tx;
		ty = m.ty;
		itx = m.itx;
		ity = m.ity;
		cost = m.cost;
	}
	
	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );
				
		cos = 0;
		sin = 0;
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();

			final float x1 = p[ 0 ];
			final float y1 = p[ 1 ]; // x2
			final float x2 = q[ 0 ]; // y1
			final float y2 = q[ 1 ]; // y2
			sin += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			cos += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
		}
		final float norm = ( float )Math.sqrt( cos * cos + sin * sin );
		cos /= norm;
		sin /= norm;		
	}
}
