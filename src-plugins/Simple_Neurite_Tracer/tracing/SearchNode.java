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

public class SearchNode implements Comparable {

	public int x;
	public int y;
	public int z;

	public float g; // cost of the path so far (up to and including this node)
	public float h; // heuristic esimate of the cost of going from here to the goal

	public float f; // should always be the sum of g and h

	private SearchNode predecessor;

        public SearchNode getPredecessor( ) {
                return predecessor;
        }

	public void setPredecessor( SearchNode p ) {
		this.predecessor = p;
	}

	/* This must be one of:

	   SearchThread.OPEN_FROM_START
	   SearchThread.CLOSED_FROM_START
	   SearchThread.OPEN_FROM_GOAL
	   SearchThread.CLOSED_FROM_GOAL
	   SearchThread.FREE
	*/

	public byte searchStatus;

	public SearchNode( int x, int y, int z,
			   float g, float h,
			   SearchNode predecessor,
			   byte searchStatus ) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.g = g;
		this.h = h;
		this.f = g + h;
		this.predecessor = predecessor;
		this.searchStatus = searchStatus;
	}

	// FIXME: check whether this is used any more:

	@Override
	public boolean equals( Object other ) {
		SearchNode o = (SearchNode) other;
		return (x == o.x) && (y == o.y) && (z == o.z);
	}

	@Override
        public int hashCode() {
		int hash = 3;
		hash = 67 * hash + this.x;
		hash = 67 * hash + this.y;
		hash = 67 * hash + this.z;
		return hash;
        }

	public void setFrom( SearchNode another ) {
		this.x = another.x;
		this.y = another.y;
		this.z = another.z;
		this.g = another.g;
		this.h = another.h;
		this.f = another.f;
		this.predecessor = another.predecessor;
		this.searchStatus = another.searchStatus;
	}

	/* This is used by PriorityQueue: */

	public int compareTo( Object other ) {

		SearchNode o = (SearchNode) other;

		int compare_f_result = 0;
		if( f > o.f )
			compare_f_result = 1;
		else if( f < o.f )
			compare_f_result = -1;

		if( compare_f_result != 0 ) {

			return compare_f_result;

		} else {

			// Annoyingly, we need to distinguish between nodes with the
			// same priority, but which are at different locations.

			int x_compare = 0;
			if( x > o.x )
				x_compare = 1;
			if( x < o.x )
				x_compare = -1;

			if( x_compare != 0 )
				return x_compare;

			int y_compare = 0;
			if( y > o.y )
				y_compare = 1;
			if( y < o.y )
				y_compare = -1;

			if( y_compare != 0 )
				return y_compare;

			int z_compare = 0;
			if( z > o.z )
				z_compare = 1;
			if( z < o.z )
				z_compare = -1;

			return z_compare;

		}


	}

	@Override
	public String toString( ) {
		String searchStatusString = "BUG: unknown!";
		if( searchStatus == SearchThread.OPEN_FROM_START )
			searchStatusString = "open from start";
		else if( searchStatus == SearchThread.CLOSED_FROM_START )
			searchStatusString = "closed from start";
		else if( searchStatus == SearchThread.OPEN_FROM_GOAL )
			searchStatusString = "open from goal";
		else if( searchStatus == SearchThread.CLOSED_FROM_GOAL )
			searchStatusString = "closed from goal";
		else if( searchStatus == SearchThread.FREE )
			searchStatusString = "free";
		return "("+x+","+y+","+z+") h: "+h+" g: "+g+" f: "+f+" ["+searchStatusString+"]";
	}

	public Path asPath( double x_spacing, double y_spacing, double z_spacing, String spacing_units ) {
		Path creversed = new Path(x_spacing, y_spacing, z_spacing, spacing_units);
		SearchNode p = this;
		do {
			creversed.addPointDouble( p.x * x_spacing, p.y * y_spacing, p.z * z_spacing );
			p = p.predecessor;
		} while( p != null );
		return creversed.reversed();
	}

	public Path asPathReversed( double x_spacing, double y_spacing, double z_spacing, String spacing_units ) {
		Path result = new Path(x_spacing, y_spacing, z_spacing, spacing_units);
		SearchNode p = this;
		do {
			result.addPointDouble( p.x * x_spacing, p.y * y_spacing, p.z * z_spacing );
			p = p.predecessor;
		} while( p != null );
		return result;
	}

}
