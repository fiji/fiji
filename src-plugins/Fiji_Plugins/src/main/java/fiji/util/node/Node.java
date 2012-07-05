package fiji.util.node;

public interface Node<N extends Node<N>>
{
	public boolean isLeaf();
}
