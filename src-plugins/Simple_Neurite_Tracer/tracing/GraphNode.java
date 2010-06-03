/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

class GraphNode implements Comparable {



	public int id;

	public int x;
	public int y;
	public int z;

	public String material_name;

	/* These few for the path finding... */
	public GraphNode previous;
	public double g; // cost of the path so far (up to and including this node)
	public double h; // heuristic esimate of the cost of going from here to the goal
	/* ... end of path */

	void setFrom( GraphNode other ) {
		this.id = other.id;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.material_name = other.material_name;
		this.previous = other.previous;
		this.g = other.g;
		this.h = other.h;
	}

	// -----------------------------------------------------------------

	double f() {
		return g + h;
	}

	public int compareTo( Object other ) {
		GraphNode n = (GraphNode)other;
		return Double.compare( f(), n.f() );
	}

	@Override
	public boolean equals( Object other ) {
		// System.out.println("  equals called "+id);
		return this.id == ((GraphNode)other).id;
	}

	@Override
	public int hashCode() {
		// System.out.println("  hashcode called "+id);
		return this.id;
	}

	// -----------------------------------------------------------------

	public boolean nearTo( int within, int other_x, int other_y, int other_z ) {
		int xdiff = other_x - x;
		int ydiff = other_y - y;
		int zdiff = other_z - z;
		long distance_squared = xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
		long within_squared = within * within;
		return distance_squared <= within_squared;
	}

	public String toDotName( ) {
		return material_name + " (" + id + ")";
	}

	public String toCollapsedDotName( ) {
		if( material_name.equals("Exterior") )
			return material_name + " (" + id + ")";
		else
			return material_name;
	}

}
