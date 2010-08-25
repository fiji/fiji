/**
 * Graph cut implementation for images.
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

import java.util.LinkedList;
import java.util.List;

/**
 * The two possible segments, represented as special terminal nodes in the graph.
 */
enum Terminal {

	FOREGROUND, // a.k.a. the source
	BACKGROUND; // a.k.a. the sink
}

/**
 * Class implementing the grach cut algorithm.
 */
public class GraphCut {

	// number of nodes
	private int numNodes;

	// maximum number of edges
	private int numEdges;

	// list of all nodes in the graph
	private Node[] nodes;

	// list of all edges in the graph
	private Edge[] edges;


	// internal counter for edge creation
	private int edgeNum;

	// the total flow in the whole graph
	private float totalFlow;

	// counter for the numbers of iterations to maxflow
	private int maxflowIteration;

	// Lists of active nodes: activeQueueFirst points to first
	// elements of the lists, activeQueueLast to the last ones.
	// In between, nodes are connected via reference to next node
	// in each node.
	private Node[] activeQueueFirst;
	private Node[] activeQueueLast;

	// list of orphans
	private LinkedList<Node> orphans;

	// counter for iterations of main loop
	private int time;

	/**
	 * Initialises the graph cut implementation and allocates the memory needed
	 * for the given number of nodes and edges.
	 *
	 * @param numNodes The number of nodes that should be created.
	 * @param numEdges The number of edges that you can add. A directed edge and its 
	 *                 counterpart (i.e., the directed edge in the other
	 *                 direction) count as one edge.
	 */
	public GraphCut(int numNodes, int numEdges) {

		this.numNodes  = numNodes;
		this.numEdges  = numEdges;

		this.nodes     = new Node[numNodes];
		this.edges     = new Edge[2*numEdges];

		this.edgeNum   = 0;

		this.totalFlow = 0;

		this.maxflowIteration = 0;

		this.activeQueueFirst = new Node[2];
		this.activeQueueLast  = new Node[2];

		this.orphans = new LinkedList<Node>();

		assert(numNodes > 0);

		for (int i = 0; i < numNodes; i++)
			nodes[i] = new Node();
	}

	/**
	 * Set the affinity for one node to belong to the foreground (i.e., source)
	 * or background (i.e., sink).
	 *
	 * @param nodeId The number of the node.
	 * @param source The affinity of this node to the foreground (i.e., source)
	 * @param sink   The affinity of this node to the background (i.e., sink)
	 */
	public void setTerminalWeights(int nodeId, float source, float sink) {

		assert(nodeId >= 0 && nodeId < numNodes);

		float delta = nodes[nodeId].getResidualCapacity();

		if (delta > 0)
			source += delta;
		else
			sink   -= delta;

		totalFlow += (source < sink) ? source : sink;

		nodes[nodeId].setResidualCapacity(source - sink);
	}

	/**
	 * Set the edge weight of an undirected edge between two nodes.
	 *
	 * Please note that you cannot call any <tt>setEdgeWeight</tt> more often
	 * than the number of edges you specified at the time of construction!
	 *
	 * @param nodeId1 The first node.
	 * @param nodeId2 The second node.
	 * @param weight  The weight (i.e., the cost) of the connecting edge.
	 */
	public void setEdgeWeight(int nodeId1, int nodeId2, float weight) {

		setEdgeWeight(nodeId1, nodeId2, weight, weight);
	}

