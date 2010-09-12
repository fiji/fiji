package fiji.plugin.spottracker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TrackNodeI<K> implements TrackNode<K> {

	/*
	 * CONSTRUCTORS
	 */

	private K object;
	private HashSet<TrackNode<K>> parents = new HashSet<TrackNode<K>>();
	private HashSet<TrackNode<K>> children = new HashSet<TrackNode<K>>();
	

	public TrackNodeI(K object) {
		this.object = object;
	}
	
	public TrackNodeI() {
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
