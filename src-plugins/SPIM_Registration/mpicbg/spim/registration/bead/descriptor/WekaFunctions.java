package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import mpicbg.models.Point;
import mpicbg.spim.io.IOFunctions;

import weka.core.AbstractInstance;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

public class WekaFunctions
{
	public static KDTree setupKDTree(final Instances instances, final boolean normalizeDistances)
	{
		KDTree tree = new KDTree();
		try
		{
			tree.setInstances(instances);

			if (!normalizeDistances)
			{
				EuclideanDistance df = new EuclideanDistance(instances);
				df.setDontNormalize(true);
	
				tree.setDistanceFunction(df);
			}
		}
		catch (Exception e)
		{
			IOFunctions.printErr("WekaFunctions.setupKDTree(): Cannot create KDTree: " + e);
			tree = null;
		}		
		
		return tree;
	}
	
	public static void updateInstance(final Instance inst, final Point3d p, final Point3d offset)
	{
		inst.setValue(0, p.x + offset.x);
		inst.setValue(1, p.y + offset.y);
		inst.setValue(2, p.z + offset.z);
	}

	/**
	 * Create an Instance of the same type as the Instances object you are
	 * searching in.
	 * 
	 * @param p - a 3D point
	 * @param dataset - the dataset you are searching in, which was used to build the KDTree
	 * @return an Instance that the nearest neighbor can be found for
	 */
	public static Instance createInstance(final Point3f p, final Instances dataset)
	{
		// Create numeric attributes "x" and "y" and "z"
		Attribute x = dataset.attribute(0);
		Attribute y = dataset.attribute(1);
		Attribute z = dataset.attribute(2);

		// Create vector of the above attributes
		FastVector attributes = new FastVector(3);
		attributes.addElement(x);
		attributes.addElement(y);
		attributes.addElement(z);

		// Create empty instance with three attribute values
		Instance inst = new DenseInstance(3);

		// Set instance's values for the attributes "x", "y", and "z"
		inst.setValue(x, p.x);
		inst.setValue(y, p.y);
		inst.setValue(z, p.z);

		// Set instance's dataset to be the dataset "points1"
		inst.setDataset(dataset);

		return inst;
	}

	/**
	 * Creates a Weka Datastructure out of a List of 3D Points
	 * 
	 * @param points -
	 *            List of 3D Points
	 * @param name -
	 *            Instance name
	 * @return Instances containing all 3D points
	 */
	public static Instances insertIntoWeka(final List<Point3f> points, final String name)
	{
		// Create numeric attributes "x" and "y" and "z"
		Attribute x = new Attribute("x");
		Attribute y = new Attribute("y");
		Attribute z = new Attribute("z");
		
		// Create vector of the above attributes
		FastVector attributes = new FastVector(3);
		attributes.addElement(x);
		attributes.addElement(y);
		attributes.addElement(z);
		
		// Create the empty datasets "wekaPoints" with above attributes
		Instances wekaPoints = new Instances(name, attributes, 0);

		for (Iterator<Point3f> i = points.iterator(); i.hasNext();)
		{
			// Create empty instance with three attribute values
			Instance inst = new DenseInstance(3);

			// get the point3d
			Point3f p = i.next();

			// Set instance's values for the attributes "x", "y", and "z"
			inst.setValue(x, p.x);
			inst.setValue(y, p.y);
			inst.setValue(z, p.z);

			// Set instance's dataset to be the dataset "wekaPoints"
			inst.setDataset(wekaPoints);

			// Add the Instance to Instances
			wekaPoints.add(inst);
		}

		return wekaPoints;
	}

	/**
	 * Creates a Weka Datastructure out of a List of 3D Points
	 * 
	 * @param points -
	 *            List of 3D Points
	 * @param name -
	 *            Instance name
	 * @return Instances containing all 3D points
	 */
	public static <T extends Point>Instances insertIntoWekaFloat(final List<T> beads, final String name)
	{
		// Create numeric attributes "x" and "y" and "z"
		Attribute x = new Attribute("x");
		Attribute y = new Attribute("y");
		Attribute z = new Attribute("z");		

	    // Create vector to hold nominal values "first", "second", "third" 
	    FastVector my_nominal_values = new FastVector( beads.size() );
	    for ( int i = 0; i < beads.size(); i++ )
			my_nominal_values.addElement( new String( "" + i )); 
	    
	    // Create nominal attribute "position" 
	    Attribute position = new Attribute("position", my_nominal_values);	    
		
		// Create vector of the above attributes
		FastVector attributes = new FastVector( 4 );
		attributes.addElement(x);
		attributes.addElement(y);
		attributes.addElement(z);
		attributes.addElement(position);
				
		// Create the empty datasets "wekaPoints" with above attributes
		Instances wekaPoints = new Instances(name, attributes, 0);
		
	    // Make position the class attribute
		wekaPoints.setClassIndex(position.index());		

		int arrayIndex = 0;
		for (Iterator<T> i = beads.iterator(); i.hasNext();)
		{
			// get the point3d
			T bead = i.next();

			// Create empty instance with three attribute values
			Instance inst = new DenseInstance( 4 );
			
			// Set instance's values for the attributes "x", "y", and "z"
			final float[] loc = bead.getL(); 
			inst.setValue(x, loc[0]);
			inst.setValue(y, loc[1]);
			inst.setValue(z, loc[2]);			
		    inst.setValue(position, "" + arrayIndex );
		    
		    arrayIndex++;
					
			// Set instance's dataset to be the dataset "wekaPoints"
			inst.setDataset(wekaPoints);					
		
			// Add the Instance to Instances
			wekaPoints.add(inst);
		}

		return wekaPoints;
	}
	
