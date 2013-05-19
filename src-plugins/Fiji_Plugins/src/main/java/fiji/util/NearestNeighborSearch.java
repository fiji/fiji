package fiji.util;

import fiji.util.node.Leaf;
import fiji.util.node.Node;
import fiji.util.node.NonLeaf;

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
				bestPointSoFar = (T)node;

			T returnNode = (T)node;

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