	/**
	 * Set the edge weight of a pair of directed edges between two nodes.
	 *
	 * Please note that you cannot call any <tt>setEdgeWeight</tt> more often
	 * than the number of edges you specified at the time of construction!
	 *
	 * @param nodeId1    The first node.
	 * @param nodeId2    The second node.
	 * @param weight1to2 The weight (i.e., the cost) of the directed edge from
	 *                   node1 to node2.
	 * @param weight2to1 The weight (i.e., the cost) of the directed edge from
	 *                   node2 to node1.
	 */
	public void setEdgeWeight(int nodeId1, int nodeId2, float weight1to2, float weight2to1) {

		assert(nodeId1 >= 0 && nodeId1 < numNodes);
		assert(nodeId2 >= 0 && nodeId2 < numNodes);
		assert(nodeId1 != nodeId2);
		assert(weight1to2 >= 0);
		assert(weight2to1 >= 0);
		assert(edgeNum < numEdges - 2);

		// create new edges
		Edge edge        = new Edge();
		edges[edgeNum]   = edge; edgeNum++;
		Edge reverseEdge = new Edge();
		edges[edgeNum]   = reverseEdge; edgeNum++;

		// get the nodes
		Node node1       = nodes[nodeId1];
		Node node2       = nodes[nodeId2];

		// link edges
		edge.setSister(reverseEdge);
		reverseEdge.setSister(edge);

		// add node1 to edge
		edge.setNext(node1.getFirstOutgoing());
		node1.setFirstOutgoing(edge);

		// add node2 to reverseEdge
		reverseEdge.setNext(node2.getFirstOutgoing());
		node2.setFirstOutgoing(reverseEdge);

		// set targets of edges
		edge.setHead(node2);
		reverseEdge.setHead(node1);

		// set residual capacities
		edge.setResidualCapacity(weight1to2);
		reverseEdge.setResidualCapacity(weight2to1);
	}

	/**
	 * Performs the actual max-flow/min-cut computation.
	 *
	 * @param reuseTrees   reuse trees of a previos call
	 * @param changedNodes list of nodes that potentially changed their
	 *                     segmentation compared to a previous call, can be set
	 *                     to <tt>null</tt>
	 */
	public float computeMaximumFlow(boolean reuseTrees, List<Integer> changedNodes) {

		if (maxflowIteration == 0)
			reuseTrees = false;

		if (reuseTrees)
			maxflowReuseTreesInit();
		else
			maxflowInit();

		Node currentNode = null;
		Edge edge        = null;

		// main loop
		while (true) {

			Node activeNode = currentNode;

			if (activeNode != null) {
				// remove active flag
				activeNode.setNext(null);
				if (activeNode.getParent() == null)
					activeNode = null;
			}
			if (activeNode == null) {
				activeNode = getNextActiveNode();
				if (activeNode == null)
					// no more active nodes - we're done here
					break;
			}

			// groth
			if (!activeNode.isInSink()) {
				// grow source tree
				for (edge = activeNode.getFirstOutgoing(); edge != null; edge = edge.getNext()) {
					if (edge.getResidualCapacity() != 0) {

						Node headNode = edge.getHead();

						if (headNode.getParent() == null) {
							// free node found, add to source tree
							headNode.setInSink(false);
							headNode.setParent(edge.getSister());
							headNode.setTimestamp(activeNode.getTimestamp());
							headNode.setDistance(activeNode.getDistance() + 1);
							setNodeActive(headNode);
							addToChangedList(headNode);

						} else if (headNode.isInSink()) {
							// node is not free and belongs to other tree - path
							// via edge found
							break;

						} else if (headNode.getTimestamp() <= activeNode.getTimestamp() &&
						           headNode.getDistance()  >  activeNode.getDistance()) {
							// node is not free and belongs to our tree - try to
							// shorten its distance to the source
							headNode.setParent(edge.getSister());
							headNode.setTimestamp(activeNode.getTimestamp());
							headNode.setDistance(activeNode.getDistance() + 1);
						}
					}
				}
			} else {
				// activeNode is in sink, grow sink tree
				for (edge = activeNode.getFirstOutgoing(); edge != null; edge = edge.getNext()) {
					if (edge.getSister().getResidualCapacity() != 0) {

						Node headNode = edge.getHead();

						if (headNode.getParent() == null) {
							// free node found, add to sink tree
							headNode.setInSink(true);
							headNode.setParent(edge.getSister());
							headNode.setTimestamp(activeNode.getTimestamp());
							headNode.setDistance(activeNode.getDistance() + 1);
							setNodeActive(headNode);
							addToChangedList(headNode);

						} else if (!headNode.isInSink()) {
							// node is not free and belongs to other tree - path
							// via edge's sister found
							edge = edge.getSister();
							break;

						} else if (headNode.getTimestamp() <= activeNode.getTimestamp() &&
						           headNode.getDistance()  >  activeNode.getDistance()) {
							// node is not free and belongs to our tree - try to
							// shorten its distance to the sink
							headNode.setParent(edge.getSister());
							headNode.setTimestamp(activeNode.getTimestamp());
							headNode.setDistance(activeNode.getDistance() + 1);
						}
					}
				}
			}

			time++;

			if (edge != null) {
				// we found a path via edge

				// set active flag
				activeNode.setNext(activeNode);
				currentNode = activeNode;

				// augmentation
				augment(edge);

				// adoption
				while (orphans.size() > 0) {
					Node orphan = orphans.poll();
					if (orphan.isInSink())
						processSinkOrphan(orphan);
					else
						processSourceOrphan(orphan);
				}
			} else {
				// no path found
				currentNode = null;
			}
		}

		maxflowIteration++;

		// create list of changed nodes
		if (changedNodes != null) {
			changedNodes.clear();
			for (int i = 0; i < nodes.length; i++)
				if (nodes[i].isInChangedList())
					changedNodes.add(i);
		}

		return totalFlow;
	}

