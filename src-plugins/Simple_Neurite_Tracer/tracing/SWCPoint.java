/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010 Mark Longair */

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

import java.util.ArrayList;
import java.io.PrintWriter;

public class SWCPoint implements Comparable {
	ArrayList<SWCPoint> nextPoints;
	SWCPoint previousPoint;
	int id, type, previous;
	double x, y, z, radius;
	Path fromPath = null;
	public SWCPoint( int id, int type, double x, double y, double z, double radius, int previous ) {
		nextPoints = new ArrayList<SWCPoint>();
		this.id = id;
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.previous = previous;
	}
	public PointInImage getPointInImage() {
		return new PointInImage( x, y, z );
	}
	public void addNextPoint( SWCPoint p ) {
		if( ! nextPoints.contains( p ) )
			nextPoints.add( p );
	}
	public void setPreviousPoint( SWCPoint p ) {
		previousPoint = p;
	}
	public String toString( ) {
		return "SWCPoint ["+id+"] "+Path.swcTypeNames[type]+" "+
			"("+x+","+y+","+z+") "+
			"radius: "+radius+", "+
			"[previous: "+ previous+"]";
	}
	public int compareTo( Object o ) {
		int oid = ((SWCPoint)o).id;
		return (id < oid) ? -1 : ((id > oid) ? 1 : 0);
	}
	public void println(PrintWriter pw) {
		pw.println(""+id+" "+type+" "+x+" "+y+" "+z+" "+radius+" "+previous);
	}
}
