/**
 * 
 */
package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;

import weka.core.neighboursearch.KDTree;
//import edu.bucknell.util.KDTree;


/**
 * @author Stephan
 *
 */
public class FixedPointDescriptor extends PointDescriptor
{
	/**
	 * This static method creates a number of FixedPointDescriptors based on the tolerance term
	 * 
	 * @param tree - The KDTree to construct the PointDescriptors from
	 * @param pos - The position at which the PointDescriptor is created
	 * @param numNeighbors - The number of neighbors for this point
	 * @param tolerance - How many number of Points tolerance
	 * @param firstNeighborIndex - The first neighbor to use (0 terminated), the 0 neighbor is usually the point itself
	 * @return - A List of PointDescriptors
	 */
	public static final ArrayList <PointDescriptor> PointDescriptorFactory(final KDTree tree, final Bead bead, final ViewDataBeads view, final int numNeighbors, final int tolerance, final int firstNeighborIndex)
	{
		final ArrayList <PointDescriptor> pointDescriptors = new ArrayList<PointDescriptor>();
		
		final int[][] neighbors = computePDRecursive(tolerance, numNeighbors, firstNeighborIndex);
		
		int id = index.getAndIncrement();
					
		for (int i = 0; i < neighbors.length; i++)
			pointDescriptors.add( new FixedPointDescriptor(tree, bead, view, neighbors[i], tree.getInstances().relationName() + "_" + id + "_" + (i+1), id) );
			//pointDescriptors.add( new FixedPointDescriptor(tree, bead, neighbors[i], tree.getName() + "_" + id + "_" + (i+1), id) );
			
				
		return pointDescriptors;
	}

  	/**
  	 * Constructs the PointDescriptor for a certain Point3d
  	 * @param pos - The Position of a descriptor
  	 * @param numNeighbors - The number of neighbors to use for the creation. If @param beTolerant is set to true one neighbor more is used
  	 * @param discardFirstNeigbor - Defines if the first nearest neighbor is discarded. Usually this should be true as it is the point itself.
  	 */
  	private FixedPointDescriptor(final KDTree tree, final Bead bead, final ViewDataBeads view, final int[] neighborIndices, final String id, final int describedPoint)
	{
  		super(tree, bead, view, neighborIndices, id, describedPoint);
	}

	@Override
	public double getLengthNormalizedDifference(final PointDescriptor pd2)
  	{
  		double difference = 0.0;
  		
  		int numNeighbors = Math.min(this.numNeighbors, pd2.numNeighbors);

		Vector3f a[] = new Vector3f[ numNeighbors ];
		Vector3f b[] = new Vector3f[ numNeighbors ];

		float maxLength = -1;
		
		for (int i = 0; i < numNeighbors; i++)
		{
			a[i] = new Vector3f();
			a[i].set( neighbors[i] );			
			
			b[i] = new Vector3f();
			b[i].set( pd2.neighbors[i] );
			
			if ( a[i].length() > maxLength )
				maxLength = a[i].length(); 

			if ( b[i].length() > maxLength )
				maxLength = a[i].length(); 
		}
		
		for (int i = 0; i < numNeighbors; i++)
		{
			a[i].scale( 1f/maxLength );
			b[i].scale( 1f/maxLength );

			difference += getVectorDifference( a[i], b[i] );						
		}
  		
		return difference / (double)numNeighbors;
  	}
	
  	/**
  	 * Computes the difference between two PointDescriptors. If beTolerant is true, this method discards one of the vectors of each PointDescriptor.
  	 * @param pd2 - the PointDescriptor to compare to 
  	 * @return The sum of vector differences (simple subtraction) or NaN if one of them is not initialized
  	 */
	@Override
	public double getDifference(final PointDescriptor pd2)
  	{
  		double difference = 0.0;
  		
  		int numNeighbors = Math.min(this.numNeighbors, pd2.numNeighbors);

		for (int i = 0; i < numNeighbors; i++)
			difference += getVectorDifference(pd2.neighbors[i], neighbors[i]);
  		
  		/*if (!beTolerant)
  		{
  			// that is the easy case
  			for (int i = 0; i < numNeighbors; i++)
  				difference += getVectorDifference(pd2.neighbors[i], neighbors[i]);
  		}
  		else
  		{
  			// leave out vectors iteratingly on both sides to check all
  			// possible correspondences without changing the order
  			double minDifference = Double.MAX_VALUE;
  			
  			// the case of 1-1 2-2 3-3 (easy) is then when 
  			// doNotUseA == doNotUseB == numNeighbors
  			for (int doNotUseA = 0; doNotUseA <= numNeighbors; doNotUseA++)
  				for (int doNotUseB = 0; doNotUseB <= doNotUseA; doNotUseB++)
  				{
  		  			double tmpDiff = 0;
  		  			int indexA = 0, indexB = 0;
  		  			for (int i = 0; i < numNeighbors; i++)
  		  			{
  		  				if (doNotUseA == i)
  		  					indexA++;
  		  				
  		  				if (doNotUseB == i)
  		  					indexB++;
  		  				
  		  				tmpDiff += getVectorDifference(pd2.neighbors[indexA], neighbors[indexB]);
  		  				
  		  				indexA++;
  		  				indexB++;
  		  			}

  		  			if (tmpDiff < minDifference)
  		  				minDifference = tmpDiff;
  				}
  			
  			difference = minDifference;
  		}*/
  		
  		return difference / (double)numNeighbors;
  	}
}