	/**
	 * Creates a Weka Datastructure out of a List of 3D Points
	 * 
	 * @param points -
	 *            List of 3D Points
	 * @param name -
	 *            Instance name
	 * @return Instances containing all 3D points
	 */
	public static Instances insertIntoWekaFloat(final ArrayList <LocalCoordinatePointDescriptor> descriptors, final String name, final boolean normalize)
	{
		// Create numeric attributes "x" and "y" and "z"
		final Attribute bx = new Attribute("bx");
		final Attribute by = new Attribute("by");
		final Attribute cx = new Attribute("cx");
		final Attribute cy = new Attribute("cy");
		final Attribute cz = new Attribute("cz");
		
		final Attribute ax;
		
		if ( normalize )
			ax = null;
		else
			ax = new Attribute("ax");

	    // Create vector to hold nominal values "first", "second", "third" 
		final FastVector my_nominal_values = new FastVector( descriptors.size() );
	    for ( int i = 0; i < descriptors.size(); i++ )
			my_nominal_values.addElement( new String( "" + i )); 
	    
	    // Create nominal attribute "position" 
	    final Attribute position = new Attribute("position", my_nominal_values);	    
		
		// Create vector of the above attributes
	    final FastVector attributes;
	    
	    if ( ax == null)
	    {
	    	attributes = new FastVector( 6 );
	    }
	    else
	    {
	    	attributes = new FastVector( 7 );
	    	attributes.addElement( ax );
	    }
	    
		attributes.addElement(bx);
		attributes.addElement(by);
		attributes.addElement(cx);
		attributes.addElement(cy);
		attributes.addElement(cz);
		attributes.addElement(position);
		
		// Create the empty datasets "wekaPoints" with above attributes
		final Instances wekaPoints = new Instances(name, attributes, 0);

	    // Make position the class attribute
		wekaPoints.setClassIndex(position.index());		
		
		int arrayIndex = 0;
		for (Iterator<LocalCoordinatePointDescriptor> i = descriptors.iterator(); i.hasNext();)
		{
			// get the point3d
			final LocalCoordinatePointDescriptor descriptor = i.next();

			// Create empty instance with three attribute values
			final Instance inst;
			
			if ( ax == null )
			{
				inst = new DenseInstance( 6 );
			}
			else
			{
				inst = new DenseInstance( 7 );				
				inst.setValue( ax, descriptor.ax );
			}
			
			// Set instance's values for the attributes "x", "y", and "z"
			inst.setValue( bx, descriptor.bx );
			inst.setValue( by, descriptor.by );
			inst.setValue( cx, descriptor.cx );
			inst.setValue( cy, descriptor.cy );
			inst.setValue( cz, descriptor.cz );
		    inst.setValue( position, "" + arrayIndex );
		    
		    arrayIndex++;

			// Set instance's dataset to be the dataset "wekaPoints"
			inst.setDataset(wekaPoints);

			// Add the Instance to Instances
			wekaPoints.add(inst);
		}

		return wekaPoints;
	}

	/**
	 * Create an Instance of the same type as the Instances object you are
	 * searching in.
	 * 
	 * @param p - a 3D point
	 * @param dataset - the dataset you are searching in, which was used to build the KDTree
	 * @return an Instance that the nearest neighbor can be found for
	 */
	public static Instance createInstance(final LocalCoordinatePointDescriptor p, final Instances dataset, final boolean normalize)
	{
		// Create numeric attributes "x" and "y" and "z"
		final Attribute ax, bx, by, cx, cy, cz;
		
		if ( normalize )
		{
			ax = null;
			bx = dataset.attribute(0);
			by = dataset.attribute(1);
			cx = dataset.attribute(2);
			cy = dataset.attribute(3);
			cz = dataset.attribute(4);
		}
		else
		{
			ax = dataset.attribute(0);
			bx = dataset.attribute(1);
			by = dataset.attribute(2);
			cx = dataset.attribute(3);
			cy = dataset.attribute(4);
			cz = dataset.attribute(5);			
		}
		
		// Create vector of the above attributes
		final FastVector attributes;
		
		if ( ax == null )
		{
			attributes = new FastVector(5);
		}
		else
		{
			attributes = new FastVector(6);
			attributes.addElement( ax );
		}
		attributes.addElement(bx);
		attributes.addElement(by);
		attributes.addElement(cx);
		attributes.addElement(cy);
		attributes.addElement(cz);

		// Create empty instance with three attribute values
		final Instance inst;
		
		if ( ax == null )
		{
			inst = new DenseInstance(5);
		}
		else
		{
			inst = new DenseInstance(6);
			inst.setValue(ax, p.ax);
		}

		// Set instance's values for the attributes "x", "y", and "z"
		inst.setValue(bx, p.bx);
		inst.setValue(by, p.by);
		inst.setValue(cx, p.cx);
		inst.setValue(cy, p.cy);
		inst.setValue(cz, p.cz);

		// Set instance's dataset to be the dataset "points1"
		inst.setDataset(dataset);

		return inst;
	}
	
	public final static double computeDistance(final Instance p1, final Instance p2)
	{
		return Math.sqrt(Math.pow(p1.value(0) - p2.value(0), 2) + Math.pow(p1.value(1) - p2.value(1), 2) + Math.pow(p1.value(2) - p2.value(2), 2));
	}

}
