package graphcut;

/**
 * Class wrapping some basic structures that are used to represent a graph.
 */

public class Graph {

	private int numNodes;
	private int numEdges;

	// special index assigment
	public final static int NONE     = -1;
	public final static int TERMINAL = -2;
	public final static int ORPHAN   = -3;

	/////////////////////////
	// node representation //
	/////////////////////////

	// first outgoing edge
	private int[] firstOutgoings;

	// parent (in the tree structure)
	private int[] parents;

	// next active node
	private int[] nextNodes;

	
	// timestamp indicating when distance was computed
	private int[] timestamps;

	// distance to the terminal
	private int[] distances;

	// indicates whether this node belongs to the sink or the source tree
	private boolean[] inSink;

	// indicates whether this node was changed
	private boolean[] marked;

	// indicates whether this node is in the changed list
	private boolean[] inChangedList;

	// the residual capacity of this node to the sink (<0) or from the source
	// (>0)
	private float[] residualNodeCapacities;

	/////////////////////////
	// edge representation //
	/////////////////////////

	// node the edge points to
	private int[] heads;

	// next edge with the same originating node
	private int[] nextEdges;

	// reverse arc
	private int[] sisters;

	// residual capacity of this edge
	private float[] residualEdgeCapacities;

	public Graph(int numNodes, int numEdges) {

		this.numNodes = numNodes;
		this.numEdges = numEdges;

		// allocate node data
		firstOutgoings         = new int[numNodes];
		parents                = new int[numNodes];
		nextNodes              = new int[numNodes];
		timestamps             = new int[numNodes];
		distances              = new int[numNodes];
		inSink                 = new boolean[numNodes];
		marked                 = new boolean[numNodes];
		inChangedList          = new boolean[numNodes];
		residualNodeCapacities = new float[2*numEdges];

		// allocate edge data
		heads                  = new int[2*numEdges];
		nextEdges              = new int[2*numEdges];
		sisters                = new int[2*numEdges];
		residualEdgeCapacities = new float[2*numEdges];

		// initialise node data
		for (int i = 0; i < numNodes; i++) {
			firstOutgoings[i]         = NONE;
			parents[i]                = NONE;
			nextNodes[i]              = NONE;
			timestamps[i]             = 0;
			distances[i]              = 0;
			inSink[i]                 = false;
			marked[i]                 = false;
			residualNodeCapacities[i] = 0;
		}

		// initialise edge data
		for (int i = 0; i < 2*numEdges; i++) {
			heads[i]                  = NONE;
			nextEdges[i]              = NONE;
			sisters[i]                = NONE;
			residualEdgeCapacities[i] = 0;
		}
	}

	public final float getResidualNodeCapacity(int node) {
		return residualNodeCapacities[node];
	}

	public final void setResidualNodeCapacity(int node, float capacity) {
		residualNodeCapacities[node] = capacity;
	}

	public final float getResidualEdgeCapacity(int edge) {
		return residualEdgeCapacities[edge];
	}

	public final void setResidualEdgeCapacity(int edge, float capacity) {
		residualEdgeCapacities[edge] = capacity;
	}

	public final int getParent(int node) {
		return parents[node];
	}

	public final void setParent(int node, int edge) {
		parents[node] = edge;
	}

	public final int getSister(int edge) {
		return sisters[edge];
	}

	public final void setSister(int edge, int sister) {
		sisters[edge] = sister;
	}

	public final int getNextNode(int node) {
		return nextNodes[node];
	}

	public final void setNextNode(int node, int next) {
		nextNodes[node] = next;
	}

	public final int getNextEdge(int edge) {
		return nextEdges[edge];
	}

	public final void setNextEdge(int edge, int next) {
		nextEdges[edge] = next;
	}

	public final int getFirstOutgoing(int node) {
		return firstOutgoings[node];
	}

	public final void setFirstOutgoing(int node, int edge) {
		firstOutgoings[node] = edge;
	}

	public final int getHead(int edge) {
		return heads[edge];
	}

	public final void setHead(int edge, int head) {
		heads[edge] = head;
	}

	public final boolean isInSink(int node) {
		return inSink[node];
	}

	public final void isInSink(int node, boolean isIn) {
		inSink[node] = isIn;
	}

	public final int getTimestamp(int node) {
		return timestamps[node];
	}

	public final void setTimestamp(int node, int time) {
		timestamps[node] = time;
	}

	public final int getDistance(int node) {
		return distances[node];
	}

	public final void setDistance(int node, int distance) {
		distances[node] = distance;
	}

	public final boolean isInChangedList(int node) {
		return inChangedList[node];
	}

	public final void isInChangedList(int node, boolean isIn) {
		inChangedList[node] = isIn;
	}

	public final int getNumNodes() {
		return numNodes;
	}

	public final int getNumEdges() {
		return numEdges;
	}

	public final boolean isMarked(int node) {
		return marked[node];
	}

	public final void isMarked(int node, boolean is) {
		marked[node] = is;
	}
}
