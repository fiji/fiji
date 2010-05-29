package fiji.util.node;

public class NonLeaf<T extends Leaf<T>> implements Node<T>
{
	/* the axis of 'coordinate' is the depth modulo the dimension */
	final public float coordinate;
	final public Node<T> left, right;
	final int dimension;

	public NonLeaf(final float coordinate, final int dimension, final Node<T> left, final Node<T> right) {
		this.coordinate = coordinate;
		this.left = left;
		this.right = right;
		this.dimension = dimension;
	}

	public boolean isLeaf() {
		return false;
	}

	public String toString(final Node<T> node) {
		if (node == null)
			return "null";
		if (node instanceof Leaf) {
			String result = "(" + ((Leaf<?>)node).get(0);

			for (int i = 1; i < dimension; i++)
				result += ", " + ((Leaf<?>)node).get(i);

			return result + ")";
		}

		if (node instanceof NonLeaf) {
			NonLeaf<T> nonLeaf = (NonLeaf<T>)node;
			return "[" + toString( nonLeaf.left ) + " |{" + nonLeaf.coordinate + "} " + toString(nonLeaf.right) + "]";
		}

		return node.toString();
	}

	public String toString() {
		return toString(this);
	}
}
