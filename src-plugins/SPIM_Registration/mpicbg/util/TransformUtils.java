package mpicbg.util;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;

import mpicbg.imglib.util.Util;
import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.RigidModel3D;

public class TransformUtils
{
	/**
	 * Return the min and max coordinate of the transformed image in each dimension
	 * relative to the dimensions of the image it is based on. This is important
	 * for computing bounding boxes.
	 *
	 * @param dimensions - the dimensions of the image
	 * @param transform - the transformation
	 *
	 * @return - float[ numDimensions ][ 2 ], in the respective dimension d
	 * float[ d ][ 0 ] is min, float[ d ][ 1 ] is max
	 */
	public static float[][] getMinMaxDim( final int[] dimensions, final CoordinateTransform transform )
	{
		final int numDimensions = dimensions.length;

		final float[] tmp = new float[ numDimensions ];
		final float[][] minMaxDim = new float[ numDimensions ][ 2 ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			minMaxDim[ d ][ 0 ] = Float.MAX_VALUE;
			minMaxDim[ d ][ 1 ] = -Float.MAX_VALUE;
		}

		// recursively get all corner points of the image, assuming they will still be the extremum points
		// in the transformed image
		final boolean[][] positions = new boolean[ Util.pow( 2, numDimensions ) ][ numDimensions ];
		Util.setCoordinateRecursive( numDimensions - 1, numDimensions, new int[ numDimensions ], positions );

		// get the min and max location for each dimension independently
		for ( int i = 0; i < positions.length; ++i )
		{
			for ( int d = 0; d < numDimensions; ++d )
			{
				if ( positions[ i ][ d ])
					tmp[ d ] = dimensions[ d ];
				else
					tmp[ d ] = 0;
			}

			transform.applyInPlace( tmp );

			for ( int d = 0; d < numDimensions; ++d )
			{
				if ( tmp[ d ] < minMaxDim[ d ][ 0 ])
					minMaxDim[ d ][ 0 ] = tmp[ d ];

				if ( tmp[ d ] > minMaxDim[ d ][ 1 ])
					minMaxDim[ d ][ 1 ] = tmp[ d ];
			}
		}

		return minMaxDim;
	}
	
	public static Matrix4f getMatrix4f( final AffineModel3D model )
	{
		final Matrix4f matrix = new Matrix4f();

		final float[] m = new float[ 12 ];
		model.getMatrix( m );

		matrix.m00 = m[ 0 ];
		matrix.m01 = m[ 1 ];
		matrix.m02 = m[ 2 ];
		matrix.m03 = m[ 3 ];
		matrix.m10 = m[ 4 ];
		matrix.m11 = m[ 5 ];
		matrix.m12 = m[ 6 ];
		matrix.m13 = m[ 7 ];
		matrix.m20 = m[ 8 ];
		matrix.m21 = m[ 9 ];
		matrix.m22 = m[ 10 ];
		matrix.m23 = m[ 11 ];
		matrix.m30 = 0;
		matrix.m31 = 0;
		matrix.m32 = 0;
		matrix.m33 = 0;

		return matrix;
	}

	public static Matrix4f getMatrix4f( final RigidModel3D model )
	{
		final Matrix4f matrix = new Matrix4f();

		final float[] m = new float[ 12 ];
		model.getMatrix( m );

		matrix.m00 = m[ 0 ];
		matrix.m01 = m[ 1 ];
		matrix.m02 = m[ 2 ];
		matrix.m03 = m[ 3 ];
		matrix.m10 = m[ 4 ];
		matrix.m11 = m[ 5 ];
		matrix.m12 = m[ 6 ];
		matrix.m13 = m[ 7 ];
		matrix.m20 = m[ 8 ];
		matrix.m21 = m[ 9 ];
		matrix.m22 = m[ 10 ];
		matrix.m23 = m[ 11 ];
		matrix.m30 = 0;
		matrix.m31 = 0;
		matrix.m32 = 0;
		matrix.m33 = 0;

		return matrix;
	}
	
	public static Transform3D getTransform3D1( final AbstractAffineModel3D<?> model )
	{
		final Transform3D transform = new Transform3D();
		final float[] m = model.getMatrix( null );

		final float[] m2 = new float[ 16 ];
		transform.get( m2 );

		for ( int i = 0; i < m.length; ++i )
			m2[ i ] = m[ i ];

		transform.set( m2 );

		return transform;
	}
	
	public static <M extends AbstractAffineModel3D<M>> Transform3D getTransform3D( final M model )
	{
		final Transform3D transform = new Transform3D();
		final float[] m = model.getMatrix( null );

		final float[] m2 = new float[ 16 ];
		transform.get( m2 );

		for ( int i = 0; i < m.length; ++i )
			m2[ i ] = m[ i ];

		transform.set( m2 );

		return transform;
	}

	public static AffineModel3D getAffineModel3D( Transform3D transform )
	{
		final float[] m = new float[16];
		transform.get( m );

		AffineModel3D model = new AffineModel3D();
		model.set( m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11] );

		return model;
	}

	public static RigidModel3D getRigidModel3D( Transform3D transform )
	{
		final float[] m = new float[16];
		transform.get( m );

		RigidModel3D model = new RigidModel3D();
		model.set( m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11] );

		return model;
	}

}
