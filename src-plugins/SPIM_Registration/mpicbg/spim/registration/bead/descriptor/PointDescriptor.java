package mpicbg.spim.registration.bead.descriptor;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;
//import edu.bucknell.util.KDTree;

public abstract class PointDescriptor
{	
	/**
	 * Used to assign a unique id to each PointDescriptor, if two points have the same distance we sort by id
	 */
	protected final static AtomicInteger index = new AtomicInteger(0);
  	
	public final Point3f position;
	protected final Vector3f neighbors[];
	protected final Point3f neighborPositions[];
	protected final Bead neighborBeads[];
	protected final String id;  
	protected final int describedPoint;
	protected final int numNeighbors;
	final protected Bead bead;
	final protected ViewDataBeads view;
		
  	/**
  	 * Constructs the PointDescriptor for a certain Point3d
  	 * @param pos - The Position of a descriptor
  	 * @param numNeighbors - The number of neighbors to use for the creation. If @param beTolerant is set to true one neighbor more is used
  	 * @param discardFirstNeigbor - Defines if the first nearest neighbor is discarded. Usually this should be true as it is the point itself.
  	 */
  	PointDescriptor(final KDTree tree, final Bead bead, final ViewDataBeads view, final int[] neighborIndices, final String id, final int describedPoint)
	{
  		position = new Point3f( bead.getL() );
		numNeighbors = neighborIndices.length;
		neighbors = new Vector3f[numNeighbors];
		neighborBeads = new Bead[numNeighbors];
		neighborPositions = new Point3f[numNeighbors];
  		this.id = id;
  		this.describedPoint = describedPoint;
  		this.bead = bead;
  		this.view = view;
		
		// build the descriptor
		this.buildDescriptor(tree, neighborIndices);
	}
	
	/**
	 * Computes recursively how to create different PointDescriptors of the same Point when some tolerance 
	 * is allowed, e.g. there are outliers which should be identified. For 3 neighbors allowing 2 outliers
	 * and starting with the second nearest neigbor (the nearest one is in most cases the one you use as input)
	 * the result looks like that:
	 * 
	 * (Each coloum will be one PointDescriptor)
	 *    
	 * 		|1| |1| |1| |2| |1| |1| |2| |1| |2| |3| 
	 * 		|2| |2| |3| |3| |2| |3| |3| |4| |4| |4| 
	 * 		|3| |4| |4| |4| |5| |5| |5| |5| |5| |5| 
	 * 
	 * @param tolerance - How many tolerance is accepted [0 ... m]
	 * @param n - initialized with the number of neighbors
	 * @param offset - the starting position for the neighbors, usually 1
	 * @return an array containing the neighbors for each PointDescriptor as array
	 */
	protected static int[][] computePDRecursive(int tolerance, int n, int offset)
	{		
		if (tolerance == 0)
		{
			final int[][] neighbors = new int[1][n];
			
			for (int i = 0; i < n; i++)
				neighbors[0][i] = i + offset;
			
			return neighbors;
		}
		else 
		{
			final ArrayList<int[][]> allneighbors = new ArrayList<int[][]>();
			int size = 1;
			
			// compute the subgroups
			for (int k = tolerance + n - 1; k > tolerance - 1; k--)
			{
				final int[][] neighbors = computePDRecursive(tolerance - 1, k - tolerance + 1, offset);
				
				allneighbors.add(neighbors);
				size += neighbors.length;
			}
			
			// fill the final array
			final int[][] neighbors = new int[size][n];
						
			int pos = 0;
			
			for (int[][] subn : allneighbors)
			{
				for (int i = 0; i < subn.length; i++)
				{
					for (int j = 0; j < subn[i].length; j++)
						neighbors[i + pos][j] = subn[i][j];

					for (int j = subn[i].length; j < n; j++)
						neighbors[i + pos][j] = j + offset + tolerance;
				}

				pos += subn.length;
			}
			
			for (int j = 0; j < n; j++)
				neighbors[pos][j] = j + offset + tolerance;
			
			return neighbors;
		}
	}
  	 
  	/**
  	 * Computes the difference between two PointDescriptors. If beTolerant is true, this method discards one of the vectors of each PointDescriptor.
  	 * @param pd2 - the PointDescriptor to compare to 
  	 * @return The differences between two PointDescriptors or NaN if one of them is not initialized
  	 */
  	public abstract double getDifference(final PointDescriptor pd2);
  	  	
  	/**
  	 * Computes the length normalized difference between two PointDescriptors (longest vector assumed to have a length of 1). 
  	 * If beTolerant is true, this method discards one of the vectors of each PointDescriptor.
  	 * @param pd2 - the PointDescriptor to compare to 
  	 * @return The differences between two PointDescriptors or NaN if one of them is not initialized
  	 */
  	public abstract double getLengthNormalizedDifference(final PointDescriptor pd2);
  	
  	/**
  	 * Computes the linear difference between two vectors
  	 * @param a - First vector
  	 * @param b - Vector that will be subtracted
  	 * @return Linear Difference or NaN if a or b null
  	 */
  	protected float getVectorDifference(Vector3f a, Vector3f b)
  	{
  		if (a == null || b == null)
  			return Float.NaN;
  		
  		float difference = 0;
  		
		difference += (float)Math.pow(a.x - b.x,2); 
		difference += (float)Math.pow(a.y - b.y,2);
		difference += (float)Math.pow(a.z - b.z,2);
  		
		return difference;
  	}
  	
