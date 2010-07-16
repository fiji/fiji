package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

public class LocalCoordinatePointDescriptor extends PointDescriptor
{
	final protected boolean normalize;
	public float ax, bx, by, cx, cy, cz;
	public float weight = 1;
	
	/**
	 * This static method creates a number of BestRigidPointDescriptors based on the tolerance term
	 * 
	 * @param tree - The KDTree to construct the PointDescriptors from
	 * @param pos - The position at which the PointDescriptor is created
	 * @param numNeighbors - The number of neighbors for this point
	 * @param tolerance - How many number of Points tolerance
	 * @param firstNeighborIndex - The first neighbor to use (0 terminated), the 0 neighbor is usually the point itself
	 * @return - A List of PointDescriptors
	 */
	public static final ArrayList <LocalCoordinatePointDescriptor> PointDescriptorFactory(final KDTree tree, final Bead bead, final ViewDataBeads view, final int numNeighbors, 
	                                                                       final int tolerance, final int firstNeighborIndex, final boolean normalize)
	{
		final ArrayList <LocalCoordinatePointDescriptor> pointDescriptors = new ArrayList<LocalCoordinatePointDescriptor>();
		
		final int[][] neighbors = computePDRecursive(tolerance, numNeighbors, firstNeighborIndex);
		
		int id = index.getAndIncrement();
					
		for (int i = 0; i < neighbors.length; i++)		
			pointDescriptors.add( new LocalCoordinatePointDescriptor(tree, bead, view, neighbors[i], tree.getInstances().relationName() + "_" + id + "_" + (i+1), id, normalize) );
				
		return pointDescriptors;
	}
	
  	/**
  	 * Constructs the PointDescriptor for a certain Point3d
  	 * @param pos - The Position of a descriptor
  	 * @param neighborIndices - which neighbors to use 
  	 * @param id - The id of this {@link PointDescriptor}
  	 */
	LocalCoordinatePointDescriptor(KDTree tree, Bead bead, ViewDataBeads view, int[] neighborIndices, String id, int describedPoint, boolean normalize)
	{
		super(tree, bead, view, neighborIndices, id, describedPoint);

		this.normalize = normalize;
		
		buildLocalCoordinateSystem( neighbors, normalize );
	}
	
	public void buildLocalCoordinateSystem( final Vector3f[] neighbors, final boolean normalize )
	{
		// most distant point		
		final Vector3f b = neighbors[ 0 ];
		final Vector3f c = neighbors[ 1 ];
		final Vector3f d = neighbors[ 2 ];
		
		final Vector3f x = new Vector3f( d );
		x.normalize();			

		/*IOFunctions.println( "Input" );
		IOFunctions.println( b );
		IOFunctions.println( c );
		IOFunctions.println( d );*/
		
		if ( normalize )
		{			
			final float lengthD = 1.0f / d.length();

			b.scale(lengthD);
			c.scale(lengthD);
			d.scale(lengthD);
				
			/*IOFunctions.println( "Scaled" );
			IOFunctions.println( b + "(" + b.length() + ")");
			IOFunctions.println( c + "(" + c.length() + ")");
			IOFunctions.println( d + "(" + d.length() + ")");*/
		}
		else
		{
			ax = d.length();
		}
		
		// get normal vector of ab and ad ( which will be the z-axis)
		final Vector3f n = new Vector3f();
		n.cross(b, x);		
		n.normalize();
		
		/*IOFunctions.println( "Normal vector (z-axis)" );
		IOFunctions.println( n );*/
		
		// check if the normal vector points into the direction of point c
		if ( n.dot( c ) < 0 )
		{
			n.negate();
			/*IOFunctions.println( "Negated normal vector (z-axis)" );
			IOFunctions.println( n );*/
		}
		
		// get the inverse of the matrix that maps the vectors into the local coordinate system
		// where the x-axis is vector(ad), the z-axis is n and the y-axis is cross-product(x,z)
		final Vector3f y = new Vector3f();
		y.cross( n, x );
		y.normalize();

		/*
		IOFunctions.println( "X - axis" );		
		IOFunctions.println( x );

		IOFunctions.println( "Y - axis" );
		IOFunctions.println( y );
		*/
		
		final Matrix3f m = new Matrix3f();
		m.m00 = x.x; m.m01 = y.x; m.m02 = n.x;  
		m.m10 = x.y; m.m11 = y.y; m.m12 = n.y; 
		m.m20 = x.z; m.m21 = y.z; m.m22 = n.z;
		
		try
		{
			m.invert();
		}
		catch ( Exception e )
		{
			bx = by = cx = cy = cz = 0;
			return;
		}
		
		
		// get the positions in the local coordinate system
		final Vector3f bl = new Vector3f( b );
		final Vector3f cl = new Vector3f( c );
		//final Vector3f dl = new Vector3f( d );

		m.transform( bl );
		m.transform( cl );
		//m.transform( dl );
		
		/*
		IOFunctions.println( "In local coordinate system" );
		IOFunctions.println( bl );
		IOFunctions.println( cl );
		IOFunctions.println( dl );
		*/
		
		bx = bl.x;
		by = bl.y;
		cx = cl.x;
		cy = cl.y;
		cz = cl.z;
	}
	
