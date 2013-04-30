/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.kdtree;

import java.util.Arrays;

import mpicbg.imglib.algorithm.kdtree.node.Leaf;
import mpicbg.imglib.algorithm.kdtree.node.Node;
import mpicbg.imglib.algorithm.kdtree.node.NonLeaf;

/**
 * TODO
 *
 * @author Johannes Schindelin
 */
public class NNearestNeighborSearch<T extends Leaf<T>>
{
	final protected KDTree<T> kdTree;

	public NNearestNeighborSearch(final KDTree<T> kdTree) {
		this.kdTree = kdTree;
	}

	public KDTree<T> getKDTree() {
		return kdTree;
	}

	// TODO: use a priority queue for larger n
	// TODO: refactor to use a class to insert the points into
	public T[] findNNearestNeighbors(final T point, final int n)
	{
		final T[] result = point.createArray(n);

		int count = findNNearestNeighbors(point, kdTree.getRoot(), 0, 0, result);
		if (count < result.length)
		{
			T[] newResult = point.createArray(count);

			for (int i = 0; i < count; ++i)
				newResult[i] = result[i];

			return newResult;
		}
		return result;
	}

	// TODO: store calculated distance in a class to avoid recalculation
	// TODO: maybe there is a way to avoid calculating the square root?
	public int findNNearestNeighbors(final T point, Node<T> node, int depth, int gotAlready, T[] result) {
		if (node.isLeaf()) {
			// TODO: urgh!  This _cries out loud_ for a class
			final T leaf = ((Leaf<T>)node).getEntry();
			if (gotAlready == 0) {
				result[0] = leaf;
				return 1;
			}

			int index;
			// TODO: double urgh!
			if (gotAlready < result.length)
			{
				for (index = 0; index < gotAlready &&
						point.distanceTo(result[index]) <
						point.distanceTo(leaf); index++);
			}
			else 
			{
				index = Arrays.binarySearch( result, leaf, new DistanceComparator<T>( point ) );
				
				if (index < 0)
					index = -1 - index;
			}
			if (index < result.length) {
				if (gotAlready < result.length) {
					if (index < gotAlready)
						System.arraycopy(result, index,
							result, index + 1,
							gotAlready - index);
					gotAlready++;
				}
				else if (index + 1 < result.length)
					System.arraycopy(result, index,
						result, index + 1,
						result.length - index - 1);
				result[index] = leaf;
			}

			return gotAlready;
		}

		final int k = (depth % kdTree.getDimension());
		final NonLeaf<T> nonLeaf = (NonLeaf<T>)node;

		if (nonLeaf.right == null)
			return findNNearestNeighbors(point, nonLeaf.left, depth + 1, gotAlready, result);

		if (nonLeaf.left == null)
			return findNNearestNeighbors(point, nonLeaf.right, depth + 1, gotAlready, result);

		final float projectedDistance = nonLeaf.coordinate - point.get(k);
		final boolean lookRight = projectedDistance < 0;

		gotAlready = findNNearestNeighbors(point, lookRight ? nonLeaf.right : nonLeaf.left, depth + 1, gotAlready, result);

		// maybe there is a better one
		float distance = point.distanceTo(result[gotAlready - 1]);

		if (gotAlready < result.length || distance > Math.abs(projectedDistance)) {
			gotAlready = findNNearestNeighbors(point, lookRight ? nonLeaf.left : nonLeaf.right, depth + 1, gotAlready, result);
		}

		return gotAlready;
	}

	public String toString(T leaf) {
		if (leaf == null)
			return "null";

		int dimension = kdTree.getDimension();
		String result = "(" + leaf.get(0);
		for (int i = 1; i < dimension; i++)
			result += ", " + leaf.get(i);
		return result + ")";
	}

	public String toString(T[] array, int length) {
		if (length == 0)
			return "[]";
		String result = "[" + toString(array[0]);
		for (int i = 1; i < length; i++)
			result += ", " + toString(array[i]);
		return result + "]";
	}
}