  	/**
  	 * Builds the PointDescriptor
  	 * @param tree - the KDTree of the points that surround the desired PointDescriptor
  	 * @return true if it was successful, false if there were not enough nearest neighbors
  	 */
  	private boolean buildDescriptor(final KDTree tree, final int[] neighborIndices)
  	{  	
  		// create a Weka instance of Point3d
  		Instance inst = WekaFunctions.createInstance(position, tree.getInstances());  		  	
  		
  		// here goes the result
  		Instance[] nn = new Instance[neighbors.length];
  		int maxNeighbor = 0;
  		for (int i : neighborIndices)
  			if (i > maxNeighbor)
  				maxNeighbor = i;
  		  		
  		// get the n nearest neighbors
  		//ArrayList<Bead> nn = tree.findNearest( position.x, position.y, position.z, 100 );

  		try
		{
  			// this is 1-terminated, e.g. if I want only neighbor 0, I have to get 1 neighbor
			Instances tmp = tree.kNearestNeighbours(inst, maxNeighbor + 1);
			for (int i = 0; i < neighbors.length; i++)
				nn[i] = tmp.instance(neighborIndices[i]);
		}
		catch (Exception e)
		{
			IOFunctions.printErr("PointDescriptor.buildDescriptor(): " + e);
			return false;
		}
				
		// compute the relative vectors
		for (int i = 0; i < neighbors.length; i++)
		{
			/*neighborBeads[i] = nn.get( neighborIndices[i] );
			
			neighbors[i] = new Vector3f();
			neighbors[i].x = neighborBeads[i].getL()[0] - position.x;
			neighbors[i].y = neighborBeads[i].getL()[1] - position.y;
			neighbors[i].z = neighborBeads[i].getL()[2] - position.z;

			neighborPositions[i] = new Point3f();
			neighborPositions[i].x = neighborBeads[i].getL()[0];
			neighborPositions[i].y = neighborBeads[i].getL()[1];
			neighborPositions[i].z = neighborBeads[i].getL()[2];*/
			
			neighborBeads[i] = view.getBeadStructure().getBead( Integer.parseInt( nn[i].stringValue(3) ) );
			
			neighborPositions[i] = new Point3f();
			neighborPositions[i].x = neighborBeads[i].getL()[0];
			neighborPositions[i].y = neighborBeads[i].getL()[1];
			neighborPositions[i].z = neighborBeads[i].getL()[2];
			/*
			neighborPositions[i].x = (float)nn[i].value(0);
			neighborPositions[i].y = (float)nn[i].value(1);
			neighborPositions[i].z = (float)nn[i].value(2);
			neighborBeads[i] = view.beads.getBead( neighborPositions[i].x, neighborPositions[i].y, neighborPositions[i].z );		
			*/
			
			neighbors[i] = new Vector3f();
			neighbors[i].x = neighborPositions[i].x - position.x;
			neighbors[i].y = neighborPositions[i].y - position.y;
			neighbors[i].z = neighborPositions[i].z - position.z;												
		}
		
		return true;
  	}
  	
  	public Point3f[] getNeighborPositions(){ return neighborPositions; }
  	public Bead[] getNeighborBeads(){ return neighborBeads; }
  	
  	/**
  	 * Returns the ID
  	 * @return the unique id of the PointDescriptor
  	 */
  	public String getID()
  	{
  		return id;
  	}

  	/**
  	 * Returns the Bead the Descriptor is based on
  	 * @return Bead
  	 */
  	public Bead getBead()
  	{
  		return bead;
  	}
  	
  	/**
  	 * Returns the integer id of the described point
  	 * @return the non-unique(in case of tolerance) integer id of the PointDescriptor
  	 */
  	public int getDescribedPoint()
  	{
  		return describedPoint;
  	}

  	/**
  	 * Overwrites the toString method
  	 */
  	public String toString()
  	{
  		String out = "PointDescriptor[" +id + "] has position: "  + position;
  		for (int i = 0; i < neighbors.length; i++)
  			out += "neighbor " + (i+1) + " vector: " + neighbors[i];
  		
  		return out;
  	}
  	
  	/**
  	 * Prints the content of the PointDescriptor
  	 */
  	public void print()
  	{
  		IOFunctions.println("PointDescriptor[" +id + "] has position: "  + position);
  		for (int i = 0; i < neighbors.length; i++)
  			IOFunctions.println("neighbor " + (i+1) + " vector: " + neighbors[i]); 
  	}

  	/**
  	 * Prints the content of the PointDescriptor in short form without the neighbors
  	 */
  	public void printShort()
  	{
  		IOFunctions.println("PointDescriptor[" +id + "] has position: "  + position);
  	}
  	
  	/**
  	 * Prints the content of the PointDescriptor
  	 * @param digits - the number of digits for each number
  	 */
  	public void print(int digits)
  	{
  		String formatting1 = "0.";
  		String formatting2 = ";-0.";
  		
  		for (int i = 0; i < digits; i++)
  		{
  			formatting1 += "0";
  			formatting2 += "0";
  		}
  		
		DecimalFormat num = new DecimalFormat(formatting1 + formatting2/*"0.00000;-0.00000"*/);
		
  		IOFunctions.println("PointDescriptor[" +id + "] has position: ("  + num.format(position.x) + ", " + num.format(position.y) + ", " + num.format(position.z) + ")");
  		
  		int count = 1;
  		for (Vector3f vec : neighbors)
  			IOFunctions.println("neighbor " + (count++) + " vector: ("  + num.format(vec.x) + ", " + num.format(vec.y) + ", " + num.format(vec.z) + ")");   		
  	}
  	
}
