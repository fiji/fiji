package fiji.util;

import java.util.ArrayList;
import java.util.List;

public class NearestNeighborInt {
	public interface Leaf extends KDTreeInt.Leaf {
		int distanceTo(Leaf other);
	}

	protected KDTreeInt kdTree;

	public NearestNeighborInt(List leaves, int dimension) {
		kdTree = new KDTreeInt(leaves, dimension);
	}

	public Leaf findNearestNeighborInt(Leaf point) {
		return findNearestNeighborInt(point, kdTree.getRoot(), 0);
	}

	// TODO: store calculated distance in a class to avoid recalculation
	// TODO: maybe there is a way to avoid calculating the square root?
	public Leaf findNearestNeighborInt(Leaf point,
			KDTreeInt.Node node, int depth) {
		if (node instanceof KDTreeInt.Leaf)
			return (Leaf)node;

		int k = (depth % kdTree.getDimension());
		KDTreeInt.NonLeaf nonLeaf = (KDTreeInt.NonLeaf)node;
		int projectedDistance = nonLeaf.coordinate - point.get(k);
		boolean lookRight = projectedDistance < 0;
		Leaf result = findNearestNeighborInt(point,
			lookRight ? nonLeaf.right : nonLeaf.left, depth + 1);

		// maybe there is a better one
		int distance = point.distanceTo(result);
		if (distance > Math.abs(projectedDistance)) {
			Leaf other = findNearestNeighborInt(point,
				lookRight ? nonLeaf.left : nonLeaf.right,
				depth + 1);
			if (point.distanceTo(other) < distance)
				result = other;
		}

		return result;
	}

	static class Leaf2D implements Leaf {
		int x, y;

		public Leaf2D(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int get(int k) {
			return k == 0 ? x : y;
		}

		static int square(int x) {
			return x * x;
		}

		public int distanceTo(Leaf other) {
			Leaf2D o = (Leaf2D)other;
			int square = square(o.x - x) + square(o.y - y);
			return (int)Math.ceil(Math.sqrt((float)square));
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

		NearestNeighborInt kd = new NearestNeighborInt(list, 2);
		System.out.println(kd.findNearestNeighborInt(new Leaf2D(10, 1)));
	}

	public static void main(String[] args) {
		test();
	}
}
