package mpicbg.pointdescriptor.test;

import fiji.util.KDTree;
import fiji.util.NNearestNeighborSearch;
import ij3d.Content;
import ij3d.Image3DUniverse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import mpicbg.imglib.util.Util;
import mpicbg.models.AffineModel3D;
import mpicbg.models.Point;
import mpicbg.pointdescriptor.LinkedPoint;
import mpicbg.pointdescriptor.LocalCoordinateSystemPointDescriptor;
import mpicbg.pointdescriptor.ModelPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.Matcher;
import mpicbg.pointdescriptor.matcher.SimpleMatcher;
import mpicbg.pointdescriptor.model.TranslationInvariantModel;
import mpicbg.pointdescriptor.model.TranslationInvariantRigidModel3D;
import mpicbg.pointdescriptor.similarity.SimilarityMeasure;
import mpicbg.pointdescriptor.similarity.SquareDistance;
import mpicbg.spim.vis3d.VisualizationFunctions;
import mpicbg.spim.vis3d.VisualizeBeads;
import mpicbg.util.TransformUtils;
import customnode.CustomLineMesh;

public class TestPointDescriptor
{
	protected static void add( final Point p1, final Point p2 )
	{
		final float[] l1 = p1.getL();
		final float[] w1 = p1.getW();
		final float[] l2 = p2.getL();
		final float[] w2 = p2.getW();
		
		for ( int d = 0; d < l1.length; ++d )
		{
			l1[ d ] += l2[ d ];
			w1[ d ] += w2[ d ];
		}
	}
	
	protected void addSimplePoints( final ArrayList<Point> points1, final ArrayList<Point> points2 )
	{
		points1.add( new Point( new float[]{ 0, 0, 0 } ) );
		points1.add( new Point( new float[]{ 0, 0, 1.1f } ) );
		points1.add( new Point( new float[]{ 0, 1.2f, 0 } ) );
		points1.add( new Point( new float[]{ 1.3f, 0, 0 } ) );
		points1.add( new Point( new float[]{ 1.3f, 1.4f, 0 } ) );

		final Point offset = new Point( new float[]{ 1, 2, 3 } );
		
		for ( final Iterator<Point> i = points1.iterator(); i.hasNext(); )
		{
			final Point p2 = new Point( i.next().getL().clone() );
			add( p2, offset );
			points2.add( p2 );
		}

		points1.add( new Point( new float[]{ 0.1f, 0.1f ,0.1f } ) );		
	}
	
	protected void addAdvancedPoints( final ArrayList<Point> points1, final ArrayList<Point> points2 )
	{
		final int commonPoints = 10;
		final int randomPoints = 100;
		
		final int offsetX = 5;
		final int offsetY = -10;
		final int offsetZ = 7;
		
		Random rnd = new Random(325235325L);

		for ( int i = 0; i < commonPoints; ++i )
		{
			// all between 5 and 10
			float v1 = rnd.nextFloat()*5 + 5;
			float v2 = rnd.nextFloat()*5 + 5;
			float v3 = rnd.nextFloat()*5 + 5;
			float o1 = (rnd.nextFloat()-0.5f)/10;
			float o2 = (rnd.nextFloat()-0.5f)/10;
			float o3 = (rnd.nextFloat()-0.5f)/10;
						
			final Point p1 = new Point( new float[]{ v1 + o1, v2 + o2, v3 + o3 } );
			final Point p2 = new Point( new float[]{ v1 + offsetX, v2 + offsetY, v3 + offsetZ } );
			
			points1.add( p1 );
			points2.add( p2 );	
		}
		
		for ( int i = 0; i < randomPoints; ++i )		
		{
			float v1 = rnd.nextFloat()*90;
			float v2 = rnd.nextFloat()*90;
			float v3 = rnd.nextFloat()*90;

			final Point p1 = new Point( new float[]{ v1, v2, v3 } );

			v1 = rnd.nextFloat()*90;
			v2 = rnd.nextFloat()*90;
			v3 = rnd.nextFloat()*90;

			final Point p2 = new Point( new float[]{ v1, v2, v3 } );

			points1.add( p1 );
			points2.add( p2 );				
		}

		for ( int i = 0; i < randomPoints/10; ++i )		
		{
			float v1 = rnd.nextFloat()*5;
			float v2 = rnd.nextFloat()*5;
			float v3 = rnd.nextFloat()*5;

			final Point p1 = new Point( new float[]{ v1, v2, v3 } );

			v1 = rnd.nextFloat()*5;
			v2 = rnd.nextFloat()*5;
			v3 = rnd.nextFloat()*5;

			final Point p2 = new Point( new float[]{ v1, v2, v3 } );

			points1.add( p1 );
			points2.add( p2 );				
		}		
	}
	
