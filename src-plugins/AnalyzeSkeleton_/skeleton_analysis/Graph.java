package skeleton_analysis;

import ij.IJ;

import java.util.ArrayList;
import java.util.Stack;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C).
 * Copyright (C) 2008,2009 Ignacio Arganda-Carreras 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

/**
 * This class represents an undirected graph to allow 
 * visiting the skeleton in an efficient way
 */
public class Graph 
{
	/** list of edges */	
	private ArrayList < Edge > edges = null;
	/** list of vertices */
	private ArrayList < Vertex > vertices = null;
	
	/** root vertex */
	private Vertex root = null;
	
	// --------------------------------------------------------------------------
	/**
	 * Empty constructor.
	 */
	public Graph()
	{
		this.edges = new ArrayList < Edge >();
		this.vertices = new ArrayList<Vertex>();
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Add edge to the graph.
	 * @param e edge to be added
	 * @return false if the edge could not be added, true otherwise
	 */
	public boolean addEdge(Edge e)
	{
		if(this.edges.contains(e))
			return false;
		else
		{
			// Set vertices from e as neighbors (undirected graph)
			e.getV1().setBranch(e);
			if(! e.getV1().equals(e.getV2()))
				e.getV2().setBranch(e);
			// Add edge to the list of edges in the graph
			this.edges.add(e);
			return true;
		}
	}// end method addEdge
	
	// --------------------------------------------------------------------------
	/**
	 * Add vertex to the graph.
	 * @param v vertex to be added
	 * @return false if the vertex could not be added, true otherwise
	 */
	public boolean addVertex(Vertex v)
	{
		if(this.vertices.contains(v))
			return false;
		else
		{
			this.vertices.add(v);
			return true;
		}
	}// end method addVertex
	// --------------------------------------------------------------------------
	/**
	 * Get list of vertices in the graph.
	 * @return list of vertices in the graph
	 */
	public ArrayList<Vertex> getVertices()
	{
		return this.vertices;
	}
	// --------------------------------------------------------------------------
	/**
	 * Get list of edges in the graph.
	 * @return list of edges in the graph
	 */
	public ArrayList<Edge> getEdges()
	{
		return this.edges;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Set root vertex.
	 */
	void setRoot(Vertex v)
	{
		this.root = v;
	}
	// --------------------------------------------------------------------------
	/**
	 * Depth first search method to detect cycles in the graph.
	 * 
	 * @return list of BACK edges
	 */
	ArrayList<Edge> depthFirstSearch()
	{
		ArrayList<Edge> backEdges = new ArrayList<Edge>();
		
		// Create empty stack
		Stack<Vertex> stack = new Stack<Vertex>();
		
		// Mark all vertices as non-visited
		for(final Vertex v : this.vertices)
			v.setVisited(false);
		
		// Push the root into the stack
		stack.push(this.root);			
		
		int visitOrder = 0;
		
		while(!stack.empty())
		{
			Vertex u = stack.pop();
			
			if(!u.isVisited())						
			{
				//IJ.log(" Visiting vertex " + u.getPoints().get(0));
				
				// If the vertex has not been visited yet, then
				// the edge from the predecessor to this vertex
				// is mark as TREE
				if(u.getPredecessor() != null)
					u.getPredecessor().setType(Edge.TREE);

				// mark as visited
				u.setVisited(true, visitOrder++);

				for(final Edge e : u.getBranches())
				{
					// For the undefined branches:
					// We push the unvisited vertices in the stack,
					// and mark the edge to the others as BACK 
					if(e.getType() == Edge.UNDEFINED)
					{
						final Vertex ov = e.getOppositeVertex(u);
						if(!ov.isVisited())						
						{
							stack.push(ov);
							ov.setPredecessor(e);
						}
						else
						{
							e.setType(Edge.BACK);
							backEdges.add(e);
						}
						
					}
				}
			}
		}	
		
		return backEdges;
		
	} // end method depthFirstSearch
	
}// end class Graph