	public LocalCoordinatePointDescriptor getMatch( final KDTree lookupKDTree, final ArrayList <LocalCoordinatePointDescriptor> descriptors, 
			 										final double differenceThreshold, final double ratioOfDistance )
	{
  		// create a Weka instance of Point3d
  		Instance inst = WekaFunctions.createInstance( this, lookupKDTree.getInstances(), normalize);  		  	
  		
  		// here goes the result ( best and second best )
  		Instance[] nn = new Instance[ 2 ];
		
  		try
		{
			Instances tmp = lookupKDTree.kNearestNeighbours(inst, 2 );
			nn[ 0 ] = tmp.instance( 0 );
			nn[ 1 ] = tmp.instance( 1 );
		}
		catch (Exception e)
		{
			IOFunctions.printErr("LocalCoordinatePointDescriptor.getMatch(): " + e);
			return null;
		}
		
		final LocalCoordinatePointDescriptor pd1, pd2;
		
		if ( normalize )
		{
			pd1 = descriptors.get( Integer.parseInt( nn[0].stringValue(5) ) );
			pd2 = descriptors.get( Integer.parseInt( nn[1].stringValue(5) ) );
		}
		else
		{
			pd1 = descriptors.get( Integer.parseInt( nn[0].stringValue(6) ) );
			pd2 = descriptors.get( Integer.parseInt( nn[1].stringValue(6) ) );			
		}
		
		final double distance1 = pd1.getDifference( this );
		
		if ( distance1 > differenceThreshold )
			return null;
		
		final double distance2 = pd2.getDifference( this );
		
		if ( normalize )
		{
			if ( distance1 * ratioOfDistance*0.9 > distance2 )
				return null;
		}
		else
		{
			if ( distance1 * ratioOfDistance > distance2 )
				return null;			
		}
		
		weight = 1.0f/(float)distance1;

		return pd1;
	}

	@Override
	public double getDifference( PointDescriptor pd2 )
	{
		LocalCoordinatePointDescriptor lpd2 = (LocalCoordinatePointDescriptor)pd2;
		double difference = 0;
		
		if ( !normalize )
			difference += ( ax - lpd2.ax ) * ( ax - lpd2.ax );  	
		
		difference += ( bx - lpd2.bx ) * ( bx - lpd2.bx );  
		difference += ( by - lpd2.by ) * ( by - lpd2.by );  
		difference += ( cx - lpd2.cx ) * ( cx - lpd2.cx );  
		difference += ( cy - lpd2.cy ) * ( cy - lpd2.cy );  
		difference += ( cz - lpd2.cz ) * ( cz - lpd2.cz );  
		
		return difference / (double)neighbors.length;
	}

	@Override
	public double getLengthNormalizedDifference(PointDescriptor pd2)
	{
		return getDifference(pd2);
	}

}
