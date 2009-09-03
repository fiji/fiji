package fiji.util;

import java.util.ArrayList;
import java.util.List;

public class NearestNeighbor {
	public interface Leaf extends KDTree.Leaf {
		float distanceTo(Leaf other);
	}

	protected KDTree kdTree;

	public NearestNeighbor(List leaves, int dimension) {
		kdTree = new KDTree(leaves, dimension);
	}

	public Leaf findNearestNeighbor(Leaf point) {
		return findNearestNeighbor(point, kdTree.getRoot(), 0);
	}

	// TODO: store calculated distance in a class to avoid recalculation
	// TODO: maybe there is a way to avoid calculating the square root?
	public Leaf findNearestNeighbor(Leaf point,
			KDTree.Node node, int depth) {
		if (node instanceof KDTree.Leaf)
			return (Leaf)node;

		int k = (depth % kdTree.getDimension());
		KDTree.NonLeaf nonLeaf = (KDTree.NonLeaf)node;
		float projectedDistance = nonLeaf.coordinate - point.get(k);
		boolean lookRight = projectedDistance < 0;
		Leaf result = findNearestNeighbor(point,
			lookRight ? nonLeaf.right : nonLeaf.left, depth + 1);

		// maybe there is a better one
		float distance = point.distanceTo(result);
		if (distance > Math.abs(projectedDistance)) {
			Leaf other = findNearestNeighbor(point,
				lookRight ? nonLeaf.left : nonLeaf.right,
				depth + 1);
			if (point.distanceTo(other) < distance)
				result = other;
		}

		return result;
	}

	static class Leaf2D implements Leaf {
		float x, y;

		public Leaf2D(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public float get(int k) {
			return k == 0 ? x : y;
		}

		static float square(float x) {
			return x * x;
		}

		public float distanceTo(Leaf other) {
			Leaf2D o = (Leaf2D)other;
			float square = square(o.x - x) + square(o.y - y);
			return (float)Math.ceil(Math.sqrt((float)square));
		}

		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}

	public static void test() {
		List list = new ArrayList();
		list.add(new Leaf2D(2, 3));
		list.add(new Leaf2D(5, 4));
		list.add(new Leaf2D(9, 6));
		list.add(new Leaf2D(4, 7));
		list.add(new Leaf2D(8, 1));
		list.add(new Leaf2D(7, 2));

		NearestNeighbor kd = new NearestNeighbor(list, 2);
		System.out.println(kd.findNearestNeighbor(new Leaf2D(10, 1)));
	}

	public static void main(String[] args) {
		test();
	}
}
