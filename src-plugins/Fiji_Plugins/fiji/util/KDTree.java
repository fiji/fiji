package fiji.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class KDTree {
	public interface Node { }

	public interface Leaf extends Node {
		/* get the k'th component of the vector */
		float get(int k);
	}

	protected class NonLeaf implements Node {
		/* the axis of 'coordinate' is the depth modulo the dimension */
		float coordinate;
		Node left, right;

		public NonLeaf(float coordinate, Node left, Node right) {
			this.coordinate = coordinate;
			this.left = left;
			this.right = right;
		}

		public String toString(Node node) {
			if (node == null)
				return "null";
			if (node instanceof Leaf) {
				String result = "(" + ((Leaf)node).get(0);
				for (int i = 1; i < dimension; i++)
					result += ", " + ((Leaf)node).get(i);
				return result + ")";
			}
			if (node instanceof NonLeaf) {
				NonLeaf nonLeaf = (NonLeaf)node;
				return "[" + toString(nonLeaf.left)
					+ " |{" + nonLeaf.coordinate + "} "
					+ toString(nonLeaf.right) + "]";
			}
			return node.toString();
		}

		public String toString() {
			return toString(this);
		}
	}

	/*
	 * Use only a subset of at most medianLength semi-randomly picked
	 * values to determine the splitting point.
	 */
	protected int medianLength = 15;

	protected int dimension;
	protected Node root;

	/**
	 * Construct a KDTree from the elements in the given list.
	 *
	 * The elements must implement the interface Leaf.
	 *
	 * The parameter 'leaves' must be a list and cannot be an iterator,
	 * as the median needs to be calculated (or estimated, if the length
	 * is greater than medianLength).
	 */
	public KDTree(List leaves, int dimension) {
		this.dimension = dimension;
		root = makeNode(leaves, 0);
	}

	protected Node makeNode(List leaves, int depth) {
		int length = leaves.size();

		if (length == 0)
			return null;

		if (length == 1)
			return (Leaf)leaves.get(0);

		int k = (depth % dimension);
		float median = median(leaves, k);

		List left = new ArrayList();
		List right = new ArrayList();
		for (int i = 0; i < length; i++) {
			Leaf leaf = (Leaf)leaves.get(i);
			if (leaf.get(k) <= median)
				left.add(leaf);
			else
				right.add(leaf);
		}

		return new NonLeaf(median, makeNode(left, depth + 1),
			makeNode(right, depth + 1));
	}

	protected float median(List leaves, int k) {
		float[] list;
		if (leaves.size() <= medianLength) {
			list = new float[leaves.size()];
			for (int i = 0; i < list.length; i++) {
				Leaf leaf = (Leaf)leaves.get(i);
				list[i] = leaf.get(k);
			}
		}
		else {
			list = new float[medianLength];
			Random random = new Random();
			for (int i = 0; i < list.length; i++) {
				int index = Math.abs(random.nextInt()) % list.length;
				Leaf leaf = (Leaf)leaves.get(index);
				list[i] = leaf.get(k);
			}
		}
		Arrays.sort(list);
		return (list.length & 1) == 1 ?
			list[list.length / 2] :
			(list[list.length / 2] + list[list.length / 2 - 1]) / 2;
	}

	public Node getRoot() {
		return root;
	}

	public int getDimension() {
		return dimension;
	}

	public String toString(Node node, String indent) {
		if (node instanceof Leaf)
			return indent + node.toString();
		NonLeaf nonLeaf = (NonLeaf)node;
		return toString(nonLeaf.left, indent + "\t") + "\n"
			+ indent + nonLeaf.coordinate + "\n"
			+ toString(nonLeaf.right, indent + "\t") + "\n";
	}

	public String toString() {
		return toString(root, "");
	}

	// tests

	static class Leaf2D implements Leaf {
		float x, y;

		public Leaf2D(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public float get(int k) {
			return k == 0 ? x : y;
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

		KDTree kd = new KDTree(list, 2);
		System.out.println(kd);
	}

	public static void main(String[] args) {
		test();
	}
}