	/**
	 * Get the segmentation, i.e., the terminal node that is connected to the
	 * specified node. If there are several min-cut solutions, free nodes are
	 * assigned to the background.
	 *
	 * @param nodeId the node to check
	 * @return Either <tt>Terminal.FOREGROUND</tt> or
	 *         <tt>Terminal.BACKGROUND</tt>
	 */
	public Terminal getTerminal(int nodeId) {

		assert(nodeId >= 0 && nodeId < numNodes);

		Node node = nodes[nodeId];

		if (node.getParent() != null)
			return node.isInSink() ? Terminal.BACKGROUND : Terminal.FOREGROUND;
		else
			return Terminal.BACKGROUND;
	}

	/**
	 * Gets the number of nodes in this graph.
	 *
	 * @return The number of nodes
	 */
	public int getNumNodes() {
		return this.numNodes;
	}

	/**
	 * Gets the number of edges in this graph.
	 *
	 * @return The number of edges.
	 */
	public int getNumEdges() {
		return this.numEdges;
	}

	/**
	 * Mark a node as being changed.
	 *
	 * Use this method if the graph weights changed after a previous computation
	 * of the max-flow. The next computation will be faster by just considering
	 * changed nodes.
	 *
	 * A node has to be considered changed if any of its adjacent edges changed
	 * its weight.
	 *
	 * @param nodeId The node that changed.
	 */
	public void markNode(int nodeId) {

		assert(nodeId >= 0 && nodeId < numNodes);

		Node node = nodes[nodeId];

		if (node.getNext() == null) {
			if (activeQueueLast[1] != null)
				activeQueueLast[1].setNext(node);
			else
				activeQueueFirst[1] = node;

			activeQueueLast[1] = node;
			node.setNext(node);
		}

		node.setMarked(true);
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Marks a node as being active and adds it to second queue of active nodes.
	 */
	private void setNodeActive(Node node) {

		if (node.getNext() == null) {
			if (activeQueueLast[1] != null)
				activeQueueLast[1].setNext(node);
			else
				activeQueueFirst[1] = node;

			activeQueueLast[1] = node;
			node.setNext(node);
		}
	}

	/**
	 * Gets the next active node, that is, the first node of the first queue of
	 * active nodes. If this queue is empty, the second queue is used. Returns
	 * <tt>nyll</tt>, if no active node is left.
	 */
	private Node getNextActiveNode() {

		Node node;

		while (true) {

			node = activeQueueFirst[0];

			if (node == null) {
				// queue 0 was empty, try other one
				node = activeQueueFirst[1];

				// swap queues
				activeQueueFirst[0] = activeQueueFirst[1];
				activeQueueLast[0]  = activeQueueLast[1];
				activeQueueFirst[1] = null;
				activeQueueLast[1]  = null;

				// if other queue was emtpy as well, return null
				if (node == null)
					return null;
			}

			// remove current node from active list
			if (node.getNext() == node) {
				// this was the last one
				activeQueueFirst[0] = null;
				activeQueueLast[0]  = null;
			} else
				activeQueueFirst[0] = node.getNext();

			// not in any list anymore
			node.setNext(null);

			// return only if it has a parent and is therefore active
			if (node.getParent() != null)
				return node;
		}
	}

	/**
	 * Mark a node as orphan and add it to the front of the queue.
	 */
	private void addOrphanAtFront(Node node) {

		node.setParent(Edge.ORPHAN);

		orphans.addFirst(node);
	}

	/**
	 * Mark a node as orphan and add it to the back of the queue.
	 */
	private void addOrphanAtBack(Node node) {

		node.setParent(Edge.ORPHAN);

		orphans.addLast(node);
	}

	/**
	 * Add a node to the list of potentially changed nodes.
	 */
	private void addToChangedList(Node node) {

		node.setInChangedList(true);
	}

	/**
	 * Initialise the algorithm.
	 *
	 * Only called if <tt>reuseTrees</tt> is false.
	 */
	private void maxflowInit() {

		activeQueueFirst[0] = null;
		activeQueueLast[0]  = null;
		activeQueueFirst[1] = null;
		activeQueueLast[1]  = null;

		orphans.clear();

		time = 0;

		for (Node node : nodes) {

			node.setNext(null);
			node.setMarked(false);
			node.setInChangedList(false);
			node.setTimestamp(time);

			if (node.getResidualCapacity() > 0) {
				// node is connected to source
				node.setInSink(false);
				node.setParent(Edge.TERMINAL);
				setNodeActive(node);
				node.setDistance(1);
			} else if (node.getResidualCapacity() < 0) {
				// node is connected to sink
				node.setInSink(true);
				node.setParent(Edge.TERMINAL);
				setNodeActive(node);
				node.setDistance(1);
			} else {
				node.setParent(null);
			}
		}
	}

	/**
	 * Initialise the algorithm.
	 *
	 * Only called if <tt>reuseTrees</tt> is true.
	 */
	private void maxflowReuseTreesInit() {

		Node node1;
		Node node2;

		Node queueStart = activeQueueFirst[1];

		Edge edge;

		activeQueueFirst[0] = null;
		activeQueueLast[0]  = null;
		activeQueueFirst[1] = null;
		activeQueueLast[1]  = null;

		orphans.clear();

		time++;

		while ((node1 = queueStart) != null) {

			queueStart = node1.getNext();

			if (queueStart == node1)
				queueStart = null;

			node1.setNext(null);
			node1.setMarked(false);
			setNodeActive(node1);

			if (node1.getResidualCapacity() == 0) {
				if (node1.getParent() != null)
					addOrphanAtBack(node1);
				continue;
			}

			if (node1.getResidualCapacity() > 0) {

				if (node1.getParent() == null || node1.isInSink()) {

					node1.setInSink(false);
					for (edge = node1.getFirstOutgoing(); edge != null; edge = edge.getNext()) {

						node2 = edge.getHead();
						if (!node2.isMarked()) {
							if (node2.getParent() == edge.getSister())
								addOrphanAtBack(node2);
							if (node2.getParent() != null && node2.isInSink() && edge.getResidualCapacity() > 0)
								setNodeActive(node2);
						}
					}
					addToChangedList(node1);
				}
			} else {

				if (node1.getParent() == null || !node1.isInSink()) {

					node1.setInSink(true);
					for (edge = node1.getFirstOutgoing(); edge != null; edge = edge.getNext()) {

						node2 = edge.getHead();
						if (!node2.isMarked()) {
							if (node2.getParent() == edge.getSister())
								addOrphanAtBack(node2);
							if (node2.getParent() != null && !node2.isInSink() && edge.getSister().getResidualCapacity() > 0)
								setNodeActive(node2);
						}
					}
					addToChangedList(node1);
				}
			}
			node1.setParent(Edge.TERMINAL);
			node1.setTimestamp(time);
			node1.setDistance(1);
		}

		// adoption
		while (orphans.size() > 0) {
			Node orphan = orphans.poll();
			if (orphan.isInSink())
				processSinkOrphan(orphan);
			else
				processSourceOrphan(orphan);
		}
	}

	/**
	 * Perform the augmentation step of the graph cut algorithm.
	 *
	 * This is done whenever a path between the source and the sink was found.
	 */
	private void augment(Edge middle) {

		Node node;
		Edge edge;

		float bottleneck;

		// 1. find bottleneck capacity

		// 1a - the source tree
		bottleneck = middle.getResidualCapacity();
		for (node = middle.getSister().getHead(); ; node = edge.getHead()) {

			edge = node.getParent();

			if (edge == Edge.TERMINAL)
				break;
			if (bottleneck > edge.getSister().getResidualCapacity())
				bottleneck = edge.getSister().getResidualCapacity();
		}

		if (bottleneck > node.getResidualCapacity())
			bottleneck = node.getResidualCapacity();
		
		// 1b - the sink tree
		for (node = middle.getHead(); ; node = edge.getHead()) {

			edge = node.getParent();

			if (edge == Edge.TERMINAL)
				break;
			if (bottleneck > edge.getResidualCapacity())
				bottleneck = edge.getResidualCapacity();
		}
		if (bottleneck > -node.getResidualCapacity())
			bottleneck = -node.getResidualCapacity();

		// 2. augmenting

		// 2a - the source tree
		middle.getSister().setResidualCapacity(middle.getSister().getResidualCapacity() + bottleneck);
		middle.setResidualCapacity(middle.getResidualCapacity() - bottleneck);
		for (node = middle.getSister().getHead(); ; node = edge.getHead()) {

			edge = node.getParent();

			if (edge == Edge.TERMINAL) {
				// end of path
				break;
			}
			edge.setResidualCapacity(edge.getResidualCapacity() + bottleneck);
			edge.getSister().setResidualCapacity(edge.getSister().getResidualCapacity() - bottleneck);
			if (edge.getSister().getResidualCapacity() == 0)
				addOrphanAtFront(node);
		}
		node.setResidualCapacity(node.getResidualCapacity() - bottleneck);
		if (node.getResidualCapacity() == 0)
			addOrphanAtFront(node);

		// 2b - the sink tree
		for (node = middle.getHead(); ; node = edge.getHead()) {

			edge = node.getParent();

			if (edge == Edge.TERMINAL) {
				// end of path
				break;
			}
			edge.getSister().setResidualCapacity(edge.getSister().getResidualCapacity() + bottleneck);
			edge.setResidualCapacity(edge.getResidualCapacity() - bottleneck);
			if (edge.getResidualCapacity() == 0)
				addOrphanAtFront(node);
		}
		node.setResidualCapacity(node.getResidualCapacity() + bottleneck);
		if (node.getResidualCapacity() == 0)
			addOrphanAtFront(node);

		totalFlow += bottleneck;
	}

	/**
	 * Adopt an orphan.
	 */
	private void processSourceOrphan(Node orphan) {

		Edge bestEdge    = null;
		int  minDistance = Integer.MAX_VALUE;

		for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext())
			if (orphanEdge.getSister().getResidualCapacity() != 0) {

				Node node       = orphanEdge.getHead();
				Edge parentEdge = node.getParent();

				if (!node.isInSink() && parentEdge != null) {

					// check the origin of node
					int distance = 0;
					while (true) {

						if (node.getTimestamp() == time) {
							distance += node.getDistance();
							break;
						}
						parentEdge = node.getParent();
						distance++;
						if (parentEdge == Edge.TERMINAL) {
							node.setTimestamp(time);
							node.setDistance(1);
							break;
						}
						if (parentEdge == Edge.ORPHAN) {
							distance = Integer.MAX_VALUE;
							break;
						}
						// otherwise, proceed to the next node
						node = parentEdge.getHead();
					}
					if (distance < Integer.MAX_VALUE) { // node originates from the source

						if (distance < minDistance) {
							bestEdge    = orphanEdge;
							minDistance = distance;
						}
						// set marks along the path
						for (node = orphanEdge.getHead(); node.getTimestamp() != time; node = node.getParent().getHead()) {

							node.setTimestamp(time);
							node.setDistance(distance);
							distance--;
						}
					}
				}
			}

		orphan.setParent(bestEdge);
		if (bestEdge != null) {
			orphan.setTimestamp(time);
			orphan.setDistance(minDistance + 1);
		} else {
			// no parent found
			addToChangedList(orphan);

			// process neighbors
			for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {

				Node node = orphanEdge.getHead();
				Edge parentEdge = node.getParent();
				if (!node.isInSink() && parentEdge != null) {

					if (orphanEdge.getSister().getResidualCapacity() != 0)
						setNodeActive(node);
					if (parentEdge != Edge.TERMINAL && parentEdge != Edge.ORPHAN && parentEdge.getHead() == orphan)
						addOrphanAtBack(node);
				}
			}
		}

	}

