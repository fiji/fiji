package mpicbg.pointdescriptor.model;
/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Preibisch <preibisch@mpi-cbg.de>
 *
 */
import java.util.Collection;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.spim.vib.FastMatrix;

public class RigidModelOld3D extends AbstractAffineModel3D<RigidModelOld3D>
{
	static final protected int MIN_SET_SIZE = 3;
	
	protected Transform3D transform = new Transform3D();	
	protected Transform3D inverseTransform = new Transform3D();
	
	protected Matrix4f tmpM = new Matrix4f();
	
	final private Point3f tmp = new Point3f();
	
	public Transform3D getTransform3D(){ return transform; }
	public Transform3D getInverseTransform3D(){ return inverseTransform; }

	public void set( final Transform3D transform )
	{
		this.transform = new Transform3D( transform );
		this.inverseTransform = new Transform3D( this.transform );
		this.inverseTransform.invert();
	}
	
	@Override
	public void set( final RigidModelOld3D m )
	{		
		this.transform = new Transform3D(m.getTransform3D());
		this.inverseTransform = new Transform3D(m.getInverseTransform3D());
		this.cost = m.getCost();
	}
	
	@Override
	final public int getMinNumMatches()
	{
		return MIN_SET_SIZE;
	}

	@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 3 : "3d rigid transformations can be applied to 3d points only.";
		
		tmp.x = point[0]; 
		tmp.y = point[1]; 
		tmp.z = point[2];
		
		transform.transform(tmp);
		
