package mpicbg.pointdescriptor.model;

import java.util.Collection;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;

import math3d.JacobiFloat;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * 3d-rigid transformation models to be applied to points in 3d-space.
 * 
 * This function uses the method by Horn, using quaternions:
 * Closed-form solution of absolute orientation using unit quaternions,
 * Horn, B. K. P., Journal of the Optical Society of America A,
 * Vol. 4, page 629, April 1987
 * 
 * @author Johannes Schindelin (quaternion logic and implementation) and Stephan Preibisch
 * @version 0.1b
 * 
 */
public class TranslationInvariantRigidModel3D extends TranslationInvariantModel<TranslationInvariantRigidModel3D> 
{
	static final protected int MIN_NUM_MATCHES = 3;
	
	protected float
		m00 = 1.0f, m01 = 0.0f, m02 = 0.0f, 
		m10 = 0.0f, m11 = 1.0f, m12 = 0.0f, 
		m20 = 0.0f, m21 = 0.0f, m22 = 1.0f;

	final protected float[][] N = new float[4][4];
	
	public void getMatrix4f( final Matrix4f matrix )
	{
		matrix.m00 = m00;
		matrix.m01 = m01;
		matrix.m02 = m02;
		matrix.m03 = 0;
		matrix.m10 = m10;
		matrix.m11 = m11;
		matrix.m12 = m12;
		matrix.m13 = 0;
		matrix.m20 = m20;
		matrix.m21 = m21;
		matrix.m22 = m22;
		matrix.m23 = 0;
		matrix.m30 = 0;
		matrix.m31 = 0;
		matrix.m32 = 0;
		matrix.m33 = 0;
	}

	public void getMatrix3f( final Matrix3f matrix )
	{
		matrix.m00 = m00;
		matrix.m01 = m01;
		matrix.m02 = m02;
		matrix.m10 = m10;
		matrix.m11 = m11;
		matrix.m12 = m12;
		matrix.m20 = m20;
		matrix.m21 = m21;
		matrix.m22 = m22;
	}

	@Override
	public boolean canDoNumDimension( final int numDimensions ) { return numDimensions == 3; }

	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 3d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );

		// calculate N
		float Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			
			final float x1 = p[ 0 ];
			final float y1 = p[ 1 ];
			final float z1 = p[ 2 ];
			final float x2 = q[ 0 ];
			final float y2 = q[ 1 ];
			final float z2 = q[ 2 ];
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}
		
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final JacobiFloat jacobi = new JacobiFloat(N);
		final float[][] eigenvectors = jacobi.getEigenVectors();
		final float[] eigenvalues = jacobi.getEigenValues();
		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final float[] q = eigenvectors[index];
		final float q0 = q[0], qx = q[1], qy = q[2], qz = q[3];

		// computational result
		
		// rotational part
		m00 = (q0 * q0 + qx * qx - qy * qy - qz * qz);
		m01 = 2 * (qx * qy - q0 * qz);
		m02 = 2 * (qx * qz + q0 * qy);
		m10 = 2 * (qy * qx + q0 * qz);
		m11 = (q0 * q0 - qx * qx + qy * qy - qz * qz);
		m12 = 2 * (qy * qz - q0 * qx);
		m20 = 2 * (qz * qx - q0 * qy);
		m21 = 2 * (qz * qy + q0 * qx);
		m22 = (q0 * q0 - qx * qx - qy * qy + qz * qz);
		
		/*
		// translational part
		result.apply(c1x, c1y, c1z);
		result.a03 = c2x - result.x;
		result.a13 = c2y - result.y;
		result.a23 = c2z - result.z;
		*/
	}
	
	@Override
	final public void set( final TranslationInvariantRigidModel3D m )
	{
		m00 = m.m00;
		m10 = m.m10;
		m20 = m.m20;
		m01 = m.m01;
		m11 = m.m11;
		m21 = m.m21;
		m02 = m.m02;
		m12 = m.m12;
		m22 = m.m22;		

		cost = m.cost;
	}

	@Override
	public TranslationInvariantRigidModel3D copy()
	{
		TranslationInvariantRigidModel3D m = new TranslationInvariantRigidModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		
		m.cost = cost;

		return m;
	}
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public float[] apply( final float[] l )
	{
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 3 : "3d affine transformations can be applied to 3d points only.";
		
		final float l0 = l[ 0 ];
		final float l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22;
	}
	
	final public String toString()
	{
		return
			"3d-affine: (" +
			m00 + ", " + m01 + ", " + m02 + ", " +
			m10 + ", " + m11 + ", " + m12 + ", " +
			m20 + ", " + m21 + ", " + m22 + ")";
	}
	
}
