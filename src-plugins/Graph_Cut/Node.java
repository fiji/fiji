/**
* Class to represent a pixel as a node in a graph.
*
* This implementation was <b>heavily</b> inspired by the implementation
* provided by Kolmogorov and Boykov: MAXFLOW version 3.01.
*
* From the README of the library:
*
*	This software library implements the maxflow algorithm described in
*
*	"An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy
*	Minimization in Vision."
*	Yuri Boykov and Vladimir Kolmogorov.
*	In IEEE Transactions on Pattern Analysis and Machine Intelligence
*	(PAMI), 
*	September 2004
*
*	This algorithm was developed by Yuri Boykov and Vladimir Kolmogorov
*	at Siemens Corporate Research. To make it available for public
*	use, it was later reimplemented by Vladimir Kolmogorov based on open
*	publications.
*
*	If you use this software for research purposes, you should cite
*	the aforementioned paper in any resulting publication.
*
* @author Jan Funke <jan.funke@inf.tu-dresden.de>
* @version 0.1
*/

public class Node {

	// first outgoing edge
	private Edge firstOutgoing;

	// parent (in the tree structure)
	private Edge parent;

	// next active node
	private Node next;

	
	// timestamp indicating when distance was computed
	private int timestamp;

	// distance to the terminal
	private int distance;

	// indicates whether this node belongs to the sink or the source tree
	private boolean inSink;

	// indicates whether this node was changed
	private boolean marked;

	// indicates whether this node is in the changed list
	private boolean inChangedList;

	// the residual capacity of this node to the sink (<0) or from the source
	// (>0)
	private float residualCapacity;

	public Node() {

		firstOutgoing = null;
		parent        = null;
		next          = null;

		timestamp     = 0;
		distance      = 0;
		inSink        = false;
		marked        = false;

		residualCapacity = 0;
	}

	/**
	 * Gets the first outgoing node of this node.
	 *
	 * @return The first outgoing node
	 */
	public Edge getFirstOutgoing() {
		return this.firstOutgoing;
	}

	/**
	 * Sets the firstOutgoing for this instance.
	 *
	 * @param firstOutgoing The firstOutgoing.
	 */
	public void setFirstOutgoing(Edge firstOutgoing) {
		this.firstOutgoing = firstOutgoing;
	}

	/**
	 * Gets the parent of this node in the tree structure
	 *
	 * @return The parent of this node.
	 */
	public Edge getParent() {
		return this.parent;
	}

	/**
	 * Sets the parent for this node.
	 *
	 * @param parent The new parent.
	 */
	public void setParent(Edge parent) {
		this.parent = parent;
	}

	/**
	 * Gets the next active node.
	 *
	 * @return The next active node.
	 */
	public Node getNext() {
		return this.next;
	}

	/**
	 * Sets the next node for this node.
	 *
	 * @param next The next node.
	 */
	public void setNext(Node next) {
		this.next = next;
	}

	/**
	 * Gets the timestamp for this node.
	 *
	 * @return The timestamp.
	 */
	public int getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the timestamp for this instance.
	 *
	 * @param timestamp The timestamp.
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets the distance of this node to source/sink.
	 *
	 * @return The distance.
	 */
	public int getDistance() {
		return this.distance;
	}

	/**
	 * Sets the distance of this node to source/sink.
	 *
	 * @param distance The distance.
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}

	/**
	 * Determines if this node is connected to the sink.
	 *
	 * @return <tt>true</tt>, if this node is connected to the sink.
	 */
	public boolean isInSink() {
		return this.inSink;
	}

	/**
	 * Sets whether or not this instance is connected to the sink.
	 *
	 * @param inSink <tt>true</tt>, if this node is connected to the sink.
	 */
	public void setInSink(boolean inSink) {
		this.inSink = inSink;
	}

	/**
	 * Determines if this node is marked.
	 *
	 * @return <tt>true</tt>, if this node is marked.
	 */
	public boolean isMarked() {
		return this.marked;
	}

	/**
	 * Sets whether or not this instance is marked.
	 *
	 * @param marked <tt>true</tt> to mark this node
	 */
	public void setMarked(boolean marked) {
		this.marked = marked;
	}

	/**
	 * Sets the residualCapacity for this node.
	 *
	 * @param residualCapacity The residual capacity.
	 */
	public void setResidualCapacity(float residualCapacity) {
		this.residualCapacity = residualCapacity;
	}

	/**
	 * Determines if this instance is in changedNodes.
	 *
	 * @return <tt>true</tt>, if this node is in changedNodes.
	 */
	public boolean isInChangedList() {
		return this.inChangedList;
	}

	/**
	 * Sets whether or not this instance is in changedNodes.
	 *
	 * @param inChangedList <tt>true</tt>, if this node is in changedNodes
	 */
	public void setInChangedList(boolean inChangedList) {
		this.inChangedList = inChangedList;
	}

	/**
	 * Gets the residual capacity for this node.
	 *
	 * @return The residual capacity.
	 */
	public float getResidualCapacity() {
		return this.residualCapacity;
	}
}
