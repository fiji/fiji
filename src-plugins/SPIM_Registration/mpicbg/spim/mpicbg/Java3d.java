package mpicbg.spim.mpicbg;

import javax.vecmath.Point3f;

import mpicbg.models.AffineModel3D;
import mpicbg.models.NoninvertibleModelException;

final public class Java3d
{
	final public static void applyInPlace( final AffineModel3D m, final Point3f p )
	{
		final float[] tmp = new float[ 3 ];
		
		tmp[ 0 ] = p.x;
		tmp[ 1 ] = p.y;
		tmp[ 2 ] = p.z;
		
		m.applyInPlace( tmp );
		
		p.x = tmp[ 0 ];
		p.y = tmp[ 1 ];
		p.z = tmp[ 2 ];
	}

	final public static void applyInPlace( final AffineModel3D m, final Point3f p, final float[] tmp )
	{
		tmp[ 0 ] = p.x;
		tmp[ 1 ] = p.y;
		tmp[ 2 ] = p.z;
		
		m.applyInPlace( tmp );
		
		p.x = tmp[ 0 ];
		p.y = tmp[ 1 ];
		p.z = tmp[ 2 ];
	}
	
	final public static void applyInverseInPlace( final AffineModel3D m, final Point3f p ) throws NoninvertibleModelException
	{
		final float[] tmp = new float[ 3 ];
		
		tmp[ 0 ] = p.x;
		tmp[ 1 ] = p.y;
		tmp[ 2 ] = p.z;
		
		m.applyInverseInPlace( tmp );
		
		p.x = tmp[ 0 ];
		p.y = tmp[ 1 ];
		p.z = tmp[ 2 ];
	}
	
	final public static void applyInverseInPlace( final AffineModel3D m, final Point3f p, final float[] tmp ) throws NoninvertibleModelException
	{
		tmp[ 0 ] = p.x;
		tmp[ 1 ] = p.y;
		tmp[ 2 ] = p.z;
		
		m.applyInverseInPlace( tmp );
		
		p.x = tmp[ 0 ];
		p.y = tmp[ 1 ];
		p.z = tmp[ 2 ];
	}
	
}
