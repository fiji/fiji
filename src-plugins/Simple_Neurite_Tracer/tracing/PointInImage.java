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

/* The x, y and z here are in world coordinates, i.e. already scaled
   by the calibration values. */

public class PointInImage {

	public double x, y, z;
	public PointInImage( double x, double y, double z ) {
		this.x = x; this.y = y; this.z = z;
	}

	// You can optionally set this value:
	public Path onPath = null;

	public double distanceSquaredTo( double ox,
					 double oy,
					 double oz ) {
		double xdiff = x - ox;
		double ydiff = y - oy;
		double zdiff = z - oz;
		return xdiff * xdiff + ydiff * ydiff + zdiff * zdiff;
	}

	public double distanceSquaredTo( PointInImage o ) {
		return distanceSquaredTo( o.x, o.y, o.z );
	}

	public double distanceTo( PointInImage o ) {
		double xdiff = x - o.x;
		double ydiff = y - o.y;
		double zdiff = z - o.z;
		return Math.sqrt( xdiff * xdiff + ydiff * ydiff + zdiff * zdiff );
	}

	public String toString() {
		return "( "+x+", "+y+", "+z+" ) [onPath "+onPath+"]";
	}

	public PointInImage transform( PathTransformer transformer ) {
		double [] result = new double [3];
		transformer.transformPoint( x, y, z, result );
		return new PointInImage( result[0], result[1], result[2] );
	}

	public boolean isReal() {
		return ! (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) ||
			  Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z));
	}

}
