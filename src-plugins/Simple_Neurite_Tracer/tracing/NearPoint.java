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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.PriorityQueue;
import java.util.LinkedList;

import java.io.*;

import ij.*;

import client.ArchiveClient;
import ij.measure.Calibration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

import java.awt.Color;

import javax.media.j3d.View;
import ij3d.Content;
import ij3d.UniverseListener;

/* This is a class that encapsulates the relationship between an
   arbitrary point (nearX,nearY,nearZ) close to a particular point on
   a particular path.

   The important method here is:

      distanceToPathNearPoint()

   ... which will tell you the distance to the nearest point on the
   the line segments on either side of the path point, rather than
   just the point.  Also, it will return null if the point appears to
   be "off the end" of the Path.
 */

public class NearPoint implements Comparable {

	public NearPoint( double nearX, double nearY, double nearZ,
			  Path path,
			  int indexInPath ) {
		this.path = path;
		this.indexInPath = indexInPath;
		this.pathPointX = path.precise_x_positions[indexInPath];
		this.pathPointY = path.precise_y_positions[indexInPath];
		this.pathPointZ = path.precise_z_positions[indexInPath];
		this.nearX = nearX;
		this.nearY = nearY;
		this.nearZ = nearZ;
		double xdiff = nearX - pathPointX;
		double ydiff = nearY - pathPointY;
		double zdiff = nearZ - pathPointZ;
		this.distanceSquared =
			xdiff * xdiff +
			ydiff * ydiff +
			zdiff * zdiff;
		closestIntersection = null;
	}

	private Path path;
	private int indexInPath;
	double pathPointX, pathPointY, pathPointZ;
	double nearX, nearY, nearZ;
	private double distanceSquared;
	private Double cachedDistanceToPathNearPoint;

	public int compareTo( Object other ) {
		double d = distanceSquared;
		double od = ((NearPoint)other).distanceSquared;
		return (d < od) ? -1 : ((d > od) ? 1 : 0);
	}

	public String toString() {
		return "  near: ("+nearX+","+nearY+","+nearZ+")\n"+
			"  pathPoint: ("+pathPointX+","+pathPointY+","+pathPointZ+")\n"+
			"  indexInPath: "+indexInPath+"\n"+
			"  path: "+path+"\n"+
			"  distanceSquared: "+distanceSquared+"\n"+
			"  cachedDistanceToPathNearPoint: "+cachedDistanceToPathNearPoint;
	}

	IntersectionOnLine closestIntersection;

	/* If we can find a corresponding point on the path,
	   returns the distance to the path.  Returns -1 if no
	   such point can be found, so test for < 0 */

	public double distanceToPathNearPoint( ) {
		/* Currently these objects are immutable, so
		   if there's a cached value then just return
		   that: */
		if( cachedDistanceToPathNearPoint != null )
			return cachedDistanceToPathNearPoint.doubleValue();
		int pathSize = path.size();
		if( pathSize <= 1 ) {
			cachedDistanceToPathNearPoint = new Double( -1 );
			return -1;
		}
		if( indexInPath == 0 || indexInPath == (pathSize - 1) ) {
			double startX, startY, startZ;
			double endX, endY, endZ;
			if( indexInPath == 0 ) {
				startX = pathPointX;
				startY = pathPointY;
				startZ = pathPointZ;
				endX = path.precise_x_positions[0];
				endY = path.precise_y_positions[0];
				endZ = path.precise_z_positions[0];
			} else {
				startX = path.precise_x_positions[pathSize-2];
				startY = path.precise_y_positions[pathSize-2];
				startZ = path.precise_z_positions[pathSize-2];
				endX = pathPointX;
				endY = pathPointY;
				endZ = pathPointZ;
			}
			IntersectionOnLine intersection = distanceToLineSegment(
				nearX, nearY, nearZ,
				startX, startY, startZ,
				endX, endY, endZ );
			if( intersection == null ) {
				closestIntersection = null;
				cachedDistanceToPathNearPoint = new Double( -1 );
				return -1;
			} else {
				closestIntersection = intersection;
				cachedDistanceToPathNearPoint = new Double( intersection.distance );
				return intersection.distance;
			}
		} else {
			// There's a point on either size:
			double previousX = path.precise_x_positions[indexInPath-1];
			double previousY = path.precise_y_positions[indexInPath-1];
			double previousZ = path.precise_z_positions[indexInPath-1];
			double nextX = path.precise_x_positions[indexInPath+1];
			double nextY = path.precise_y_positions[indexInPath+1];
			double nextZ = path.precise_z_positions[indexInPath+1];
			IntersectionOnLine intersectionA = distanceToLineSegment(
				nearX, nearY, nearZ,
				previousX, previousY, previousZ,
				pathPointX, pathPointY, pathPointZ );
			IntersectionOnLine intersectionB = distanceToLineSegment(
				nearX, nearY, nearZ,
				pathPointX, pathPointY, pathPointZ,
				nextX, nextY, nextZ );
			double smallestDistance = -1;
			if( intersectionA == null && intersectionB != null ) {
				smallestDistance = intersectionB.distance;
				closestIntersection = intersectionB;
			} else if( intersectionA != null && intersectionB == null ) {
				smallestDistance = intersectionA.distance;
				closestIntersection = intersectionA;
			} else if( intersectionA != null && intersectionB != null ) {
				if( intersectionA.distance < intersectionB.distance ) {
					smallestDistance = intersectionA.distance;
					closestIntersection = intersectionA;
				} else {
					smallestDistance = intersectionB.distance;
					closestIntersection = intersectionB;
				}
			}
			if( smallestDistance >= 0 ) {
				cachedDistanceToPathNearPoint = new Double( smallestDistance );
				return smallestDistance;
			}
			/* Otherwise the only other
			   possibility is that it's between
			   the planes: */
			boolean afterPlaneAtEndOfPrevious =
				0 < normalSideOfPlane(
					pathPointX, pathPointY, pathPointZ,
					pathPointX - previousX, pathPointY - previousY, pathPointZ - previousZ,
					nearX, nearY, nearZ );
			boolean beforePlaneAtStartOfNext =
				0 < normalSideOfPlane(
					pathPointX, pathPointY, pathPointZ,
					pathPointX - nextX, pathPointY - nextY, pathPointZ - nextZ,
					nearX, nearY, nearZ );
			if( afterPlaneAtEndOfPrevious && beforePlaneAtStartOfNext ) {
				// Then just return the distance to the point:
				closestIntersection = new IntersectionOnLine();
				closestIntersection.distance = distanceToPathPoint();
				closestIntersection.x = pathPointX;
				closestIntersection.y = pathPointY;
				closestIntersection.z = pathPointZ;
				closestIntersection.fromPerpendicular = false;
				cachedDistanceToPathNearPoint = new Double( closestIntersection.distance );
				return closestIntersection.distance;
			} else {
				closestIntersection = null;
				cachedDistanceToPathNearPoint = new Double( -1 );
				return -1;
			}
		}
	}

