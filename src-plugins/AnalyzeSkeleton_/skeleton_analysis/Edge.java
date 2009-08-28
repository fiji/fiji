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

public class Edge
{
	public final static int TREE = 0;
	public final static int BACK = 1;
	public final static int UNDEFINED = -1;
	
	private int type = Edge.UNDEFINED;
	
	private Vertex v1 = null;
	private Vertex v2 = null;
	private ArrayList <Point> slabs = null;

	public Edge(Vertex v1, Vertex v2, ArrayList<Point> slabs)
	{
		this.v1 = v1;
		this.v2 = v2;
		this.slabs = slabs;		
	}
	
	public Vertex getV1()
	{
		return this.v1;
	}
	
	public Vertex getV2()
	{
		return this.v2;
	}
	
	public ArrayList<Point> getSlabs()
	{
		return this.slabs;
	}
	
	public void setType(int type)
	{
		this.type = type;
	}
	
	public int getType()
	{
		return this.type;
	}
	
	public Vertex getOppositeVertex(Vertex v)
	{
		if(this.v1.equals(v))
			return this.v2;
		else if (this.v2.equals(v))
			return this.v1;
		else 
			return null;
	}

}
