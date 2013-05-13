package fiji.plugin.trackmate.tracking.kdtree;

public class FlagNode<K> {

	private K value;
	private boolean visited = false;
	
	public FlagNode(K value) {
		this.setValue(value);
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public K getValue() {
		return value;
	}

	public void setValue(K value) {
		this.value = value;
	}
}
