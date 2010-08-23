/**
* Class to represent pixel coherence as directed edges in
* the graph.
*/
public class Edge {

	// node the edge points to
	private Node head;

	// nect edge with the same originating node
	private Edge next;

	// reverse arc
	private Edge sister;

	// residual capacity of this edge
	private float residualCapacity;

	// special edges
	public final static Edge TERMINAL = new Edge();
	public final static Edge ORPHAN   = new Edge();

	public Edge() {

		head   = null;
		next   = null;
		sister = null;

		residualCapacity = 0;
	}

	/**
	 * Gets the head, i.e., the node this edge points to for this edge.
	 *
	 * @return The head, i.e., the node this edge points to.
	 */
	public Node getHead()
	{
		return this.head;
	}

	/**
	 * Sets the head for this edge.
	 *
	 * @param head The head, i.e., the node this edge shall point to.
	 */
	public void setHead(Node head)
	{
		this.head = head;
	}

	/**
	 * Sets the next edge for this edge.
	 *
	 * @param next The next edge.
	 */
	public void setNext(Edge next)
	{
		this.next = next;
	}

	/**
	 * Sets the sister for this instance.
	 *
	 * @param sister The sister.
	 */
	public void setSister(Edge sister)
	{
		this.sister = sister;
	}

	/**
	 * Sets the residual capacity for this instance.
	 *
	 * @param residualCapacity The residual capacity.
	 */
	public void setResidualCapacity(float residualCapacity)
	{
		this.residualCapacity = residualCapacity;
	}

	/**
	 * Gets the residual capacity for this instance.
	 *
	 * @return The residualCapacity.
	 */
	public float getResidualCapacity()
	{
		return this.residualCapacity;
	}

	/**
	 * Gets the sister for this instance.
	 *
	 * @return The sister.
	 */
	public Edge getSister()
	{
		return this.sister;
	}

	/**
	 * Gets the next for this instance.
	 *
	 * @return The next.
	 */
	public Edge getNext()
	{
		return this.next;
	}
}

