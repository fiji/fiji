package util;

/*
 * This class does not implement a complete Fibonacci Heap:
 *
 * Since we will use it as a PriorityQueue, where (key,element)
 * pairs will not be changed, but only pushed into, and popped out
 * of the queue, we do not need DecreaseKey, Union, and Cut.
 */

public class FibonacciHeap<T extends Comparable<T>> implements Comparable<T>
{
	private static class Node<T extends Comparable<T>> {
		T key;
		Object object;
		Node<T> next, previous, parent, firstChild;
		int degree;
		boolean marked;

		public Node(T key, Object object, Node<T> parent) {
			this.key = key;
			this.object = object;
			this.parent = parent;
		}

		void insert(Node<T> node) {
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

		void insertChild(Node<T> node) {
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
			for (Node<T> n = firstChild; n != null; n = n.next)
				n.print(label + ":" + (i++), indent + "    ");
		}
	}

	private Node<T> root;
	private Node<T> min;
	int count;

	public FibonacciHeap() {
		root = new Node<T>(null, null, null);
	}

	public void add(T key, Object object) {
		Node<T> node = new Node<T>(key, object, root);
		if (min == null || min.key.compareTo(key) > 0)
			min = node;
		root.insertChild(node);
		count++;
	}

	public Object pop() {
		if (min == null)
			return null;
		// put all children on the root list
		for (Node<T> node = min.firstChild; node != null; ) {
			Node<T> next = node.next;
			// no need to extract(), since the whole list goes
			node.parent = node.next = node.previous = null;
			root.firstChild.insert(node);
			node = next;
		}
		Node<T> ret = min;
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

	public int compareTo(T other) {
		return min == null ? 1 : min.key.compareTo(other);
	}

	final private Node<T> link(Node<T> a, Node<T> b) {
		if (a.key.compareTo(b.key) > 0)
			return link(b, a);
		b.extract();
		b.parent = a;
		a.insertChild(b);
		return a;
	}

	final private void insert(Node<T>[] list, Node<T> node) {
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
		@SuppressWarnings("unchecked")
		Node<T>[] list = new Node[maxDegree];
		for (Node<T> n = root.firstChild; n != null; ) {
			Node<T> next = n.next;
			n.extract();
			n.parent = root;
			insert(list, n);
			n = next;
		}
		Node<T> last = null;
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
		FibonacciHeap<Double> heap = new FibonacciHeap<Double>();
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
