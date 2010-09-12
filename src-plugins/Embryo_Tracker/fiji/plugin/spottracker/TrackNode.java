package fiji.plugin.spottracker;

import java.util.Set;


/**
 * A general interface meant to describe a track node, which does not take necessary
 * the shape of a tree node: a node can have multiple parents.
 * <p>
 * Because there is parents and children, this node takes the shape of a node
 * within a directed graph. Only basic method to add nodes and retrieve them are
 * here. Vertices are not explicitly handled. If there is a need to play with the
 * graph in a more sophisticated way, then one have to replace this naive interface
 * by for instance JGraphT, which offers a proper framework for graphs.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sep 2010
 */
public interface TrackNode<K> extends Iterable<TrackNode<K>> {

	
	/*
	 * TRACKNODE
	 */
	
	/**
	 * Set the object at this TrackNode.
	 */
	public void setObject(K object);
	
	/**
	 * Retrieve the object at this tracknode.
	 * @return
	 */
	public K getObject();
	
	/**
	 * Add a parent node to this node.
	 * @return  true if the parent was successfully added to the parent list.
	 */
	public boolean addParent(TrackNode<K> parent);

	/**
	 * Add a child node to this node.
	 * @return  true if the child was successfully added to the children list.
	 */
	public boolean addChild(TrackNode<K> child);
	
	/**
	 * Return all the parent nodes of this node. If there is not parents, 
	 * then the returned set is empty (never <code>null</code>).
	 */
	public Set<TrackNode<K>> getParents();

	/**
	 * Return all the children nodes of this node. If there is not children, 
	 * then the returned set is empty (never <code>null</code>).
	 */
	public Set<TrackNode<K>> getChildren();
	
}
