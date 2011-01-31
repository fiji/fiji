package mpicbg.panorama;

import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.util.Matrix3x3;

/**
 * Essentially, a simplified homography that allows panning (&lambda;), tilting
 * (&phi;) and and zooming (f) only.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 * 
 */
public abstract class PanoramaCamera< T extends PanoramaCamera< T > > implements InvertibleCoordinateTransform
{
	final static private float cos( final float a ){ return ( float )Math.cos( a ); }
	final static private float sin( final float a ){ return ( float )Math.sin( a ); }
	
	/* orientation */
	final protected Matrix3x3 m = new Matrix3x3();
	
	/* inverse orientation */
	final protected Matrix3x3 i = new Matrix3x3();
	
	/* focal length */
	protected float f = 1;
	final public float getF(){ return f; }
	final public void setF( final float f ){ this.f = f; }
	
	/**
	 * 
	 * @param lambda
	 * @param phi
	 * @param rho
	 */
	final public void setOrientation(
			final float lambda,
			final float phi,
			final float rho )
	{
		final float sinLambda = sin( lambda );
		final float cosLambda = cos( lambda );
		
		final float sinPhi = sin( phi );
		final float cosPhi = cos( phi );
		
		final float sinRho = sin( rho );
		final float cosRho = cos( rho );
		
		/* TODO calculate m */
		
		/* TODO reduce this */
		final Matrix3x3 panInverse = new Matrix3x3(
				cosLambda, 0, -sinLambda,
				0, 1, 0,
				sinLambda, 0, cosLambda );
		
		final Matrix3x3 tiltInverse = new Matrix3x3(
				1, 0, 0,
				0, cosPhi, sinPhi,
				0, -sinPhi, cosPhi );
		
		final Matrix3x3 rollInverse = new Matrix3x3(
				cosRho, sinRho, 0,
				-sinRho, cosRho, 0,
				0, 0, 1 );
		
		i.set( rollInverse );
		i.preConcatenate( tiltInverse );
		i.preConcatenate( panInverse );
	}
	
	final public void pan( final float lambda )
	{
		final float cosLambda = cos( lambda );
		final float sinLambda = sin( lambda );
		
		/* TODO calculate m */
		
		/* TODO reduce this */
		final Matrix3x3 panInverse = new Matrix3x3(
				cosLambda, 0, -sinLambda,
				0, 1, 0,
				sinLambda, 0, cosLambda );
		i.concatenate( panInverse );
	}
	
	final public void tilt( final float phi )
	{
		final float cosPhi = cos( phi );
		final float sinPhi = sin( phi );
		
		/* TODO calculate m */
		
		/* TODO reduce this */
		final Matrix3x3 tiltInverse = new Matrix3x3(
				1, 0, 0,
				0, cosPhi, sinPhi,
				0, -sinPhi, cosPhi );
		i.concatenate( tiltInverse );
	}
	
	final public void roll( final float rho )
	{
		final float cosRho = cos( rho );
		final float sinRho = sin( rho );
		
		/* TODO calculate m */
		
		/* TODO reduce this */
		final Matrix3x3 panInverse = new Matrix3x3(
				cosRho, sinRho, 0,
				-sinRho, cosRho, 0,
				0, 0, 1 );
		i.concatenate( panInverse );
	}
	
	/* the target (camera plane)*/
	protected float targetMaxSize = 0;
	
	protected float targetWidth = 0;
	protected float targetWidth2 = 0;
	final public float getTargetWidth(){ return targetWidth; }
	final public void setTargetWidth( final float targetWidth )
	{
		this.targetWidth = targetWidth;
		targetWidth2 = 0.5f * targetWidth;
		targetMaxSize = Math.max( targetWidth, targetHeight );
	}
	
	protected float targetHeight = 0;
	protected float targetHeight2 = 0;
	final public float getTargetHeight(){ return targetHeight; }
	final public void setTargetHeight( final float targetHeight )
	{
		this.targetHeight = targetHeight;
		targetHeight2 = 0.5f * targetHeight;
		targetMaxSize = Math.max( targetWidth, targetHeight );
	}
	
	
	final public void resetOrientation()
	{
		m.reset();
		i.reset();
	}
	
	
	final public void concatenateOrientation( final T p )
	{
		m.concatenate( p.m );
		i.preConcatenate( p.i );
	}
	
	
	final public void preConcatenateOrientation( final T p )
	{
		m.preConcatenate( p.m );
		i.concatenate( p.i );
	}
	
	final public void setCamera( final PanoramaCamera< ? > c )
	{
		f = c.f;
		i.set( c.i );
		m.set( c.m );
	}
	
	@Override
	abstract public T clone();
	
	public void set( final T t )
	{
		f = t.f;
		i.set( t.i );
		m.set( t.m );
		targetHeight = t.targetHeight;
		targetHeight2 = t.targetHeight2;
		targetMaxSize = t.targetMaxSize;
		targetWidth = t.targetWidth;
		targetWidth2 = t.targetWidth2;
	}
}
