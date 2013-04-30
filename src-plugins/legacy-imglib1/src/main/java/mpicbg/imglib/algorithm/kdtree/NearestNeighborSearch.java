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

import mpicbg.imglib.algorithm.kdtree.node.Leaf;
import mpicbg.imglib.algorithm.kdtree.node.Node;
import mpicbg.imglib.algorithm.kdtree.node.NonLeaf;

/**
 * TODO
 *
 * @author Johannes Schindelin
 */
public class NearestNeighborSearch<T extends Leaf<T>>
{
	final protected KDTree<T> kdTree;
	private T bestPointSoFar;

	public NearestNeighborSearch(final KDTree<T> kdTree) {
		this.kdTree = kdTree;
	}

	public KDTree<T> getKDTree() {
		return kdTree;
	}

	public T findNearestNeighbor(final T point) {
		bestPointSoFar = null;
		return findNearestNeighbor(point, kdTree.getRoot(), 0);
	}

	// TODO: store calculated distance in a class to avoid recalculation
	// TODO: maybe there is a way to avoid calculating the square root?
	protected T findNearestNeighbor(final T point, final Node<T> node, final int depth) {
		if (node.isLeaf()) {
			if (bestPointSoFar == null)
				bestPointSoFar = ((Leaf<T>)node).getEntry();

			T returnNode = ((Leaf<T>)node).getEntry();

			if (point.distanceTo(bestPointSoFar) < point.distanceTo(returnNode))
				returnNode = bestPointSoFar;

			return returnNode;
		}

		final int k = (depth % kdTree.getDimension());
		final NonLeaf<T> nonLeaf = (NonLeaf<T>)node;

		if (nonLeaf.right == null)
			return findNearestNeighbor(point, nonLeaf.left, depth + 1);

		if (nonLeaf.left == null)
			return findNearestNeighbor(point, nonLeaf.right, depth + 1);

		final float projectedDistance = nonLeaf.coordinate - point.get(k);
		final boolean lookRight = projectedDistance < 0;

		final T result = findNearestNeighbor(point, lookRight ? nonLeaf.right : nonLeaf.left, depth + 1);

		// maybe there is a better one
		final float distance = point.distanceTo(result);

		if (distance > Math.abs(projectedDistance)) {
			final T other = findNearestNeighbor(point, lookRight ? nonLeaf.left : nonLeaf.right, depth + 1) ;
			if (point.distanceTo(other) < distance)
				return other;
		}

		return result;
	}
}