	/* This returns null if the perpendicular dropped to the line
	   doesn't lie within the segment.  Otherwise it returns the
	   shortest distance to this line segment and the point of
	   intersection in an IntersectionOnLine object */

	public static IntersectionOnLine distanceToLineSegment(
		double x, double y, double z,
		double startX, double startY, double startZ,
		double endX, double endY, double endZ ) {

		boolean insideStartPlane = 0 >= normalSideOfPlane(
			startX, startY, startZ,
			startX - endX, startY - endY, startZ - endZ,
			x, y, z );
		boolean insideEndPlane = 0 >= normalSideOfPlane(
			endX, endY, endZ,
			endX - startX, endY - startY, endZ - startZ,
			x, y, z );
		if( insideStartPlane && insideEndPlane )
			return distanceFromPointToLine(
				startX, startY, startZ,
				endX - startX, endY - startY, endZ - startZ,
				x, y, z );
		else
			return null;
	}

	public double distanceToPathPoint() {
		return Math.sqrt( distanceSquared );
	}

	public double distanceToPathPointSquared() {
		return distanceSquared;
	}

	/* This tests whether a given point (x, y, z) is on
	   the side of a plane in the direction of its normal
	   vector (nx,ny,nz).  (cx,cy,cz) is any point in the
	   plane.  If (x,y,z) is in the plane, it returns 0;
	   if (x,y,z) is on the side of the plane pointed to
	   by the normal vector then it returns 1; otherwise
	   (i.e. it is on the other side) this returns -1 */

	public static int normalSideOfPlane( double cx, double cy, double cz,
					     double nx, double ny, double nz,
					     double x, double y, double z ) {
		double vx = x - cx;
		double vy = y - cy;
		double vz = z - cz;

		double dotProduct =
			nx * vx +
			ny * vy +
			nz * vz;

		if( dotProduct > 0 )
			return 1;
		else if( dotProduct < 0 )
			return -1;
		else
			return 0;
	}

	/* To find where the perpendicular dropped from the
	   the point to the line meets it, with:

	   A = (ax, ay, az) being a point in the line
	   V = (vx, vy, vz) being a vector along the line
	   P = (x, y, z) being our point

	   [(A + b V) - P] . V = 0

	   ... which we can reduce to:

	   b = [ (x - ax) * vx + (y - ay) * vy + (z - az) * vz ] / (vx * vx + vy * vy + vz * vz)
	*/

	static class IntersectionOnLine {
		double x, y, z, distance;
		boolean fromPerpendicular = true;
	}

	public static IntersectionOnLine distanceFromPointToLine( double ax, double ay, double az,
								  double vx, double vy, double vz,
								  double x, double y, double z ) {
		double b = ( (x - ax) * vx +
			     (y - ay) * vy +
			     (z - az) * vz ) /
			(vx * vx + vy * vy + vz * vz);

		IntersectionOnLine i = new IntersectionOnLine();

		i.x = ax + b * vx;
		i.y = ay + b * vy;
		i.z = az + b * vz;

		double xdiff = i.x - x;
		double ydiff = i.y - y;
		double zdiff = i.z - z;

		i.distance = Math.sqrt( xdiff * xdiff +
					ydiff * ydiff +
					zdiff * zdiff );
		return i;
	}

}
