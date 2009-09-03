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

public class Vertex 
{
	private ArrayList<Point> points = null;
	
	private ArrayList<Edge> branches = null;
	
	private boolean visited = false;
	
	private Edge precedessor = null;
	
	private int visitOrder = -1;
	
	
	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	public Vertex()
	{
		this.points = new ArrayList < Point > ();
		this.branches = new ArrayList<Edge>();
	}
	
	// --------------------------------------------------------------------------
	/**
	 * 
	 * @param p
	 */
	public void addPoint(Point p)
	{
		this.points.add(p);
	}
	// --------------------------------------------------------------------------
	/**
	 * 
	 * @param p
	 * @return
	 */
	public boolean isVertexPoint(Point p)
	{
		if (points == null)
			return false;
		return points.contains(p);
	}
	// --------------------------------------------------------------------------
	/**
	 * 
	 * @return
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
	 * 
	 * @return
	 */
	public ArrayList < Point > getPoints()
	{
		return this.points;
	}
	// --------------------------------------------------------------------------
	/**
	 * Add a new branch to the vertex
	 * 
	 * @param e neighbor edge
	 */
	public void setBranch(Edge e)
	{
		this.branches.add(e);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get branch list
	 * 
	 * @return list of branch vertices
	 */
	public ArrayList<Edge> getBranches()
	{
		return this.branches;
	}
	// --------------------------------------------------------------------------
	/**
	 * Set vertex as visited or not
	 * 
	 * @param b boolean flag 
	 */
	public void setVisited(boolean b)
	{
		this.visited = b;
	}

	// --------------------------------------------------------------------------
	/**
	 * Set vertex as visited or not
	 * 
	 * @param b boolean flag 
	 */
	public void setVisited(boolean b, int visitOrder)
	{
		this.visited = b;
		this.visitOrder = visitOrder;
	}	
	
	public boolean isVisited()
	{
		return this.visited;
	}
	
	public void setPredecessor(Edge pred)
	{
		this.precedessor = pred;
	}
	
	public Edge getPredecessor()
	{
		return this.precedessor;
	}
	
	public int getVisitOrder()
	{
		return this.visitOrder;
	}
	
}// end class Vertex
