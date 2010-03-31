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
 * This class represents the edge between two vertices in an undirected graph.
 */
public class Edge
{
	/** "tree" edge classification constant for Depth-first search (DFS) */
	public final static int TREE = 0;
	/** "back" edge classification constant for Depth-first search (DFS) */
	public final static int BACK = 1;
	/** not yet defined edge classification constant for Depth-first search (DFS) */
	public final static int UNDEFINED = -1;
	
	/** DFS classification */
	private int type = Edge.UNDEFINED;
	
	/** vertex at one extreme of the edge */
	private Vertex v1 = null;
	/** vertex at the other extreme of the edge */
	private Vertex v2 = null;
	/** list of slab voxels belonging to this edge */
	private ArrayList <Point> slabs = null;
	/** length of the edge */
	private double length = 0;

	/**
	 * Create an edge of specific vertices and list of slab voxels.
	 * @param v1 first vertex
	 * @param v2 second vertex
	 * @param slabs list of slab voxels
	 * @param length calibrated edge length
	 */
	public Edge(
			Vertex v1, 
			Vertex v2, 
			ArrayList<Point> slabs,
			double length)
	{
		this.v1 = v1;
		this.v2 = v2;
		this.slabs = slabs;
		this.length = length;
	}
	/**
	 * Get first vertex. 
	 * @return first vertex of the edge
	 */
	public Vertex getV1()
	{
		return this.v1;
	}
	/**
	 * Get second vertex.
	 * @return second vertex of the edge
	 */
	public Vertex getV2()
	{
		return this.v2;
	}
	/**
	 * Get list of slab voxels belonging to the edge.
	 * @return list of slab voxels
	 */
	public ArrayList<Point> getSlabs()
	{
		return this.slabs;
	}
	/**
	 * Set DFS type (BACK or TREE)
	 * @param type DFS classification (BACK or TREE)
	 */
	public void setType(int type)
	{
		this.type = type;
	}
	/**
	 * Get DFS edge type
	 * @return DFS classification type
	 */
	public int getType()
	{
		return this.type;
	}
	/**
	 * Get opposite vertex from a given one.
	 * @param v input vertex
	 * @return opposite vertex in the edge
	 */
	public Vertex getOppositeVertex(Vertex v)
	{
		if(this.v1.equals(v))
			return this.v2;
		else if (this.v2.equals(v))
			return this.v1;
		else 
			return null;
	}
	
	/**
	 * Set edge length
	 * @param length calibrated edge length
	 */
	public void setLength(double length)
	{
		this.length = length;
	}

	/**
	 * Get edge length
	 * @return calibrated edge length
	 */
	public double getLength()
	{
		return this.length;
	}
	
}// end class Edge
