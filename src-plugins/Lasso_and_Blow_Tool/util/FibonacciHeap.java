package util;

/*
 * This class does not implement a complete Fibonacci Heap:
 *
 * Since we will use it as a PriorityQueue, where (key,element)
 * pairs will not be changed, but only pushed into, and popped out
 * of the queue, we do not need DecreaseKey, Union, and Cut.
 */

public class FibonacciHeap implements Comparable
{
	private static class Node {
		Comparable key;
		Object object;
		Node next, previous, parent, firstChild;
		int degree;
		boolean marked;

		public Node(Comparable key, Object object, Node parent) {
			this.key = key;
			this.object = object;
			this.parent = parent;
		}

		void insert(Node node) {
			if (node.next != null || node.previous != null
					|| (node.parent != null &&
						node.parent != parent)
					|| previous != null)
				throw new RuntimeException("node not new: " + node.next + ", " + node.previous + ", " + node.parent + ", " + previous);
			node.next = this;
			previous = node;
			node.parent = parent;
			parent.firstChild = node;
		}

		void insertChild(Node node) {
			if (firstChild == null) {
				firstChild = node;
				degree = node.degree + 1;
			} else {
				firstChild.insert(node);
				if (node.degree + 1 > degree)
					degree = node.degree + 1;
			}
		}

		final void extract() {
			if (parent != null && parent.firstChild == this)
				parent.firstChild = next;
			if (next != null)
				next.previous = previous;
			if (previous != null)
				previous.next = next;
			previous = next = parent = null;
		}

		public void print(String label) {
			print(label, "");
		}

		public void print(String label, String indent) {
			System.out.println(indent + label + ": " + key + ", " + object);
			int i = 1;
			for (Node n = firstChild; n != null; n = n.next)
				n.print(label + ":" + (i++), indent + "    ");
		}
	}

	private Node root;
	private Node min;
	int count;

	public FibonacciHeap() {
		root = new Node(null, null, null);
	}

	public void add(Comparable key, Object object) {
		Node node = new Node(key, object, root);
		if (min == null || min.key.compareTo(key) > 0)
			min = node;
		root.insertChild(node);
		count++;
	}

	public Object pop() {
		if (min == null)
			return null;
		// put all children on the root list
		for (Node node = min.firstChild; node != null; ) {
			Node next = node.next;
			// no need to extract(), since the whole list goes
			node.parent = node.next = node.previous = null;
			root.firstChild.insert(node);
			node = next;
		}
		Node ret = min;
		if (root.firstChild == min) {
			root.firstChild = min.next;
			if (min.next != null)
				min.next.previous = null;
		} else {
			if (min.next != null)
				min.next.previous = min.previous;
			if (min.previous != null)
				min.previous.next = min.next;
		}
		if (root.firstChild != null)
			consolidate();
		else
			min = null;
		count--;
		return ret.object;
	}

	public boolean hasMore() {
		return root.firstChild != null;
	}

	public int compareTo(Object other) {
		return min == null ? 1 : min.key.compareTo(other);
	}

	final private Node link(Node a, Node b) {
		if (a.key.compareTo(b.key) > 0)
			return link(b, a);
		b.extract();
		b.parent = a;
		a.insertChild(b);
		return a;
	}

	final private void insert(Node[] list, Node node) {
		if (list[node.degree] == null)
			list[node.degree] = node;
		else {
			int oldDegree = node.degree;
			node = link(node, list[oldDegree]);
			list[oldDegree] = null;
			insert(list, node);
		}
	}

	final private void consolidate() {
		int maxDegree = 1;
		for (int i = 1; i <= count; i *= 2)
			maxDegree++;
		Node[] list = new Node[maxDegree];
		for (Node n = root.firstChild; n != null; ) {
			Node next = n.next;
			n.extract();
			n.parent = root;
			insert(list, n);
			n = next;
		}
		Node last = null;
		root.firstChild = null;
		for (int i = 0; i < maxDegree; i++)
			if (list[i] != null) {
				if (last == null) {
					last = root.firstChild = list[i];
					last.previous = last.next = null;
					min = last;
				} else {
					last.next = list[i];
					list[i].previous = last;
					last = list[i];
					last.next = null;
					if (min.key.compareTo(last.key) > 0)
						min = last;
				}
			}
	}

	public static void main(String[] args) {
		FibonacciHeap heap = new FibonacciHeap();
		double[] prios = {
			9, -5, Math.PI, 132, 15.223, 9e5, 1997, 0.001, 0.0012, 0
		};
		for (int i = 0; i < prios.length; i++) {
			Double p = new Double(prios[i]);
			heap.add(p, p);
		}
		int i = 0;
		while (heap.hasMore()) {
			i++;
			System.out.println("Extract " + i + ": " +
					(Double)heap.pop());
		}
	}
}
