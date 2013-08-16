package mpicbg.panorama;

import mpicbg.models.NoninvertibleModelException;

/**
 * Essentially, a simplified homography that allows panning (&lambda;), tilting
 * (&phi;) and and zooming (f) only.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 * 
 */
public class RectlinearCamera extends PanoramaCamera< RectlinearCamera >
{
	/* the source (rotating image plane) */
	private float sourceMaxSize = 0;
	
	private float sourceWidth = 0;
	private float sourceWidth2 = 0;
	final public float getSourceWidth(){ return sourceWidth; }
	final public void setSourceWidth( final float sourceWidth )
	{
		this.sourceWidth = sourceWidth;
		sourceWidth2 = 0.5f * sourceWidth;
		sourceMaxSize = Math.max( sourceWidth, sourceHeight );
	}
	
	private float sourceHeight = 0;
	private float sourceHeight2 = 0;
	final public float getSourceHeight(){ return sourceHeight; }
	final public void setSourceHeight( final float sourceHeight )
	{
		this.sourceHeight = sourceHeight;
		sourceHeight2 = 0.5f * sourceHeight;
		sourceMaxSize = Math.max( sourceWidth, sourceHeight );
	}
	
	//@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 3 : "2d homogeneous projections can be applied to 2d homogeneous points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return t;
	}

	//@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length == 3 : "2d homogeneous projections can be applied to 2d homogeneous points only.";
		
		System.err.println( "Not yet implemented.  Please feel free to do it yourself." );
		// TODO implement it
	}

	//@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 3 : "2d homogeneous projections can be applied to 2d homogeneous points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return null;
	}

	//@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 3 : "2d homogeneous projections can be applied to 2d homogeneous points only.";
		
		
		/* scale with respect to target */
		final float x = ( point[ 0 ] - targetWidth2 ) / targetMaxSize;
		final float y = ( point[ 1 ] - targetHeight2 ) / targetMaxSize;
		final float z = point[ 2 ];
		
		/* TODO the multiples of f only depend on f and can be stored on update of lambda, phi or f */
		final float xt = i.m00 * x + i.m01 * y  + i.m02 * z * f;
		final float yt = i.m10 * x + i.m11 * y  + i.m12 * z * f;
		final float zt = ( i.m20 * x + i.m21 * y  + i.m22 * z * f ) * 2;
		
		point[ 0 ] = xt / zt * sourceMaxSize + sourceWidth2;
		point[ 1 ] = yt / zt * sourceMaxSize + sourceHeight2;
		point[ 2 ] = zt;
	}
	
	@Override
	final public RectlinearCamera clone()
	{
		final RectlinearCamera c = new RectlinearCamera();
		c.set( this );
		return c;
	}
	
	final public void set( final RectlinearCamera e )
	{
		super.set( e );
		
		sourceHeight = e.sourceHeight;
		sourceHeight2 = e.sourceHeight2;
		sourceMaxSize = e.sourceMaxSize;
		sourceWidth = e.sourceWidth;
		sourceWidth2 = e.sourceWidth2;
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
	final public RectlinearCamera createInverse()
	{
		final RectlinearCamera ict = new RectlinearCamera();
		return ict;
		
	}
}
