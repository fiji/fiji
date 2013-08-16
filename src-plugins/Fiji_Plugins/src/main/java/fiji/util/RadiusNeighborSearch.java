package fiji.util;
/**
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
 * @author Stephan Preibisch and Johannes Schindelin
 */

import java.util.ArrayList;
import java.util.Collections;

import fiji.util.node.Leaf;
import fiji.util.node.Node;
import fiji.util.node.NonLeaf;

public class RadiusNeighborSearch<T extends Leaf<T>>
{
	final protected KDTree<T> kdTree;
	protected ArrayList<T> pointsWithinRadius;
	protected double radius;
	
	public RadiusNeighborSearch( final KDTree<T> kdTree )  { this.kdTree = kdTree; }

	public KDTree<T> getKDTree() { return kdTree; } 

	
	public ArrayList<T> findNeighborsUnsorted( final T point, final double r )
	{
		this.pointsWithinRadius = new ArrayList<T>();
		this.radius = r;
		
		findNeighbors( point, kdTree.getRoot(), 0 );
		
		return pointsWithinRadius;
	}

	public ArrayList<T> findNeighborsSorted( final T point, final double r )
	{
		// first find them unsorted
		findNeighborsUnsorted( point, r );

		// now sort
		Collections.sort( pointsWithinRadius, new DistanceComparator<T>( point ) );
		
		return pointsWithinRadius;
	}

	protected void findNeighbors( final T point, final Node<T> node, final int depth ) 
	{
		// if we reach a leaf we check if it is within the radius,
		// and if it is we add it to the ArrayList of pointsWithinRadius
		if ( node.isLeaf() )
		{
			// get the leaf instance 
			final T leaf = (T)node;
			
			// compute the real distance to the point of interest
			final double distance = leaf.distanceTo( point );
			
			// check if it is within range
			if ( distance <= radius )
				pointsWithinRadius.add( leaf );
		}
		else
		{
			// get current splitting plane
			final int k = (depth % kdTree.getDimension());
			
			// cast to the Nonleaf instance that it is
			final NonLeaf<T> nonLeaf = (NonLeaf<T>)node;

			// if there is nothing on the right side we can look on the left one where 
			// there should be only one leaf instance anyways 
			if ( nonLeaf.right == null )
			{
				findNeighbors( point, nonLeaf.left, depth + 1 );
			}			
			else if ( nonLeaf.left == null ) // same for the left side
			{
				findNeighbors( point, nonLeaf.right, depth + 1 );
			}
			else
			{
				// compute the projected distance
				// distance just in the current splitting plane
				final float projectedDistance = nonLeaf.coordinate - point.get( k );
				
				// if the distance is < 0, we have to go right, otherwise left
				final boolean lookRight = projectedDistance < 0;
	
				// we first test the direction which the projected distance is smaller 
				findNeighbors( point, lookRight ? nonLeaf.right : nonLeaf.left, depth + 1 );
				
				// maybe there is another one within the radius on the other side of the split plane?
				if ( Math.abs( projectedDistance ) <= radius)
					findNeighbors(point, lookRight ? nonLeaf.left : nonLeaf.right, depth + 1);
			}
		}
	}	
}
