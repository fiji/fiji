package skeleton_analysis;

import java.util.ArrayList;

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
 * This class represents a vertex or node in a graph.
 */
public class Vertex 
{
	/** list of points belonging to the vertex */
	private ArrayList<Point> points = null;
	/** list of projecting edges from this vertex */
	private ArrayList<Edge> branches = null;
	/** visit status (for depth-first search, DFS) */
	private boolean visited = false;
	/** previously visited edge in DFS */
	private Edge precedessor = null;
	/** DFS visit order */
	private int visitOrder = -1;
	
	
	// --------------------------------------------------------------------------
	/**
	 * Create empty vertex.
	 */
	public Vertex()
	{
		this.points = new ArrayList < Point > ();
		this.branches = new ArrayList<Edge>();
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Add point to the vertex.
	 * @param p input point
	 */
	public void addPoint(Point p)
	{
		this.points.add(p);
	}
	// --------------------------------------------------------------------------
	/**
	 * Check if a point belongs to the vertex list of points.
	 * @param p input points
	 * @return true if the point is in the vertex point list
	 */
	public boolean isVertexPoint(Point p)
	{
		if (points == null)
			return false;
		return points.contains(p);
	}
	// --------------------------------------------------------------------------
	/**
	 * Convert list of points to String.
	 * @return printable version of the list of points
	 */
	public String pointsToString()
	{
		StringBuilder sb = new StringBuilder();
		for(final Point p : this.points)
			sb.append(p.toString() + " ");
		
		return sb.toString();
	}
	// --------------------------------------------------------------------------
	/**
	 * Get list of points.
	 * @return list of points
	 */
	public ArrayList < Point > getPoints()
	{
		return this.points;
	}
	// --------------------------------------------------------------------------
	/**
	 * Add a new branch to the vertex.
	 * 
	 * @param e neighbor edge
	 */
	public void setBranch(Edge e)
	{
		this.branches.add(e);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get branch list.
	 * 
	 * @return list of branch vertices
	 */
	public ArrayList<Edge> getBranches()
	{
		return this.branches;
	}
	// --------------------------------------------------------------------------
	/**
	 * Set vertex as visited or not.
	 * 
	 * @param b boolean flag 
	 */
	public void setVisited(boolean b)
	{
		this.visited = b;
	}

	// --------------------------------------------------------------------------
	/**
	 * Set vertex as visited or not.
	 * 
	 * @param b boolean flag 
	 */
	public void setVisited(boolean b, int visitOrder)
	{
		this.visited = b;
		this.visitOrder = visitOrder;
	}	
	/**
	 * Check visit status.
	 * @return true if the vertex was already visited (DFS)
	 */
	public boolean isVisited()
	{
		return this.visited;
	}
	/**
	 * Set predecessor (for DFS).
	 * @param pred predecessor edge in DFS visit.
	 */
	public void setPredecessor(Edge pred)
	{
		this.precedessor = pred;
	}
	/**
	 * Get predecessor edge.
	 * @return predecessor edge.
	 */
	public Edge getPredecessor()
	{
		return this.precedessor;
	}
	/**
	 * Get DFS visit order.
	 * @return visit order
	 */
	public int getVisitOrder()
	{
		return this.visitOrder;
	}
	
}// end class Vertex
