/**
 * 
 */
package mpicbg.spim.registration.bead.descriptor;

import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.Bead;
import mpicbg.spim.vib.FastMatrix;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import weka.core.neighboursearch.KDTree;
//import edu.bucknell.util.KDTree;
/**
 * @author Stephan
 *
 */
public class BestRigidPointDescriptor extends PointDescriptor
{
	final Vector3f[] tmp;
	final Transform3D trans;

	// temporary arrays to speed up bestRigid
	final Matrix3f result = new Matrix3f();
	final float[][] N = new float[4][4];

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
	public static final ArrayList <PointDescriptor> PointDescriptorFactory(final KDTree tree, final Bead bead, final ViewDataBeads view, final int numNeighbors, final int tolerance, final int firstNeighborIndex)
	{
		final ArrayList <PointDescriptor> pointDescriptors = new ArrayList<PointDescriptor>();
		
		final int[][] neighbors = computePDRecursive(tolerance, numNeighbors, firstNeighborIndex);
		
		int id = index.getAndIncrement();
					
		for (int i = 0; i < neighbors.length; i++)		
			pointDescriptors.add( new BestRigidPointDescriptor(tree, bead, view, neighbors[i], tree.getInstances().relationName() + "_" + id + "_" + (i+1), id) );
			//pointDescriptors.add( new BestRigidPointDescriptor(tree, bead, neighbors[i], tree.getName() + "_" + id + "_" + (i+1), id) );
				
		return pointDescriptors;
	}

  	/**
  	 * Constructs the PointDescriptor for a certain Point3d
  	 * @param pos - The Position of a descriptor
  	 * @param neighborIndices - the 
  	 * @param id - The id of this {@link PointDescriptor}
  	 */
	private BestRigidPointDescriptor(KDTree tree, Bead bead, final ViewDataBeads view, int[] neighborIndices, String id, final int describedPoint)
	{
		super(tree, bead, view, neighborIndices, id, describedPoint);
		
		tmp = new Vector3f[neighbors.length];
		
		for (int i = 0; i < tmp.length; i++)
			tmp[i] = new Vector3f();
		
		trans = new Transform3D();
	}

	public final Matrix3f getMatrix(final PointDescriptor pd2)
	{
		return FastMatrix.bestRigidNoTranslation(this.neighbors, pd2.neighbors, false);		
	}
	
	private final void getMatrixFast(final PointDescriptor pd2)
	{
		FastMatrix.bestRigidNoTranslationNoScaling(this.neighbors, pd2.neighbors, this.result, this.N);
	}
	
	
	@Override
	public final double getDifference(final PointDescriptor pd2)
	{
		//final Matrix3d matrix = getMatrix(pd2);
		//Transform3D trans = new Transform3D();
		
		//trans.set(getMatrix(pd2));
		
		getMatrixFast(pd2);
		trans.set(result);

		double difference = 0;
		
		for (int i = 0; i < neighbors.length; i++)
		{
			//tmp[i] = new Vector3d(neighbors[i]);
			tmp[i].set(neighbors[i]);
			
			trans.transform(tmp[i]);			
			difference += getVectorDifference(tmp[i], pd2.neighbors[i]);			
		}
						
		return difference / (double)neighbors.length;
	}
	
	@Override
	public final double getLengthNormalizedDifference(final PointDescriptor pd2)
	{
		//final Matrix3d matrix = getMatrix(pd2);
		//Transform3D trans = new Transform3D();
		
		//trans.set(getMatrix(pd2));
		
		getMatrixFast(pd2);
		trans.set(result);

		double difference = 0;
		
		Vector3f a[] = new Vector3f[ neighbors.length ];
		Vector3f b[] = new Vector3f[ neighbors.length ];

		float maxLength = -1;
		
		for (int i = 0; i < neighbors.length; i++)
		{
			a[i] = new Vector3f();
			a[i].set( neighbors[i] );			
			trans.transform( a[i] );
			
			b[i] = new Vector3f();
			b[i].set( pd2.neighbors[i] );
			
			if ( a[i].length() > maxLength )
				maxLength = a[i].length(); 

			if ( b[i].length() > maxLength )
				maxLength = a[i].length(); 
		}
		
		for (int i = 0; i < neighbors.length; i++)
		{
			a[i].scale( 1f/maxLength );
			b[i].scale( 1f/maxLength );

			difference += getVectorDifference( a[i], b[i] );						
		}
		
		return (difference / (double)neighbors.length);
	}	
}