		return new float[]{tmp.x, tmp.y, tmp.z};
	}
	
	@Override
	public void applyInPlace( final float[] point )
	{
		assert point.length == 3 : "3d rigid transformations can be applied to 3d points only.";
		
		tmp.x = point[0]; 
		tmp.y = point[1]; 
		tmp.z = point[2];
		
		transform.transform(tmp);
		
		point[0] = tmp.x; 
		point[1] = tmp.y; 
		point[2] = tmp.z;		
	}
	
	@Override
	public float[] applyInverse( final float[] point )
	{
		assert point.length == 3 : "3d rigid transformations can be applied to 3d points only.";

		tmp.x = point[0]; 
		tmp.y = point[1]; 
		tmp.z = point[2];
		
		inverseTransform.transform(tmp);
		
		return new float[]{tmp.x, tmp.y, tmp.z};
		
	}

	@Override
	public void applyInverseInPlace( final float[] point )
	{
		assert point.length == 3 : "3d rigid transformations can be applied to 3d points only.";

		tmp.x = point[0]; 
		tmp.y = point[1]; 
		tmp.z = point[2];
		
		inverseTransform.transform(tmp);
		
		point[0] = tmp.x; 
		point[1] = tmp.y; 
		point[2] = tmp.z;		
	}

	
	@Override
	public String toString()
	{
		return ( transform + "\nCost = " + cost );
	}

	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_SET_SIZE ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 3d rigid transformation model, at least " + MIN_SET_SIZE + " data points required." );
		
		final Point3f[] m1 = new Point3f[ matches.size() ];
		final Point3f[] m2 = new Point3f[ matches.size() ];
		final float[] w = new float[ matches.size() ];
		
		int i = 0;
		
		for ( final PointMatch m : matches )
		{
			m1[ i ] = new Point3f( m.getP1().getL()[0], m.getP1().getL()[1], m.getP1().getL()[2] );
			m2[ i ] = new Point3f( m.getP2().getW()[0], m.getP2().getW()[1], m.getP2().getW()[2] );
			w[ i ] = m.getWeight();
			++i;
		}
		
		final Matrix4f matrix;
		
		try
		{
			matrix = FastMatrix.bestRigid( m1, m2, w, false );
		} 
		catch (RuntimeException e){throw new IllDefinedDataPointsException(e.getMessage());}
		
		transform.set(matrix);
		
		inverseTransform.set(matrix);
		inverseTransform.invert();
	}

	public RigidModelOld3D copy()
	{
		RigidModelOld3D tm = new RigidModelOld3D();
		tm.transform = new Transform3D(transform);	
		tm.inverseTransform = new Transform3D(inverseTransform);	
		
		tm.cost = cost;
		return tm;
	}
	
	@Override
	public RigidModelOld3D createInverse()
	{
		RigidModelOld3D model = this.copy();
		
		model.transform.invert();
		model.inverseTransform.invert();
		
		return model;
	}
	
	@Override
	public void preConcatenate( final RigidModelOld3D model ) 
	{
		final Transform3D t = new Transform3D( model.getTransform3D() );
		t.mul( this.transform );
		
		this.transform.set( t );
		this.inverseTransform.set( transform );
		this.inverseTransform.invert();
	}
	
	@Override
	public void concatenate( final RigidModelOld3D model ) 
	{
		transform.mul( model.getTransform3D() );
		inverseTransform.set( transform );
		inverseTransform.invert();
	}
	
	@Override
	public void toArray( final float[] data ) 
	{
		final Matrix4f matrix = new Matrix4f();
		transform.get( matrix );

		data[ 0 ] = matrix.m00;
		data[ 1 ] = matrix.m10;
		data[ 2 ] = matrix.m20;
		data[ 3 ] = matrix.m01;
		data[ 4 ] = matrix.m11;
		data[ 5 ] = matrix.m21;
		data[ 6 ] = matrix.m02;
		data[ 7 ] = matrix.m12;
		data[ 8 ] = matrix.m22;
		data[ 9 ] = matrix.m03;
		data[ 10 ] = matrix.m13;
		data[ 11 ] = matrix.m23;
	}
	@Override
	public void toArray( final double[] data ) 
	{
		final Matrix4f matrix = new Matrix4f();
		transform.get( matrix );

		data[ 0 ] = matrix.m00;
		data[ 1 ] = matrix.m10;
		data[ 2 ] = matrix.m20;
		data[ 3 ] = matrix.m01;
		data[ 4 ] = matrix.m11;
		data[ 5 ] = matrix.m21;
		data[ 6 ] = matrix.m02;
		data[ 7 ] = matrix.m12;
		data[ 8 ] = matrix.m22;
		data[ 9 ] = matrix.m03;
		data[ 10 ] = matrix.m13;
		data[ 11 ] = matrix.m23;
	}
	@Override
	public void toMatrix( final float[][] data ) 
	{
		final Matrix4f matrix = new Matrix4f();
		transform.get( matrix );

		data[ 0 ][ 0 ] = matrix.m00;
		data[ 0 ][ 1 ] = matrix.m01;
		data[ 0 ][ 2 ] = matrix.m02;
		data[ 0 ][ 3 ] = matrix.m03;
		data[ 1 ][ 0 ] = matrix.m10;
		data[ 1 ][ 1 ] = matrix.m11;
		data[ 1 ][ 2 ] = matrix.m12;
		data[ 1 ][ 3 ] = matrix.m13;
		data[ 2 ][ 0 ] = matrix.m20;
		data[ 2 ][ 1 ] = matrix.m21;
		data[ 2 ][ 2 ] = matrix.m22;
		data[ 2 ][ 3 ] = matrix.m23;
		
	}
	
	@Override
	public void toMatrix( final double[][] data ) 
	{
		final Matrix4f matrix = new Matrix4f();
		transform.get( matrix );

		data[ 0 ][ 0 ] = matrix.m00;
		data[ 0 ][ 1 ] = matrix.m01;
		data[ 0 ][ 2 ] = matrix.m02;
		data[ 0 ][ 3 ] = matrix.m03;
		data[ 1 ][ 0 ] = matrix.m10;
		data[ 1 ][ 1 ] = matrix.m11;
		data[ 1 ][ 2 ] = matrix.m12;
		data[ 1 ][ 3 ] = matrix.m13;
		data[ 2 ][ 0 ] = matrix.m20;
		data[ 2 ][ 1 ] = matrix.m21;
		data[ 2 ][ 2 ] = matrix.m22;
		data[ 2 ][ 3 ] = matrix.m23;
	}
	
	@Override
	public float[] getMatrix( final float[] m ) 
	{
		final float[] a;
		if ( m == null || m.length != 12 )
			a = new float[ 12 ];
		else
			a = m;
		
		final Matrix4f matrix = new Matrix4f();
		transform.get( matrix );
		
		a[ 0 ] = matrix.m00;
		a[ 1 ] = matrix.m01;
		a[ 2 ] = matrix.m02;
		a[ 3 ] = matrix.m03;
		
		a[ 4 ] = matrix.m10;
		a[ 5 ] = matrix.m11;
		a[ 6 ] = matrix.m12;
		a[ 7 ] = matrix.m13;
		
		a[ 8 ] = matrix.m20;
		a[ 9 ] = matrix.m21;
		a[ 10 ] = matrix.m22;
		a[ 11 ] = matrix.m23;
		
		return a;
	}	
}