	/**
	 * Adopt an orphan.
	 */
	private void processSinkOrphan(Node orphan) {

		Edge bestEdge    = null;
		int  minDistance = Integer.MAX_VALUE;

		for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext())
			if (orphanEdge.getResidualCapacity() != 0) {

				Node node       = orphanEdge.getHead();
				Edge parentEdge = node.getParent();

				if (node.isInSink() && parentEdge != null) {

					// check the origin of node
					int distance = 0;
					while (true) {

						if (node.getTimestamp() == time) {
							distance += node.getDistance();
							break;
						}
						parentEdge = node.getParent();
						distance++;
						if (parentEdge == Edge.TERMINAL) {
							node.setTimestamp(time);
							node.setDistance(1);
							break;
						}
						if (parentEdge == Edge.ORPHAN) {
							distance = Integer.MAX_VALUE;
							break;
						}
						// otherwise, proceed to the next node
						node = parentEdge.getHead();
					}
					if (distance < Integer.MAX_VALUE) {
						// node originates from the sink
						if (distance < minDistance) {
							bestEdge    = orphanEdge;
							minDistance = distance;
						}
						// set marks along the path
						for (node = orphanEdge.getHead(); node.getTimestamp() != time; node = node.getParent().getHead()) {

							node.setTimestamp(time);
							node.setDistance(distance);
							distance--;
						}
					}
				}
			}

		orphan.setParent(bestEdge);
		if (bestEdge != null) {
			orphan.setTimestamp(time);
			orphan.setDistance(minDistance + 1);
		} else {
			// no parent found
			addToChangedList(orphan);

			// process neighbors
			for (Edge orphanEdge = orphan.getFirstOutgoing(); orphanEdge != null; orphanEdge = orphanEdge.getNext()) {

				Node node = orphanEdge.getHead();
				Edge parentEdge = node.getParent();
				if (node.isInSink() && parentEdge != null) {

					if (orphanEdge.getResidualCapacity() != 0)
						setNodeActive(node);
					if (parentEdge != Edge.TERMINAL && parentEdge != Edge.ORPHAN && parentEdge.getHead() == orphan)
						addOrphanAtBack(node);
				}
			}
		}
	}
}
