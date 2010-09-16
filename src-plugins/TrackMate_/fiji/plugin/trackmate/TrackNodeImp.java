package fiji.plugin.trackmate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Plain implementation of the {@link TrackNode} interface.		
 */
public class TrackNodeImp<K> implements TrackNode<K> {

	/*
	 * CONSTRUCTORS
	 */

	private K object;
	private HashSet<TrackNode<K>> parents = new HashSet<TrackNode<K>>();
	private HashSet<TrackNode<K>> children = new HashSet<TrackNode<K>>();
	

	public TrackNodeImp(K object) {
		this.object = object;
	}
	
	public TrackNodeImp() {
		this(null);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public boolean addChild(TrackNode<K> child) {
		return children.add(child);
	}

	@Override
	public boolean addParent(TrackNode<K> parent) {
		return parents.add(parent);
	}

	@Override
	public Set<TrackNode<K>> getChildren() {
		return children;
	}

	@Override
	public K getObject() {
		return object;
	}

	@Override
	public Set<TrackNode<K>> getParents() {
		return parents;
	}

	@Override
	public void setObject(K object) {
		this.object = object;		
	}

	@Override
	public Iterator<TrackNode<K>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
