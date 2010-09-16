package fiji.plugin.trackmate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

public class TrackIterator<K> implements Iterator<TrackNode<K>> {

	
	/*
	 * CONSTRUCTOR
	 */
	
	private Stack<TrackNode<K>> fringe;

	public TrackIterator(TrackNode<K> node) {
		fringe = new Stack<TrackNode<K>>();
		fringe.push(node);
	}
	
	/*
	 * ITERATOR METHODS
	 */
	
	@Override
	public boolean hasNext() {
		return !fringe.empty();
	}

	@Override
	public TrackNode<K> next() {
		if (!hasNext())
            throw new NoSuchElementException("Track ran out of elements");
        
		TrackNode<K> node = fringe.pop();
		
		// Put parents first in the stack - last to pop
		for(TrackNode<K> parent : node.getParents())
			fringe.push(parent);
		for(TrackNode<K> child : node.getChildren())
			fringe.push(child);
		return node;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
