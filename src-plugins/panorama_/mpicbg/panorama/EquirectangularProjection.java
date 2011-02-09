package mpicbg.panorama;

import mpicbg.models.NoninvertibleModelException;
import mpicbg.util.Util;

/**
 * A rectlinear projection from equirectangular coordinates (longitude,
 * latitude). The rectlinear frame is defined by the
 * {@link #getF() focal length}, its dimensions in #get<i>x</i> and <i>y</i>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 * 
 */
public class EquirectangularProjection extends PanoramaCamera< EquirectangularProjection >
{

	final private float PI = ( float )Math.PI;


	final static private float asin( final float a )
	{
		return ( float )Math.asin( a );
	}
	
	final static private float sqrt( final float alpha )
	{
		return ( float )Math.sqrt( alpha );
	}
	
	/* the equirectangular map */
	private float lambdaPiScale = 0;
	final public float getLambdaPiScale(){ return lambdaPiScale; }
	final public void setLambdaPiScale( final float lambdaPiScale ){ this.lambdaPiScale = lambdaPiScale; }
	
	private float phiPiScale = 0;
	final public float getPhiPiScale(){ return phiPiScale; }
	final public void setPhiPiScale( final float phiPiScale ){ this.phiPiScale = phiPiScale; }
	
	private float minLambda = 0;
	final public float getMinLambda(){ return minLambda; }
	final public void setMinLambda( final float minLambda ){ this.minLambda = minLambda; }
	
	private float minPhi = 0;
	final public float getMinPhi(){ return minPhi; }
	final public void setMinPhi( final float minPhi ){ this.minPhi = minPhi; }
	
	//@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 2 : "2d projections can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return t;
	}

	//@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length == 2 : "2d projections can be applied to 2d points only.";
		
		System.err.println( "Not yet implemented.  Please feel free to do it yourself." );
		// TODO implement it
	}

	//@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d projections can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return null;
	}

	//@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d projections can be applied to 2d points only.";
		
		/* scale with respect to targetHeight and f */
		final float x = ( point[ 0 ] - 0.5f * targetWidth ) / targetMaxSize;
		final float y = ( point[ 1 ] - 0.5f * targetHeight ) / targetMaxSize;
		
		/* calculate sphere cut */
		final float t = 1.0f / sqrt( x * x + y * y + f * f );
		
		final float tx = t * x;
		final float ty = t * y;
		final float tz = t * f;
		
		/* rotate */
		final float rx = i.m00 * tx + i.m01 * ty  + i.m02 * tz;
		final float ry = i.m10 * tx + i.m11 * ty  + i.m12 * tz;
		final float rz = i.m20 * tx + i.m21 * ty  + i.m22 * tz; 
		
		/* calculate phi and lambda */
		final float tLambda;
		if ( rz < 0 )
			tLambda = ( asin( -rx / sqrt( rx * rx + rz * rz ) ) + PI - minLambda ) / PI;
		else
			tLambda = ( asin( rx / sqrt( rx * rx + rz * rz ) ) - minLambda ) / PI;
			
		point[ 0 ] = Util.mod( tLambda, 2 );
		point[ 1 ] = ( ( float )Math.asin( ry ) - minPhi ) / PI + 0.5f;
		
		point[ 0 ] = point[ 0 ] * lambdaPiScale;
		point[ 1 ] = point[ 1 ] * phiPiScale;
	}
	
	@Override
	final public EquirectangularProjection clone()
	{
		final EquirectangularProjection c = new EquirectangularProjection();
		
		c.set( this );
		
		return c;
	}
	
	final public void set( final EquirectangularProjection e )
	{
		super.set( e );
		
		lambdaPiScale = e.lambdaPiScale;
		minLambda = e.minLambda;
		minPhi = e.minPhi;
		phiPiScale = e.phiPiScale;
	}

	@Override
	final public String toString()
	{
		return "";
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	final public EquirectangularProjection createInverse()
	{
		final EquirectangularProjection ict = new EquirectangularProjection();
		return ict;
		
	}
}
