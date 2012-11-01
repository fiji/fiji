package fiji.util.node;

public interface Leaf<N extends Leaf<N>> extends Node<N>
{
	/* get the k'th component of the vector */
	float get(int k);
	float distanceTo(N other);

	int getNumDimensions();

	N[] createArray(int n);
}
