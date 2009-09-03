package fiji.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class NNearestNeighbors {
	public interface Leaf extends KDTree.Leaf {
		float distanceTo(Leaf other);
	}

	protected KDTree kdTree;

	public NNearestNeighbors(List leaves, int dimension) {
		kdTree = new KDTree(leaves, dimension);
	}

	// TODO: use a priority queue for larger n
	// TODO: refactor to use a class to insert the points into
	public Leaf[] findNNearestNeighbors(Leaf point, int n) {
		Leaf[] result = new Leaf[n];
		int count = findNNearestNeighbors(point, kdTree.getRoot(), 0, 0, result);
		if (count < result.length) {
			Leaf[] newResult = new Leaf[count];
			System.arraycopy(result, 0, newResult, 0, count);
			result = newResult;
		}
		return result;
	}

	// TODO: store calculated distance in a class to avoid recalculation
	// TODO: maybe there is a way to avoid calculating the square root?
	public int findNNearestNeighbors(final Leaf point,
			KDTree.Node node, int depth,
			int gotAlready, Leaf[] result) {
		if (node instanceof KDTree.Leaf) {
			// TODO: urgh!  This _cries out loud_ for a class
			Leaf leaf = (Leaf)node;
			if (gotAlready == 0) {
				result[0] = leaf;
				return 1;
			}

			int index;
			// TODO: double urgh!
			if (gotAlready < result.length)
				for (index = 0; index < gotAlready &&
						point.distanceTo(result[index]) <
						point.distanceTo(leaf); index++);
			else {
				index = Arrays.binarySearch(result, leaf,
					new Comparator() {
					public int compare(Object a, Object b) {
						float distA = point.distanceTo((Leaf)a);
						float distB = point.distanceTo((Leaf)b);
						return distA < distB ? -1 :
							distA > distB ? +1 : 0;
					}
					public boolean equals(Object a, Object b) {
						return a.equals(b);
					}});
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

		int k = (depth % kdTree.getDimension());
		KDTree.NonLeaf nonLeaf = (KDTree.NonLeaf)node;
		if (nonLeaf.right == null)
			return findNNearestNeighbors(point, nonLeaf.left,
				depth + 1, gotAlready, result);
		if (nonLeaf.left == null)
			return findNNearestNeighbors(point, nonLeaf.right,
				depth + 1, gotAlready, result);
		float projectedDistance = nonLeaf.coordinate - point.get(k);
		boolean lookRight = projectedDistance < 0;
		gotAlready = findNNearestNeighbors(point,
			lookRight ? nonLeaf.right : nonLeaf.left, depth + 1,
			gotAlready, result);

		// maybe there is a better one
		float distance = point.distanceTo(result[gotAlready - 1]);
		if (gotAlready < result.length ||
				distance > Math.abs(projectedDistance)) {
			gotAlready = findNNearestNeighbors(point,
				lookRight ? nonLeaf.left : nonLeaf.right,
				depth + 1, gotAlready, result);
		}

		return gotAlready;
	}

	public String toString(Leaf leaf) {
		if (leaf == null)
			return "null";

		int dimension = kdTree.getDimension();
		String result = "(" + leaf.get(0);
		for (int i = 1; i < dimension; i++)
			result += ", " + leaf.get(i);
		return result + ")";
	}

	public String toString(Leaf[] array, int length) {
		if (length == 0)
			return "[]";
		String result = "[" + toString(array[0]);
		for (int i = 1; i < length; i++)
			result += ", " + toString(array[i]);
		return result + "]";
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

		Leaf2D point = new Leaf2D(10, 1);

		for (int i = 0; i < list.size(); i++)
			System.out.println("point " + i + ": " + list.get(i) + ", dist: "
				+ point.distanceTo((Leaf)list.get(i)));

		NNearestNeighbors kd = new NNearestNeighbors(list, 2);
		Leaf[] leaves = kd.findNNearestNeighbors(point, 2);
		for (int i = 0; i < leaves.length; i++)
			System.out.println("neighbor " + i + ": " + leaves[i] + ", dist: "
				+ point.distanceTo(leaves[i]));
	}

	public static void main(String[] args) {
		test();
	}
}