	protected void applyTransform( final ArrayList<Point> points )
	{
        final Transform3D trans = new Transform3D();
        trans.rotX( Math.toRadians( 30 ) );
        
        final AffineModel3D model = TransformUtils.getAffineModel3D( trans );
        
        for ( final Point p : points )
        {
        	model.apply( p.getL() );
        	model.apply( p.getW() );
        }        			
	}
	
	
	public static <P extends Point> ArrayList< VirtualPointNode< P > > createVirtualNodeList( final ArrayList<P> points )
	{
		final ArrayList< VirtualPointNode< P > > nodeList = new ArrayList< VirtualPointNode< P > >();
		
		for ( final P point : points )
			nodeList.add( new VirtualPointNode<P>( point ) );
		
		return nodeList;
	}
	
	public static < P extends Point > ArrayList< ModelPointDescriptor< P > > createModelPointDescriptors( final KDTree< VirtualPointNode< P > > tree, 
	                                                                                               final ArrayList< VirtualPointNode< P > > basisPoints, 
	                                                                                               final int numNeighbors, 
	                                                                                               final TranslationInvariantModel<?> model, 
	                                                                                               final Matcher matcher, 
	                                                                                               final SimilarityMeasure similarityMeasure )
	{
		final NNearestNeighborSearch< VirtualPointNode< P > > nnsearch = new NNearestNeighborSearch< VirtualPointNode< P > >( tree );
		final ArrayList< ModelPointDescriptor< P > > descriptors = new ArrayList< ModelPointDescriptor< P > > ( );
		
		for ( final VirtualPointNode< P > p : basisPoints )
		{
			final ArrayList< P > neighbors = new ArrayList< P >();
			final VirtualPointNode< P > neighborList[] = nnsearch.findNNearestNeighbors( p, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
				neighbors.add( neighborList[ n ].getPoint() );
			
			try
			{
				descriptors.add( new ModelPointDescriptor<P>( p.getPoint(), neighbors, model, similarityMeasure, matcher ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
			
		return descriptors;
	}

	public static < P extends Point > ArrayList< LocalCoordinateSystemPointDescriptor< P > > createLocalCoordinateSystemPointDescriptors( 
			final KDTree< VirtualPointNode< P > > tree, 
            final ArrayList< VirtualPointNode< P > > basisPoints, 
            final int numNeighbors,
            final boolean normalize )
	{
		final NNearestNeighborSearch< VirtualPointNode< P > > nnsearch = new NNearestNeighborSearch< VirtualPointNode< P > >( tree );
		final ArrayList< LocalCoordinateSystemPointDescriptor< P > > descriptors = new ArrayList< LocalCoordinateSystemPointDescriptor< P > > ( );
		
		for ( final VirtualPointNode< P > p : basisPoints )
		{
			final ArrayList< P > neighbors = new ArrayList< P >();
			final VirtualPointNode< P > neighborList[] = nnsearch.findNNearestNeighbors( p, numNeighbors + 1 );
			
			// the first hit is always the point itself
			for ( int n = 1; n < neighborList.length; ++n )
			neighbors.add( neighborList[ n ].getPoint() );
			
			try
			{
				descriptors.add( new LocalCoordinateSystemPointDescriptor<P>( p.getPoint(), neighbors, normalize ) );
			}
			catch ( NoSuitablePointsException e )
			{
				e.printStackTrace();
			}
		}
		
		return descriptors;
	}
	
	public TestPointDescriptor()
	{
		final ArrayList<Point> points1 = new ArrayList<Point>();
		final ArrayList<Point> points2 = new ArrayList<Point>();
		
		/* add some corresponding points */
		addSimplePoints( points1, points2 );
		//addAdvancedPoints( points1, points2 );
		
		/* rotate one of the pointclouds */
		applyTransform( points2 );
		
		/* create KDTrees */
		final ArrayList< VirtualPointNode< Point > > nodeList1 = createVirtualNodeList( points1 );
		final ArrayList< VirtualPointNode< Point > > nodeList2 = createVirtualNodeList( points2 );
		
		final KDTree< VirtualPointNode< Point > > tree1 = new KDTree< VirtualPointNode< Point > >( nodeList1 );
		final KDTree< VirtualPointNode< Point > > tree2 = new KDTree< VirtualPointNode< Point > >( nodeList2 );
		
		/* extract point descriptors */						
		final int numNeighbors = 4;
		final TranslationInvariantModel<?> model = new TranslationInvariantRigidModel3D();
		final Matcher matcher = new SimpleMatcher( numNeighbors );
		final SimilarityMeasure similarityMeasure = new SquareDistance();
				
		final ArrayList< ModelPointDescriptor< Point > > descriptors1 = 
			createModelPointDescriptors( tree1, nodeList1, numNeighbors, model, matcher, similarityMeasure );
		
		final ArrayList< ModelPointDescriptor< Point > > descriptors2 = 
			createModelPointDescriptors( tree2, nodeList2, numNeighbors, model, matcher, similarityMeasure );
		
		/* compute matching */
		for ( final ModelPointDescriptor< Point > descriptorA : descriptors1 )
		{
			for ( final ModelPointDescriptor< Point > descriptorB : descriptors2 )
			{
				final double difference = descriptorA.descriptorDistance( descriptorB );
				
				//if ( difference < 0.1 )
				{
					System.out.println( "Difference " + descriptorA.getId() + " -> " + descriptorB.getId() + " : " + difference );
					System.out.println( "Position " + Util.printCoordinates( descriptorA.getBasisPoint().getL() ) + " -> " + 
					                    Util.printCoordinates( descriptorB.getBasisPoint().getL() ) + "\n" );
					
				}
			}
		}				
	}
	
	public static void testQuaternions()
	{
        final Quat4f qu = new Quat4f();
        final Matrix3f m = new Matrix3f();
        
        final Transform3D transformationPrior = new Transform3D();
        transformationPrior.rotX( Math.toRadians( 45 ) );

        transformationPrior.get( m );        
        qu.set( m );       
        Vector3f v1 = new Vector3f( qu.getX(), qu.getY(), qu.getZ() );
        v1.normalize();
        System.out.println( "Axis: " + v1  );      
        System.out.println( Math.toDegrees( Math.acos( qu.getW() ) * 2 ) );
        
        final Transform3D trans = new Transform3D();
        trans.rotY( Math.toRadians( 90 ) );
        
        trans.get( m );        
        qu.set( m );       
        Vector3f v2 = new Vector3f( qu.getX(), qu.getY(), qu.getZ() );        
        v2.normalize();
        System.out.println( "Axis: " + v2  );      
        System.out.println( Math.toDegrees( Math.acos( qu.getW() ) * 2 ) );
        
        Vector3f v3 = new Vector3f( 1.1f, 0.1f, 0.25f );
        v3.normalize();
        
       	Point3f p1 = new Point3f( 1, 0, 0 );
       	Point3f p2 = new Point3f( v3 );
              	
       	System.out.println( "Distance to: " + Math.pow( 50, p1.distance( p2 ) ) );
 
        transformationPrior.invert();        
        trans.mul( transformationPrior );                
        
        trans.get( m );        
        qu.set( m );        
        System.out.println( Math.toDegrees( Math.acos( qu.getW() ) * 2 ) );
 	}
	
	public static void testStability( final int numNeighbors, final int numTrueMatches, final int numRandomPoints, final double nTimesBetter, final float stdev, boolean fastMatching, boolean showPoints )
	{
		final ArrayList<Point> truepoints1 = new ArrayList<Point>();
		final ArrayList<Point> falsepoints1 = new ArrayList<Point>();

		final ArrayList<Point> points1 = new ArrayList<Point>();
		final ArrayList<Point> points2 = new ArrayList<Point>();
		
		final Random rnd = new Random( 4353451 );
		
		// ensure contstant points per volume
		final double cubeSize = 326508; //2 * 2 * 2;
		final double pointsPercubePixel = 1;
		
		//
		// Add point descriptors that are easy to find
		//
		final double cubeSizeTrue = ( cubeSize / pointsPercubePixel ) * numTrueMatches;
		final float cubeTrueKantenLength = (float)Math.pow( cubeSizeTrue, 1.0/3.0 );
		
		Point offset = new Point( new float[]{ -cubeTrueKantenLength/2, -cubeTrueKantenLength/2, -cubeTrueKantenLength/2 } );
		//Point offset = new Point( new float[]{ 1.5f*cubeTrueKantenLength, 1.5f*cubeTrueKantenLength, 1.5f*cubeTrueKantenLength } );
		
		for ( int n = 0; n < numTrueMatches; ++n )
		{
			final Point p = new Point( new float[]{ rnd.nextFloat()*cubeTrueKantenLength, rnd.nextFloat()*cubeTrueKantenLength, rnd.nextFloat()*cubeTrueKantenLength } );
			add( p, offset );
			points1.add( p );
			truepoints1.add( p );
			
			final LinkedPoint<Point> p2 = new LinkedPoint<Point>( p.getL().clone(), p );
			
			p2.getL()[ 0 ] += stdev * (float)rnd.nextGaussian();
			p2.getL()[ 1 ] += stdev * (float)rnd.nextGaussian();
			p2.getL()[ 2 ] += 3 * stdev * (float)rnd.nextGaussian();
			points2.add( p2 );
		}
		
		//
		// Add Random Points around the true one's
		//
		final double cubeSizeFalse = ( cubeSize / pointsPercubePixel ) * numRandomPoints + cubeSizeTrue;
		//final double cubeSizeFalse = ( cubeSize / pointsPercubePixel ) * numRandomPoints;
		final float cubeFalseKantenLength = (float)Math.pow( cubeSizeFalse, 1.0/3.0 );
		final float o = -cubeFalseKantenLength/2;
		//final float o = -cubeFalseKantenLength;

		for ( int n = 0; n < numRandomPoints; ++n )
		{
			float l[][] = new float[ 2 ][ 3 ];
			
			for ( int i = 0; i < l.length; ++i )
			{
				float[] li = l[ i ];
				boolean worked = true;
				do
				{
					worked = true;
					li[0] = rnd.nextFloat()*cubeFalseKantenLength + o;
					li[1] = rnd.nextFloat()*cubeFalseKantenLength + o;
					li[2] = rnd.nextFloat()*cubeFalseKantenLength + o;
					
					if ( ( li[0] >= -cubeTrueKantenLength/2 && li[0] < cubeTrueKantenLength/2 ) &&
						 ( li[1] >= -cubeTrueKantenLength/2 && li[1] < cubeTrueKantenLength/2 ) &&
						 ( li[2] >= -cubeTrueKantenLength/2 && li[2] < cubeTrueKantenLength/2 ) ) 
						worked = false;
				}
				while ( !worked );
			}
			
			final Point p1 = new Point( new float[]{ l[ 0 ][ 0 ], l[ 0 ][ 1 ], l[ 0 ][ 2 ] } );
			final Point p2 = new Point( new float[]{ l[ 1 ][ 0 ], l[ 1 ][ 1 ], l[ 1 ][ 2 ] } );
			points1.add( p1 );
			falsepoints1.add( p1 );
			points2.add( p2 );
		}
		
		if ( showPoints )
		{
			Image3DUniverse univ = VisualizeBeads.initUniverse();
			//final Image3DUniverse univ = new Image3DUniverse( 800, 600 );
			
			Color3f colorTrue = new Color3f( 38f/255f, 140f/255f, 0.1f );
			Color3f colorFalse = new Color3f( 1f, 0.1f, 0.1f );
			float size = .1f;
						
			CustomLineMesh c = new CustomLineMesh( getBoundingBox( -cubeFalseKantenLength/2, cubeFalseKantenLength/2 ), CustomLineMesh.PAIRWISE );
			c.setLineWidth( 2 );
			c.setColor( colorFalse );
			final Content content = univ.addCustomMesh( c, "BoundingBoxFalse" );			
			content.showCoordinateSystem(false);			

			CustomLineMesh c2 = new CustomLineMesh( getBoundingBox( -cubeTrueKantenLength/2, cubeTrueKantenLength/2 ), CustomLineMesh.PAIRWISE );
			//CustomLineMesh c2 = new CustomLineMesh( getBoundingBox( 1.5f*cubeTrueKantenLength, 2.5f*cubeTrueKantenLength ), CustomLineMesh.PAIRWISE );
			c2.setLineWidth( 2 );
			c2.setColor( colorTrue );
			final Content content2 = univ.addCustomMesh( c2, "BoundingBoxTrue" );			
			content2.showCoordinateSystem(false);			

			VisualizationFunctions.drawPoints( univ, truepoints1, new Transform3D(), colorTrue, size+0.05f, 0.1f );
			VisualizationFunctions.drawPoints( univ, falsepoints1, new Transform3D(), colorFalse, size, 0.3f );
			
			//univ.show();
			return;
		}
		
		final long time = System.currentTimeMillis();
		
		/* create KDTrees */
		final ArrayList< VirtualPointNode< Point > > nodeList1 = createVirtualNodeList( points1 );
		final ArrayList< VirtualPointNode< Point > > nodeList2 = createVirtualNodeList( points2 );
		
		final KDTree< VirtualPointNode< Point > > tree1 = new KDTree< VirtualPointNode< Point > >( nodeList1 );
		final KDTree< VirtualPointNode< Point > > tree2 = new KDTree< VirtualPointNode< Point > >( nodeList2 );
	
		int detectedRight = 0;
		int detectedWrong = 0;
		
		final boolean foundByNeighbor[] = new boolean[ numTrueMatches ];
		
		for ( int i = 0; i < numTrueMatches; ++i )
			foundByNeighbor[ i ] = false;
		
		if ( fastMatching )
		{
			final ArrayList< LocalCoordinateSystemPointDescriptor< Point > > descriptors1 = 
				createLocalCoordinateSystemPointDescriptors( tree1, nodeList1, numNeighbors, false );
			
			final ArrayList< LocalCoordinateSystemPointDescriptor< Point > > descriptors2 = 
				createLocalCoordinateSystemPointDescriptors( tree2, nodeList2, numNeighbors, false );
			
			// create lookup tree for descriptors2
			final KDTree< LocalCoordinateSystemPointDescriptor< Point > > lookUpTree2 = new KDTree< LocalCoordinateSystemPointDescriptor< Point > >( descriptors2 );
			final NNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< Point > > nnsearch = new NNearestNeighborSearch< LocalCoordinateSystemPointDescriptor< Point > >( lookUpTree2 );
		
			/* compute matching */
			for ( final LocalCoordinateSystemPointDescriptor< Point > descriptorA : descriptors1 )
			{
				LocalCoordinateSystemPointDescriptor< Point > matches[] = nnsearch.findNNearestNeighbors( descriptorA, 2 );

				double best = descriptorA.descriptorDistance( matches[ 0 ]);
				double secondBest = descriptorA.descriptorDistance( matches[ 1 ]);

				if ( best * nTimesBetter < secondBest )
				{
					if ( isCorrect( descriptorA.getBasisPoint(), matches[ 0 ].getBasisPoint() ) )
					{
						++detectedRight;
						ArrayList< Point > neighbors = descriptorA.getOrderedNearestNeighboringPoints();
						
						for ( Point p : neighbors )
							for ( int i = 0; i < numTrueMatches; ++i )
								if ( isCorrect(p, points1.get( i )) )
									foundByNeighbor[ i ] = true;
					}
					else
						++detectedWrong;
				}				
			}			
			
		}
		else
		{
			/* extract point descriptors */						
			final TranslationInvariantModel<?> model = new TranslationInvariantRigidModel3D();
			final Matcher matcher = new SimpleMatcher( numNeighbors );
			//final Matcher matcher = new SubsetMatcher( numNeighbors, numNeighbors+2 ); 
			final SimilarityMeasure similarityMeasure = new SquareDistance();
					
			final ArrayList< ModelPointDescriptor< Point > > descriptors1 = 
				createModelPointDescriptors( tree1, nodeList1, numNeighbors, model, matcher, similarityMeasure );
			
			final ArrayList< ModelPointDescriptor< Point > > descriptors2 = 
				createModelPointDescriptors( tree2, nodeList2, numNeighbors, model, matcher, similarityMeasure );
		
			/* compute matching */
			for ( final ModelPointDescriptor< Point > descriptorA : descriptors1 )
			{
				double best = Double.MAX_VALUE;
				double secondBest = Double.MAX_VALUE;
				
				boolean correct = true;
				
				for ( final ModelPointDescriptor< Point > descriptorB : descriptors2 )
				{
					final double difference = descriptorA.descriptorDistance( descriptorB );
									
					if ( difference < secondBest )
					{
						if ( difference < best )
						{
							secondBest = best;
							best = difference;
							
							correct = isCorrect( descriptorA.getBasisPoint(), descriptorB.getBasisPoint() );
						}
						else
						{
							secondBest = difference;
						}
					}
				}
				
				if ( best * nTimesBetter < secondBest )
				{
					if ( correct )
					{
						++detectedRight;
						ArrayList< Point > neighbors = descriptorA.getOrderedNearestNeighboringPoints();
						
						for ( Point p : neighbors )
							for ( int i = 0; i < numTrueMatches; ++i )
								if ( isCorrect(p, points1.get( i )) )
									foundByNeighbor[ i ] = true;
					}
					else
						++detectedWrong;
				}
			}		
		}
		
		final long duration = System.currentTimeMillis() - time;
		
		int countAll = 0;

		for ( int i = 0; i < numTrueMatches; ++i )
			if ( foundByNeighbor[ i ] )
				++countAll;

		System.out.println( numNeighbors + "\t" + numRandomPoints + "\t" + detectedRight + "\t" + countAll + "\t" + detectedWrong + "\t" + duration );
	}

	public static ArrayList<Point3f> getBoundingBox( final float start, final float end )
	{
		final ArrayList<Point3f> boundingBox = new ArrayList<Point3f>();
		
		boundingBox.add( new Point3f(start,start,start) );
		boundingBox.add( new Point3f(end, start, start) );
		
		boundingBox.add( new Point3f(end, start, start) );
		boundingBox.add( new Point3f(end, end, start) );

		boundingBox.add( new Point3f(end, end, start) );
		boundingBox.add( new Point3f(start, end, start) );
		
		boundingBox.add( new Point3f(start, end, start) );
		boundingBox.add( new Point3f(start, start, start) );

		boundingBox.add( new Point3f(start, start, end) );
		boundingBox.add( new Point3f(end, start, end) );

		boundingBox.add( new Point3f(end, start,  end) );
		boundingBox.add( new Point3f(end, end,  end) );

		boundingBox.add( new Point3f(end, end,  end) );
		boundingBox.add( new Point3f(start, end, end) );

		boundingBox.add( new Point3f(start, end,  end) );
		boundingBox.add( new Point3f(start, start, end) );

		boundingBox.add( new Point3f(start, start, start) );
		boundingBox.add( new Point3f(start, start, end) );

		boundingBox.add( new Point3f(end, start, start) );
		boundingBox.add( new Point3f(end, start, end) );

		boundingBox.add( new Point3f(end, end, start) );
		boundingBox.add( new Point3f(end, end, end) );

		boundingBox.add( new Point3f(start, end, start) );
		boundingBox.add( new Point3f(start, end, end) );
		
		return boundingBox;
	}
	
	protected static boolean isCorrect( Point a, Point b )
	{
		if ( a instanceof LinkedPoint )
		{
			if ( ((LinkedPoint<Point>)a).getLinkedObject() == b  )
				return true;
			else
				return false;
		}
		else if ( b instanceof LinkedPoint )
		{
			if ( ((LinkedPoint<Point>)b).getLinkedObject() == a  )
				return true;
			else
				return false;
		}
		else
		{
			return false;
		}
		
		/*
		float[] a1 = a.getL();
		float[] b1 = b.getL();
		
		if ( a1[ 0 ] == b1[ 0 ] && a1[ 1 ] == b1[ 1 ] && a1[ 2 ] == b1[ 2 ] )
			return true;
		else
			return false;
		*/
	}
	
	public static void main( String args[] )
	{
		boolean showPoints = false;
		
		if ( showPoints )
		{
			final String params[] = { "-ijpath ." };
			ij.ImageJ.main( params );
		}
		
		//testQuaternions();
		//new TestPointDescriptor();
		
		float stdev = 0.2f;
		
		//testStability( 3, 100, 0, 10.0, stdev, true, showPoints );
						
		for ( int n = 2; n <= 10000000; n *= 1.5 )
			testStability( 3, 100, n, 10.0, stdev, false, showPoints );
		
		//for ( int neighbors = 3; neighbors <= 8; ++neighbors )
		//	for ( int n = 1; n <= 10000; n *= 10 )
		//		testStability( neighbors, 100, n, 10.0, false, showPoints );
	}
}
