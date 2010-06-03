package mpicbg.spim.preprocessing;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import javax.vecmath.Point3d;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.models.AffineModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class AutomaticAngleSetup 
{
	public AutomaticAngleSetup( final ViewStructure viewStructure )
	{		
		final ArrayList<ViewDataBeads>views = viewStructure.getViews();
		
		final ViewDataBeads viewA = views.get( 0 ); 
		final ViewDataBeads viewB = views.get( 1 );
		
		IOFunctions.println("Using view " + viewA.getName() + " and " + viewB.getName() + " for automatic angle setup." );
		
		final Vector3f rotationAxis = extractRotationAxis( viewA.getTile().getModel(), viewB.getTile().getModel() );
		//final float rotationAngle = extractRotationAngle( viewA.getTile().getModel(), viewB.getTile().getModel(), rotationAxis );
		//IOFunctions.println( "rotation axis: " + rotationAxis + ", angle: " + rotationAngle );
				
		IOFunctions.println( "rotation axis: " + rotationAxis );		
		rotationAxis.normalize();		
		IOFunctions.println( "rotation axis normed: " + rotationAxis );
		final Vector3f xAxis = new Vector3f( new float[] { 1, 0, 0 } );
				
		IOFunctions.println( "Difference to xAxis: " + distance( rotationAxis, xAxis ) );
		//testRotationAxis();
		//getCommonAreaPerpendicularViews( viewA, viewB );
	}
	
	public static float distance( final Vector3f a, final Vector3f b )
	{
		final float dx = a.x - b.x;
		final float dy = a.y - b.y;
		final float dz = a.z - b.z;
		
		return (float) Math.sqrt( dx*dx + dy*dy + dz*dz );
	}
	
	public static void testRotationAxis()
	{
		Transform3D a = new Transform3D();
		Transform3D b = new Transform3D();

		a.setRotation( new AxisAngle4f( new Vector3f( 1, 2, 3), (float)Math.toRadians( 45 ) ) );
		b.setRotation( new AxisAngle4f( new Vector3f( 1, 2, 3), (float)Math.toRadians( 90 ) ) );
		
		Transform3D c = new Transform3D( a );
		
		c.mul( a );
		
		IOFunctions.println(c);
		IOFunctions.println(b);
				
		Vector3f roationAxis = extractRotationAxis ( MathLib.getAffineModel3D( a ), MathLib.getAffineModel3D( b )  );

		IOFunctions.println( roationAxis );				
		IOFunctions.println( extractRotationAngle( MathLib.getAffineModel3D( a ), MathLib.getAffineModel3D( b ), roationAxis) );		
		
	}

	public static float extractRotationAngle( final AffineModel3D modelA, final AffineModel3D modelB, final Vector3f rotationAxis )
	{
		final Transform3D transformA = MathLib.getTransform3D( modelA );
		final Transform3D transformB = MathLib.getTransform3D( modelB );

		// reset translational components
		transformA.setTranslation( new Vector3f() );
		transformB.setTranslation( new Vector3f() );
		
		final Transform3D connectingTransform = new Transform3D();
		final Transform3D tmp = new Transform3D();
		
		final Matrix3f matrix1 = new Matrix3f();
		final Matrix3f matrix2 = new Matrix3f();
		
		transformB.get( matrix2 );
		
		float minError = Float.MAX_VALUE;
		float minAngle = -1;
		
		for ( float angle = 0f; angle < 360.0f; angle += 1f )
		{
			connectingTransform.setIdentity();
			connectingTransform.setRotation( new AxisAngle4f( rotationAxis, (float)Math.toRadians( angle ) ) );
		
			tmp.set( transformA );
			tmp.mul( connectingTransform );
			
			tmp.get( matrix1 );
			
			matrix1.sub( matrix2 );
			
			final float diff = matrix1.m00 * matrix1.m00 + matrix1.m01 * matrix1.m01 + matrix1.m02 * matrix1.m02 +
							   matrix1.m10 * matrix1.m10 + matrix1.m11 * matrix1.m11 + matrix1.m12 * matrix1.m12 +
							   matrix1.m20 * matrix1.m20 + matrix1.m21 * matrix1.m21 + matrix1.m22 * matrix1.m22;

			IOFunctions.println( angle + " " + diff );
			
			if ( diff < minError )
			{
				minError = diff;
				minAngle = angle;
			}
		}
		
		return minAngle;
	}
	
	/**
	 * Computes the rotation axis between two affine matrices, assuming that the first affine transform is the identity transform E. 
	 * The rotation axis is just a relative ratio between x,y and z, so x is set to 1.
	 * @param model - the second affine transformation 
	 * @return - the Vector containing the rotation axis
	 */
	public static Vector3f extractRotationAxis( final AffineModel3D model )
	{
		final float[] matrix = model.getMatrix( null );
				
		final float m00 = matrix[ 0 ];
		final float m01 = matrix[ 1 ];
		final float m02 = matrix[ 2 ];
		final float m10 = matrix[ 4 ];
		final float m11 = matrix[ 5 ];
		final float m12 = matrix[ 6 ];
		final float m20 = matrix[ 8 ];
		final float m21 = matrix[ 9 ];
		final float m22 = matrix[ 10 ];
		
		final Vector3f rotationAxis = new Vector3f( 1, 0, 0 );
		
		final float x = rotationAxis.x;		
		rotationAxis.y = ( ( 1 - m00 ) * ( 1 - m22 ) * x - m20 * m02 * x ) / ( m01 * ( 1 - m22 ) + m21 * m02 );
		rotationAxis.z = ( ( 1 - m00 ) * ( 1 - m11 ) * x - m10 * m01 * x ) / ( m02 * ( 1 - m11 ) + m12 * m01 );
		
		return rotationAxis;
		
		/*
		IOFunctions.println( modelA );
		IOFunctions.println( modelB );
		
		final float[] v1 = new float[ 3 ];
		v1[ 0 ] = -1;
		v1[ 1 ] = 0;
		v1[ 2 ] = 0;
		
		final float[] v2 = v1.clone();
		
		modelA.applyInPlace( v1 );
		modelB.applyInPlace( v2 );
		
		float sum = 0;
		for ( int j = 0; j < 3; ++j )
			sum += (v1[ j ] - v2[ j ]) * (v1[ j ] - v2[ j ]);
			
		IOFunctions.println( sum );
		*/
	}

	/**
	 * Computes the rotation axis between two affine matrices. 
	 * The rotation axis is just a relative ratio between x,y and z, so x is set to 1.
	 * @param modelA - the first affine transformation 
	 * @param modelB - the second affine transformation 
	 * @return - the Vector containing the rotation axis
	 */
	public static Vector3f extractRotationAxis( final AffineModel3D modelA, final AffineModel3D modelB )
	{
		final float[] matrixA = modelA.getMatrix( null );
		final float[] matrixB = modelB.getMatrix( null );
				
		final float m00 = matrixA[ 0 ];
		final float m01 = matrixA[ 1 ];
		final float m02 = matrixA[ 2 ];
		final float m10 = matrixA[ 4 ];
		final float m11 = matrixA[ 5 ];
		final float m12 = matrixA[ 6 ];
		final float m20 = matrixA[ 8 ];
		final float m21 = matrixA[ 9 ];
		final float m22 = matrixA[ 10 ];

		final float n00 = matrixB[ 0 ];
		final float n01 = matrixB[ 1 ];
		final float n02 = matrixB[ 2 ];
		final float n10 = matrixB[ 4 ];
		final float n11 = matrixB[ 5 ];
		final float n12 = matrixB[ 6 ];
		final float n20 = matrixB[ 8 ];
		final float n21 = matrixB[ 9 ];
		final float n22 = matrixB[ 10 ];
		
		final Vector3f rotationAxis = new Vector3f( 1, 0, 0 );
		
		final float x = rotationAxis.x;		
		rotationAxis.y = ( ( m00 - n00 ) * ( m22 - n22 ) * x - ( n20 - m20 ) * ( n02 - m02 ) * x ) / 
						 ( ( n01 - m01 ) * ( m22 - n22 ) + ( n21 - m21 ) * ( n02 - m02 ) );
		rotationAxis.z = ( ( m00 - n00 ) * ( m11 - n11 ) * x - ( n10 - m10 ) * ( n01 - m01 ) * x ) / 
						 ( ( n02 - m02 ) * ( m11 - n11 ) + ( n12 - m12 ) * ( n01 - m01 ) );
		
		return rotationAxis;
		
		/*
		IOFunctions.println( modelA );
		IOFunctions.println( modelB );
		
		final float[] v1 = new float[ 3 ];
		v1[ 0 ] = -1;
		v1[ 1 ] = 0;
		v1[ 2 ] = 0;
		
		final float[] v2 = v1.clone();
		
		modelA.applyInPlace( v1 );
		modelB.applyInPlace( v2 );
		
		float sum = 0;
		for ( int j = 0; j < 3; ++j )
			sum += (v1[ j ] - v2[ j ]) * (v1[ j ] - v2[ j ]);
			
		IOFunctions.println( sum );
		*/
	}

	public static Vector3f extractRotationAxis( final Matrix3f matrixA, final Matrix3f matrixB )
	{				
		final float m00 = matrixA.m00;
		final float m01 = matrixA.m01;
		final float m02 = matrixA.m02;
		final float m10 = matrixA.m10;
		final float m11 = matrixA.m11;
		final float m12 = matrixA.m12;
		final float m20 = matrixA.m20;
		final float m21 = matrixA.m21;
		final float m22 = matrixA.m22;

		final float n00 = matrixB.m00;
		final float n01 = matrixB.m01;
		final float n02 = matrixB.m02;
		final float n10 = matrixB.m10;
		final float n11 = matrixB.m11;
		final float n12 = matrixB.m12;
		final float n20 = matrixB.m20;
		final float n21 = matrixB.m21;
		final float n22 = matrixB.m22;
		
		final Vector3f rotationAxis = new Vector3f( 1, 0, 0 );
		
		final float x = rotationAxis.x;		
		rotationAxis.y = ( ( m00 - n00 ) * ( m22 - n22 ) * x - ( n20 - m20 ) * ( n02 - m02 ) * x ) / 
						 ( ( n01 - m01 ) * ( m22 - n22 ) + ( n21 - m21 ) * ( n02 - m02 ) );
		rotationAxis.z = ( ( m00 - n00 ) * ( m11 - n11 ) * x - ( n10 - m10 ) * ( n01 - m01 ) * x ) / 
						 ( ( n02 - m02 ) * ( m11 - n11 ) + ( n12 - m12 ) * ( n01 - m01 ) );
		
		return rotationAxis;
		
		/*
		IOFunctions.println( modelA );
		IOFunctions.println( modelB );
		
		final float[] v1 = new float[ 3 ];
		v1[ 0 ] = -1;
		v1[ 1 ] = 0;
		v1[ 2 ] = 0;
		
		final float[] v2 = v1.clone();
		
		modelA.applyInPlace( v1 );
		modelB.applyInPlace( v2 );
		
		float sum = 0;
		for ( int j = 0; j < 3; ++j )
			sum += (v1[ j ] - v2[ j ]) * (v1[ j ] - v2[ j ]);
			
		IOFunctions.println( sum );
		*/
	}

	protected void getCommonAreaPerpendicularViews( final ViewDataBeads viewA, final ViewDataBeads viewB )
	{
		final float[][] minMaxDimViewA = MathLib.getMinMaxDim( viewA.getImageSize(), viewA.getTile().getModel() );
		final float[][] minMaxDimViewB = MathLib.getMinMaxDim( viewB.getImageSize(), viewB.getTile().getModel() );
		
		//final float minX = minMaxDimViewB[ 0 ][ 0 ];
		final float maxX = minMaxDimViewB[ 0 ][ 1 ];

		final float minY = minMaxDimViewB[ 1 ][ 0 ];
		final float maxY = minMaxDimViewB[ 1 ][ 1 ];

		final float minZ = minMaxDimViewA[ 2 ][ 0 ];
		final float maxZ = minMaxDimViewA[ 2 ][ 1 ];

		IOFunctions.println( "X1: " + minMaxDimViewA[ 0 ][ 0 ] + " -> " + minMaxDimViewA[ 0 ][ 1 ] );
		IOFunctions.println( "X2: " + minMaxDimViewB[ 0 ][ 0 ] + " -> " + minMaxDimViewB[ 0 ][ 1 ] );
		IOFunctions.println( "Y1: " + minMaxDimViewA[ 1 ][ 0 ] + " -> " + minMaxDimViewA[ 1 ][ 1 ] );
		IOFunctions.println( "Y2: " + minMaxDimViewB[ 1 ][ 0 ] + " -> " + minMaxDimViewB[ 1 ][ 1 ] );
		IOFunctions.println( "Z1: " + minMaxDimViewA[ 2 ][ 0 ] + " -> " + minMaxDimViewA[ 2 ][ 1 ] );
		IOFunctions.println( "Z2: " + minMaxDimViewB[ 2 ][ 0 ] + " -> " + minMaxDimViewB[ 2 ][ 1 ] );
		
		final double angle = Math.toRadians( 30 );
		
		final Point3d q = new Point3d( maxX, (maxY - minY)/2f, (maxZ - minZ)/2f );
		final Point3d r = new Point3d( 0, Math.cos( angle ), Math.sin( angle ) );
		
		final Point3d p1 = new Point3d( maxX, minY, minZ );
		final Point3d p2 = new Point3d( maxX, maxY, minZ );
		final Point3d p3 = new Point3d( maxX, minY, maxZ );
		final Point3d p4 = new Point3d( maxX, maxY, maxZ );
		
		// ebenengleichung
		
		double lambda1 = computeLambda( q, r, p1 );  
		double lambda2 = computeLambda( q, r, p2 );  
		double lambda3 = computeLambda( q, r, p3 );  
		double lambda4 = computeLambda( q, r, p4 );
		
		final Point3d fp1 = new Point3d( lambda1 * r.x + q.x, lambda1 * r.y + q.y, lambda1 * r.z + q.z );  
		final Point3d fp2 = new Point3d( lambda2 * r.x + q.x, lambda2 * r.y + q.y, lambda2 * r.z + q.z );  
		final Point3d fp3 = new Point3d( lambda3 * r.x + q.x, lambda3 * r.y + q.y, lambda3 * r.z + q.z );  
		final Point3d fp4 = new Point3d( lambda4 * r.x + q.x, lambda4 * r.y + q.y, lambda4 * r.z + q.z );
		
		final double d1 = Math.sqrt( Math.pow( p1.x - fp1.x, 2) + Math.pow( p1.y - fp1.y, 2) + Math.pow( p1.z - fp1.z, 2) ); 
		final double d2 = Math.sqrt( Math.pow( p2.x - fp2.x, 2) + Math.pow( p2.y - fp2.y, 2) + Math.pow( p2.z - fp2.z, 2) ); 
		final double d3 = Math.sqrt( Math.pow( p3.x - fp3.x, 2) + Math.pow( p3.y - fp3.y, 2) + Math.pow( p3.z - fp3.z, 2) ); 
		final double d4 = Math.sqrt( Math.pow( p4.x - fp4.x, 2) + Math.pow( p4.y - fp4.y, 2) + Math.pow( p4.z - fp4.z, 2) ); 
		
		
		IOFunctions.println( d1 + " " + d2 + " " + d3 + " " + d4 );
	}
	
	protected double computeLambda( final Point3d q, final Point3d r, final Point3d p )
	{
		return ( p.x + p.y + p.z - ( r.x*q.x + r.y*q.y + r.z*q.z ) ) / ( r.x*r.x + r.y*r.y + r.z*r.z );
	}

	public static void main( String[] args )
	{
		final SPIMConfiguration config = IOFunctions.initSPIMProcessing();
		
		//
		// load the files
		//
		final ViewStructure viewStructure = ViewStructure.initViewStructure( config, 0, new AffineModel3D(), "ViewStructure Timepoint " + 0, config.debugLevelInt );						

		for ( final ViewDataBeads view : viewStructure.getViews() )
		{
			view.loadDimensions();
			view.loadSegmentation();
			view.loadRegistration();
		}
		
		// This scaling is wrong here!
		// BeadRegistration.concatenateAxialScaling( viewStructure.getViews(), viewStructure.getDebugLevel() );		
		
		new AutomaticAngleSetup( viewStructure );
	}
}

